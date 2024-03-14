package data.scripts.RSTM;

import com.fs.starfarer.api.Global;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// This class is just a container for all the threat assessor's settings because I didn't want to clutter the mod
// plugin with almost four dozen fields.
public class RSTM_ThreatAssessorSettings {
    public final boolean doLogging;
    public final boolean verboseLogging;

    public final float globalThreatMultiplier;

    public final float threatPerDP;
    public final float threatFrigate;
    public final float threatDestroyer;
    public final float threatCruiser;
    public final float threatCapital;

    public final float threatModPerDMod;
    public final float threatModPerSMod;
    public final float threatModPerOfficerLevel;
    public final float threatModNoncombat;

    public final float threatModMaxCR;
    public final float threatModMinCR;
    public final float threatModMinHull;

    public final float ambientThreatGlobalMultiplier;
    public final float ambientThreatModReserved;

    public final float outmatchValueFrigate;
    public final float outmatchValueDestroyer;
    public final float outmatchValueCruiser;
    public final float outmatchValueCapital;
    public final float outmatchMinMod;
    public final float outmatchMaxMod;
    public final float outmatchMinRatio;
    public final float outmatchMaxRatio;
    public final float outmatchReserveMultiplier;

    public final float situationalThreatGlobalMultiplier;
    public final float situationalThreatOverload;
    public final float situationalThreatFlamedOut;
    public final float situationalThreatAtMaxHardFlux;
    public final float situationalThreatAtMaxSoftFlux;
    public final float situationalThreatAtMinHull;

    public final float localThreatGlobalMultiplier;
    public final float localThreatFrigateMaxRange;
    public final float localThreatDestroyerMaxRange;
    public final float localThreatCruiserMaxRange;
    public final float localThreatCapitalMaxRange;
    public final float localThreatFrigateMinRange;
    public final float localThreatDestroyerMinRange;
    public final float localThreatCruiserMinRange;
    public final float localThreatCapitalMinRange;
    public final float localThreatFighterRange;
    public final float localThreatFighterThreatPerOP;
    public final float localThreatOverloadMod;
    public final float localThreatFlamedOutMod;
    public final float localThreatVentingMod;

    public RSTM_ThreatAssessorSettings(JSONObject settings) {
        try {
            doLogging = settings.getBoolean("doLogging");
            verboseLogging = settings.getBoolean("verboseLogging");

            globalThreatMultiplier = (float) settings.getDouble("globalThreatMultiplier");

            JSONObject baseline = settings.getJSONObject("shipBaseThreatRatingSettings");

            threatPerDP = (float) baseline.getDouble("threatPerDP");
            JSONArray threatPerHullSize = baseline.getJSONArray("threatPerHullSize");
            threatFrigate = (float) threatPerHullSize.getDouble(0);
            threatDestroyer = (float) threatPerHullSize.getDouble(1);
            threatCruiser = (float) threatPerHullSize.getDouble(2);
            threatCapital = (float) threatPerHullSize.getDouble(3);

            threatModPerDMod = (float) baseline.getDouble("threatModPerDMod");
            threatModPerSMod = (float) baseline.getDouble("threatModPerSMod");
            threatModPerOfficerLevel = (float) baseline.getDouble("threatModPerOfficerLevel");
            threatModNoncombat = (float) baseline.getDouble("threatModNoncombat");

            threatModMaxCR = (float) baseline.getDouble("threatModMaxCR");
            threatModMinCR = (float) baseline.getDouble("threatModMinCR");
            threatModMinHull = (float) baseline.getDouble("threatModMinHull");

            JSONObject ambient = settings.getJSONObject("ambientThreatSettings");

            ambientThreatGlobalMultiplier = (float) ambient.getDouble("globalMultiplier");
            ambientThreatModReserved = (float) ambient.getDouble("reservedMod");

            JSONArray outmatchHullValue = ambient.getJSONArray("outmatchHullValue");
            outmatchValueFrigate = (float) outmatchHullValue.getDouble(0);
            outmatchValueDestroyer = (float) outmatchHullValue.getDouble(1);
            outmatchValueCruiser = (float) outmatchHullValue.getDouble(2);
            outmatchValueCapital = (float) outmatchHullValue.getDouble(3);

            outmatchMinMod = (float) ambient.getDouble("outmatchMinMod");
            outmatchMaxMod = (float) ambient.getDouble("outmatchMaxMod");
            outmatchMinRatio = (float) ambient.getDouble("outmatchMinRatio");
            outmatchMaxRatio = (float) ambient.getDouble("outmatchMaxRatio");

            outmatchReserveMultiplier = (float) ambient.getDouble("outmatchReserveMultiplier");

            JSONObject situational = settings.getJSONObject("situationalThreatSettings");

            situationalThreatGlobalMultiplier = (float) situational.getDouble("globalMultiplier");
            situationalThreatOverload = (float) situational.getDouble("overload");
            situationalThreatFlamedOut = (float) situational.getDouble("flamedOut");
            situationalThreatAtMaxHardFlux = (float) situational.getDouble("atMaxHardFlux");
            situationalThreatAtMaxSoftFlux = (float) situational.getDouble("atMaxSoftFlux");
            situationalThreatAtMinHull = (float) situational.getDouble("atMinHull");

            JSONObject local = settings.getJSONObject("localThreatSettings");

            localThreatGlobalMultiplier = (float) local.getDouble("globalMultiplier");
            JSONArray maxLocalThreatRange = local.getJSONArray("maxRange");
            localThreatFrigateMaxRange = (float) maxLocalThreatRange.getDouble(0);
            localThreatDestroyerMaxRange = (float) maxLocalThreatRange.getDouble(1);
            localThreatCruiserMaxRange = (float) maxLocalThreatRange.getDouble(2);
            localThreatCapitalMaxRange = (float) maxLocalThreatRange.getDouble(3);
            JSONArray minLocalThreatRange = local.getJSONArray("minRange");
            localThreatFrigateMinRange = (float) minLocalThreatRange.getDouble(0);
            localThreatDestroyerMinRange = (float) minLocalThreatRange.getDouble(1);
            localThreatCruiserMinRange = (float) minLocalThreatRange.getDouble(2);
            localThreatCapitalMinRange = (float) minLocalThreatRange.getDouble(3);
            localThreatFighterRange = (float) local.getDouble("fighterThreatRange");
            localThreatFighterThreatPerOP = (float) local.getDouble("fighterThreatPerOP");
            localThreatOverloadMod = (float) local.getDouble("overloadMod");
            localThreatFlamedOutMod = (float) local.getDouble("flamedOutMod");
            localThreatVentingMod = (float) local.getDouble("ventingMod");
        } catch (JSONException e) {
            Global.getLogger(this.getClass()).error("[RSTM] Failed to load threat assessor settings");
            throw new RuntimeException(e);
        }
    }
}
