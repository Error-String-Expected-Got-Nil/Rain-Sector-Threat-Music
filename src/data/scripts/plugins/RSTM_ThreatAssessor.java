package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.Misc;
import data.scripts.RSTM.RSTM_ThreatAssessorSettings;
import data.scripts.RSTM.RSTM_Utils;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.MathUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RSTM_ThreatAssessor extends BaseEveryFrameCombatPlugin {
    private final Logger logger = Global.getLogger(this.getClass());
    private final RSTM_ThreatAssessorSettings settings = RSTMModPlugin.threatAssessorSettings;
    public static RSTM_ThreatAssessor currentThreatAssessor = null;

    private final Map<String, MutableStat> fleetMemberThreatRatings = new HashMap<>();
    private final Map<ShipAPI, MutableStat> shipLocalThreatContribution = new HashMap<>();

    private boolean disabled = true;
    private float stopwatch = 0f;
    private float threat = 0f;

    // Public-access fields with the last recorded interal variables in them, so the debug mode plugin is able to
    // show the values. None of these are actually used in calculations, modifying them won't cause issues.
    public float lastAmbientThreat = 0f;
    public float lastOutmatchPercent = 0f;
    public float lastSituationalThreat = 0f;
    public float lastLocalThreat = 0f;

    @Override
    public void init(CombatEngineAPI engine) {
        if (RSTM_Utils.isInCombat()) {
            log("[RSTM] Threat assessor initializing");
            disabled = false;

            CombatFleetManagerAPI enemyFleet = engine.getFleetManager(FleetSide.ENEMY);
            CombatFleetManagerAPI playerFleet = engine.getFleetManager(FleetSide.PLAYER);

            log("[RSTM] Assessing enemy fleet");
            for (FleetMemberAPI ship : enemyFleet.getReservesCopy()) {
                logv(" ");
                fleetMemberThreatRatings.put(ship.getId(), initialAssessFleetMember(ship));
            }

            logv(" ");
            log("[RSTM] Assessing player fleet");
            for (FleetMemberAPI ship : playerFleet.getReservesCopy()) {
                logv(" ");
                fleetMemberThreatRatings.put(ship.getId(), initialAssessFleetMember(ship));
            }

            // NOTE: In a simulation, the player ship isn't spawned initially and also isn't in reserves.
            // Will just have to handle that in advance() I think

            currentThreatAssessor = this;
        }
    }

    // TODO: Account for stations, account for fighters, drones?

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (disabled) return;

        // As with the music player, due to how RSTM_Utils.isCombatEnding() works, need to wait a little bit after
        // the battle starts before trying to stop the plugin to make sure it doesn't happen early.
        if (stopwatch < 0.45f) stopwatch += amount;

        if (RSTM_Utils.isCombatEnding() && stopwatch > 0.40f) {
            logger.info("[RSTM] Threat assessor plugin thinks combat is ending");

            disabled = true;
            currentThreatAssessor = null;

            return;
        }

        RSTM_MusicPlayer musicPlayer = RSTM_MusicPlayer.getCurrentMusicPlayer();

        if (musicPlayer == null || musicPlayer.isDisabled()) return;

        MutableStat ambientThreat = new MutableStat(getBaseAmbientThreat());
        ambientThreat.modifyMult("ambientThreatGlobalMultiplier", settings.ambientThreatGlobalMultiplier);

        float outmatchModifier = getOutmatchModifier();
        ambientThreat.modifyPercent("outmatchModifier", outmatchModifier);

        lastAmbientThreat = ambientThreat.getModifiedValue();
        lastOutmatchPercent = outmatchModifier;

        MutableStat situationalThreat = new MutableStat(getBaseSituationalThreat());
        situationalThreat.modifyMult("situationalThreatGlobalMultiplier",
                settings.situationalThreatGlobalMultiplier);

        lastSituationalThreat = situationalThreat.getModifiedValue();
    }

    private float getBaseAmbientThreat() {
        CombatEngineAPI engine = Global.getCombatEngine();
        CombatFleetManagerAPI enemyFleet = engine.getFleetManager(FleetSide.ENEMY);

        MutableStat enemyReservesThreat = new MutableStat(0f);

        // Ignore reserves in simulator
        if (!engine.isSimulation()) {
            float baseEnemyReservesThreat = 0f;
            for (FleetMemberAPI ship : enemyFleet.getReservesCopy()) {
                baseEnemyReservesThreat += getFleetMemberThreatRating(ship).getModifiedValue();
            }
            enemyReservesThreat.setBaseValue(baseEnemyReservesThreat);
            enemyReservesThreat.modifyPercent("ambientThreatModReserved",
                    settings.ambientThreatModReserved);
        }

        MutableStat enemyDeployedThreat = new MutableStat(0f);

        float baseEnemyDeployedThreat = 0f;
        for (FleetMemberAPI ship : enemyFleet.getDeployedCopy()) {
            baseEnemyDeployedThreat += getFleetMemberThreatRating(ship).getModifiedValue();
        }
        enemyDeployedThreat.setBaseValue(baseEnemyDeployedThreat);

        return enemyReservesThreat.getModifiedValue() + enemyDeployedThreat.getModifiedValue();
    }

    private float getOutmatchModifier() {
        CombatEngineAPI engine = Global.getCombatEngine();
        CombatFleetManagerAPI enemyFleet = engine.getFleetManager(FleetSide.ENEMY);
        CombatFleetManagerAPI playerFleet = engine.getFleetManager(FleetSide.PLAYER);

        float enemyOutmatchValue = 0f;
        if (!engine.isSimulation()) for (FleetMemberAPI ship : enemyFleet.getReservesCopy())
            enemyOutmatchValue += getOutmatchValue(ship) * settings.outmatchReserveMultiplier;
        for (FleetMemberAPI ship : enemyFleet.getDeployedCopy()) enemyOutmatchValue += getOutmatchValue(ship);

        float friendlyOutmatchValue = 0f;
        if (!engine.isSimulation()) for (FleetMemberAPI ship : playerFleet.getReservesCopy())
            friendlyOutmatchValue += getOutmatchValue(ship) * settings.outmatchReserveMultiplier;
        for (FleetMemberAPI ship : playerFleet.getDeployedCopy()) friendlyOutmatchValue += getOutmatchValue(ship);

        if (friendlyOutmatchValue == 0f) return 0f;

        float outmatchRatio = MathUtils.clamp(enemyOutmatchValue / friendlyOutmatchValue,
                settings.outmatchMinRatio, settings.outmatchMaxRatio);

        if (outmatchRatio < 1 ) {
            return Misc.interpolate(settings.outmatchMinMod, 0f, (outmatchRatio - settings.outmatchMinRatio)
                    / (1f - settings.outmatchMinRatio));
        }

        return Misc.interpolate(0f, settings.outmatchMaxMod, (outmatchRatio - 1f)
                / (settings.outmatchMaxRatio - 1f));
    }

    private float getOutmatchValue(FleetMemberAPI ship) {
        ShipVariantAPI variant = ship.getVariant();

        if (variant.isCivilian()) return 0f;

        switch(variant.getHullSize()) {
            case FRIGATE:
                return settings.outmatchValueFrigate;
            case DESTROYER:
                return settings.outmatchValueDestroyer;
            case CRUISER:
                return settings.outmatchValueCruiser;
            case CAPITAL_SHIP:
                return settings.outmatchValueCapital;
            default:
                return 0f;
        }
    }

    private float getBaseSituationalThreat() {
        ShipAPI playerShip = Global.getCombatEngine().getPlayerShip();

        if (playerShip == null || playerShip.isShuttlePod() || !playerShip.isAlive()) return 0f;

        float baseThreat = 0f;

        if (playerShip.getEngineController().isFlamedOut()) baseThreat += settings.situationalThreatFlamedOut;

        FluxTrackerAPI fluxTracker = playerShip.getFluxTracker();

        if (fluxTracker.isOverloaded()) baseThreat += settings.situationalThreatOverload;

        if (!fluxTracker.isOverloadedOrVenting()) {
            float maxFlux = fluxTracker.getMaxFlux();
            float hardFlux = fluxTracker.getHardFlux();
            float softFlux = fluxTracker.getCurrFlux() - hardFlux;

            baseThreat += hardFlux / maxFlux * settings.situationalThreatAtMaxHardFlux;
            baseThreat += softFlux / maxFlux * settings.situationalThreatAtMaxSoftFlux;
        }

        baseThreat += (1f - playerShip.getHullLevel()) * settings.situationalThreatAtMinHull;

        return baseThreat;
    }

    private float getBaseLocalThreat() {
        ShipAPI playerShip = Global.getCombatEngine().getPlayerShip();

        if (playerShip == null || playerShip.isShuttlePod() || !playerShip.isAlive()) return 0f;
    }

    private MutableStat calculateShipLocalThreatContribution(ShipAPI ship) {
        // This shouldn't get called unless the player ship exists
        ShipAPI playerShip = Global.getCombatEngine().getPlayerShip();

        
    }

    // TODO: Right now, ships without a FleetMemberAPI cannot be assessed. Too bad!
    //  This should usually only happen if a ship was spawned directly, which I'm pretty sure shouldn't happen
    //  under normal circumstances. If it becomes a problem I'll fix it.

    private MutableStat getFleetMemberThreatRating(FleetMemberAPI ship) {
        String id = ship.getId();
        MutableStat threatRating = fleetMemberThreatRatings.get(id);

        if (threatRating == null) {
            log("[RSTM] Queried threat of fleet member not assessed initially, doing so now");
            threatRating = initialAssessFleetMember(ship);
            fleetMemberThreatRatings.put(id, threatRating);
        }

        return threatRating;
    }

    public MutableStat getFleetMemberThreatRatingCopy(FleetMemberAPI ship) {
        return getFleetMemberThreatRating(ship).createCopy();
    }

    private MutableStat initialAssessFleetMember(FleetMemberAPI ship) {
        MutableStat threat = new MutableStat(0f);

        // TODO: Placeholder until I decide to actually deal with stations and fighters
        if (ship.isStation() || ship.isFighterWing()) return threat;

        ShipVariantAPI variant = ship.getVariant();

        logv("[RSTM]    " + ship);



        float threatDP = ship.getUnmodifiedDeploymentPointsCost() * settings.threatPerDP;
        threat.setBaseValue(threat.getBaseValue() + threatDP);

        float threatHullSize = 0f;
        switch (variant.getHullSize()) {
            case FRIGATE:
                threatHullSize = settings.threatFrigate;
                break;
            case DESTROYER:
                threatHullSize = settings.threatDestroyer;
                break;
            case CRUISER:
                threatHullSize = settings.threatCruiser;
                break;
            case CAPITAL_SHIP:
                threatHullSize = settings.threatCapital;
        }
        threat.setBaseValue(threat.getBaseValue() + threatHullSize);



        float threatModDMod = 0f;
        for (String hullModID : variant.getHullMods()) {
            if (Global.getSettings().getHullModSpec(hullModID).hasTag("dmod")) {
                threatModDMod += settings.threatModPerDMod;
            }
        }
        threat.modifyPercent("threatModDMod", threatModDMod);

        float threatModSMod = 0f;
        for (int i = 0; i < variant.getSMods().size(); i++) {
            threatModSMod += settings.threatModPerSMod;
        }
        threat.modifyPercent("threatModSMod", threatModSMod);

        PersonAPI captain = ship.getCaptain();
        float threatModOfficerLevel = 0f;
        if (!captain.isPlayer() && captain.getPostId().equals("officer")) {
            for (int i = 0; i < captain.getStats().getLevel(); i++) {
                threatModOfficerLevel += settings.threatModPerOfficerLevel;
            }
        }
        threat.modifyPercent("threatModOfficerLevel", threatModOfficerLevel);

        float threatModNoncombat = 0f;
        if (variant.isCivilian()) threatModNoncombat = settings.threatModNoncombat;
        threat.modifyPercent("threatModNoncombat", threatModNoncombat);


        logv("[RSTM]       threatDP: " + threatDP);
        logv("[RSTM]       threatHullSize: " + threatHullSize);
        for (String key : threat.getPercentMods().keySet()) logv("[RSTM]       " + key + ": "
                + threat.getPercentMods().get(key).getValue() + "%");
        logv("[RSTM]       Total threat: " + threat.getModifiedValue());

        return threat;
    }

    private void log(String str) {
        if (!settings.doLogging) return;
        logger.info(str);
    }

    private void logv(String str) {
        if (!(settings.doLogging && settings.verboseLogging)) return;
        logger.info(str);
    }
}
