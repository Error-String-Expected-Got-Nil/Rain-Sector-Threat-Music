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

// I am expecting this plugin to be substantially lower-quality than the other ones. I might redo all of this at some
// point, who knows.

public class RSTM_ThreatAssessor extends BaseEveryFrameCombatPlugin {
    private final Logger logger = Global.getLogger(this.getClass());
    private final RSTM_ThreatAssessorSettings settings = RSTMModPlugin.threatAssessorSettings;

    private final Map<String, MutableStat> fleetMemberThreatRatings = new HashMap<>();

    private boolean disabled = true;
    private float threat = 0f;

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
        }
    }

    // TODO: Account for stations, account for fighters, drones?

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (disabled) return;

        RSTM_MusicPlayer musicPlayer = RSTM_MusicPlayer.getCurrentMusicPlayer();

        if (musicPlayer == null || musicPlayer.isDisabled()) return;

        CombatEngineAPI engine = Global.getCombatEngine();

        MutableStat ambientThreat = new MutableStat(getBaseAmbientThreat());
        ambientThreat.modifyMult("ambientThreatGlobalMultiplier", settings.ambientThreatGlobalMultiplier);

        float outmatchModifier = getOutmatchModifier();
        ambientThreat.modifyPercent("outmatchModifier", outmatchModifier * 100f);
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
                    settings.ambientThreatModReserved * 100f);
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
            return Misc.interpolate(settings.outmatchMinMod, 1f, (outmatchRatio - settings.outmatchMinRatio)
                    / (1f - settings.outmatchMinRatio));
        }

        return Misc.interpolate(1f, settings.outmatchMaxMod, (outmatchRatio - 1f)
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

    // TODO: Right now, ships without a FleetMemberAPI cannot be assessed. Too bad!
    //  This should usually only happen if a ship was spawned directly, which I'm pretty sure shouldn't happen
    //  under normal circumstances. If it becomes a problem I'll fix it.

    private MutableStat getFleetMemberThreatRating(FleetMemberAPI ship) {
        String id = ship.getId();
        MutableStat threatRating = fleetMemberThreatRatings.get(id);

        if (threatRating == null) {
            threatRating = initialAssessFleetMember(ship);
            fleetMemberThreatRatings.put(id, threatRating);
        }

        return threatRating;
    }

    // Returns the assessed threat rating of the given fleet member.
    // Doesn't apply any extra modifiers, meant to get a baseline threat rating for the start of battle.
    private MutableStat initialAssessFleetMember(FleetMemberAPI ship) {
        MutableStat threat = new MutableStat(0f);

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
        threat.modifyPercent("threatModDMod", threatModDMod * 100f);

        float threatModSMod = 0f;
        for (int i = 0; i < variant.getSMods().size(); i++) {
            threatModSMod += settings.threatModPerSMod;
        }
        threat.modifyPercent("threatModSMod", threatModSMod * 100f);

        PersonAPI captain = ship.getCaptain();
        float threatModOfficerLevel = 0f;
        if (!captain.isPlayer() && captain.getPostId().equals("officer")) {
            for (int i = 0; i < captain.getStats().getLevel(); i++) {
                threatModOfficerLevel += settings.threatModPerOfficerLevel;
            }
        }
        threat.modifyPercent("threatModOfficerLevel", threatModOfficerLevel * 100f);

        float threatModNoncombat = 0f;
        if (variant.isCivilian()) threatModNoncombat = settings.threatModNoncombat;
        threat.modifyPercent("threatModNoncombat", threatModNoncombat * 100f);


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
