package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.Misc;
import data.scripts.RSTM.RSTM_ThreatAssessorSettings;
import data.scripts.RSTM.RSTM_Utils;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RSTM_ThreatAssessor extends BaseEveryFrameCombatPlugin {
    // Maximum amount current threat value can change per second
    private static final float MAXIMUM_THREAT_DELTA = 20f;

    private final Logger logger = Global.getLogger(this.getClass());
    private final RSTM_ThreatAssessorSettings settings = RSTMModPlugin.threatAssessorSettings;
    public static RSTM_ThreatAssessor currentThreatAssessor = null;

    private final Map<String, MutableStat> fleetMemberThreatRatings = new HashMap<>();

    private boolean disabled = true;
    private float stopwatch = 0f;
    private float currentThreat = 0f;

    // Public-access fields with the last recorded interal variables in them, so the debug mode plugin is able to
    // show the values. None of these are actually used in calculations, modifying them won't cause issues.
    public float lastAmbientThreat = 0f;
    public float lastOutmatchPercent = 0f;
    public float lastSituationalThreat = 0f;
    public float lastLocalThreat = 0f;
    public float lastTotalThreat = 0f;
    public float lastCurrentThreat = 0f;

    @Override
    public void init(CombatEngineAPI engine) {
        if (RSTM_Utils.isInCombat()) {
            log("[RSTM] Threat assessor initializing");
            disabled = false;

            CombatFleetManagerAPI enemyFleet = engine.getFleetManager(FleetSide.ENEMY);

            log("[RSTM] Assessing enemy fleet");
            for (FleetMemberAPI ship : enemyFleet.getReservesCopy()) {
                logv(" ");
                fleetMemberThreatRatings.put(ship.getId(), initialAssessFleetMember(ship));
            }

            currentThreatAssessor = this;
        }
    }

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

        for (ShipAPI ship : Global.getCombatEngine().getShips()) {
            if (ship.getOwner() == 0) continue;
            if (!ship.isAlive()) continue;

            FleetMemberAPI fleetMember = CombatUtils.getFleetMember(ship);

            if (fleetMember == null) continue;

            assessVariableThreatModifiers(ship, fleetMember);
        }

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

        MutableStat localThreat = new MutableStat(getBaseLocalThreat());
        localThreat.modifyMult("localThreatGlobalMultiplier", settings.localThreatGlobalMultiplier);

        // Local threat is always 0 if there is no player ship so we can use that to tell if there is one
        if (localThreat.getBaseValue() != 0f) {
            ShipAPI playerShip = Global.getCombatEngine().getPlayerShip();

            if (playerShip.getFluxTracker().isOverloaded()) {
                localThreat.modifyPercent("overloadMod", settings.localThreatOverloadMod);
            }

            if (playerShip.getEngineController().isFlamedOut()) {
                localThreat.modifyPercent("flamedOutMod", settings.localThreatFlamedOutMod);
            }

            if (playerShip.getFluxTracker().isVenting()) {
                localThreat.modifyPercent("ventingMod", settings.localThreatVentingMod);
            }
        }

        lastLocalThreat = localThreat.getModifiedValue();

        MutableStat totalThreat = new MutableStat(ambientThreat.getModifiedValue()
                + situationalThreat.getModifiedValue() + localThreat.getModifiedValue());
        totalThreat.modifyMult("globalThreatMultiplier", settings.globalThreatMultiplier);

        lastTotalThreat = totalThreat.getModifiedValue();

        float desiredThreat = totalThreat.getModifiedValue();
        float threatDelta = (desiredThreat - currentThreat);

        if (Math.abs(threatDelta) < MAXIMUM_THREAT_DELTA * amount) {
            currentThreat = desiredThreat;
        } else {
            currentThreat += MAXIMUM_THREAT_DELTA * amount * Math.signum(threatDelta);
        }

        lastCurrentThreat = currentThreat;

        musicPlayer.setThreat(currentThreat);
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

        float stationThreat = 0f;
        for (DeployedFleetMemberAPI station : enemyFleet.getStations()) {
            stationThreat += settings.ambientThreatStation;
        }

        return enemyReservesThreat.getModifiedValue() + enemyDeployedThreat.getModifiedValue() + stationThreat;
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

        float localThreat = 0f;

        for (ShipAPI ship : Global.getCombatEngine().getShips()) {
            if (ship.getOwner() == 0) continue;
            if (!ship.isAlive()) continue;

            localThreat += calculateShipLocalThreatContribution(ship).getModifiedValue();
        }

        return localThreat;
    }

    public MutableStat calculateShipLocalThreatContribution(ShipAPI ship) {
        // This shouldn't get called unless the player ship exists
        ShipAPI playerShip = Global.getCombatEngine().getPlayerShip();

        float maxRange = getShipMaxLocalThreatRange(ship);
        float shipRange = MathUtils.getDistance(playerShip.getLocation(), ship.getLocation());

        if (maxRange < shipRange)
            return new MutableStat(0f);

        if (ship.getHullSize() == ShipAPI.HullSize.FIGHTER) {
            FighterWingSpecAPI wingSpec = ship.getWing().getSpec();

            return new MutableStat(wingSpec.getOpCost(null) * settings.localThreatFighterThreatPerOP
                    / wingSpec.getNumFighters());
        } else {
            FleetMemberAPI fleetMember = CombatUtils.getFleetMember(ship);

            if (fleetMember == null) return new MutableStat(0f);

            MutableStat localThreat = getFleetMemberThreatRatingCopy(fleetMember);

            float minRange = getShipMinLocalThreatRange(ship);

            float rangeMult;
            if (shipRange < minRange) {
                rangeMult = 1f;
            } else {
                rangeMult = 1f - (shipRange - minRange) / (maxRange - minRange);
            }

            localThreat.modifyMult("rangeMultiplier", rangeMult);

            return localThreat;
        }
    }

    private float getShipMaxLocalThreatRange(ShipAPI ship) {
        switch (ship.getHullSize()) {
            case FRIGATE:
                return settings.localThreatFrigateMaxRange;
            case DESTROYER:
                return settings.localThreatDestroyerMaxRange;
            case CRUISER:
                return settings.localThreatCruiserMaxRange;
            case CAPITAL_SHIP:
                return settings.localThreatCapitalMaxRange;
            case FIGHTER:
                return settings.localThreatFighterRange;
            default:
                return 0f;
        }
    }

    private float getShipMinLocalThreatRange(ShipAPI ship) {
        switch (ship.getHullSize()) {
            case FRIGATE:
                return settings.localThreatFrigateMinRange;
            case DESTROYER:
                return settings.localThreatDestroyerMinRange;
            case CRUISER:
                return settings.localThreatCruiserMinRange;
            case CAPITAL_SHIP:
                return settings.localThreatCapitalMinRange;
            case FIGHTER:
                return settings.localThreatFighterRange;
            default:
                return 0f;
        }
    }

    private void assessVariableThreatModifiers(ShipAPI ship, FleetMemberAPI fleetMember) {
        MutableStat threatRating = getFleetMemberThreatRating(fleetMember);

        float combatReadiness = ship.getCurrentCR();
        float percentModifierCR = 0f;
        if (!MathUtils.equals(combatReadiness, 0.7f)) {
            if (combatReadiness > 0.7f) {
                percentModifierCR = settings.threatModMaxCR * (combatReadiness - 0.7f) / 0.3f;
            } else {
                percentModifierCR = settings.threatModMinCR * (0.7f - combatReadiness) / 0.7f;
            }
        }

        threatRating.modifyPercent("combatReadiness", percentModifierCR);
        threatRating.modifyPercent("hullLevel", settings.threatModMinHull * (1f - ship.getHullLevel()));
    }

    private MutableStat getFleetMemberThreatRating(FleetMemberAPI ship) {
        if (ship == null) {
            log("[RSTM] Attempted to get threat rating of null fleet member, returning 0");
            return new MutableStat(0f);
        }

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

    // TODO: Increased threat for specific ships
    private MutableStat initialAssessFleetMember(FleetMemberAPI ship) {
        MutableStat threat = new MutableStat(0f);

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
