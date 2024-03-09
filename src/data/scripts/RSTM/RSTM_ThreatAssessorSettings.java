package data.scripts.RSTM;

import com.fs.starfarer.api.Global;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// This class is just a container for all the threat assessor's settings because I didn't want to clutter the mod
// plugin with two dozen fields.
public class RSTM_ThreatAssessorSettings {
    public final boolean doLogging;
    public final boolean verboseLogging;

    public final float threatPerDP;
    public final float threatFrigate;
    public final float threatDestroyer;
    public final float threatCruiser;
    public final float threatCapital;

    public final float threatModPerDMod;
    public final float threatModPerSMod;
    public final float threatModPerOfficerLevel;
    public final float threatModNoncombat;

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

    public RSTM_ThreatAssessorSettings(JSONObject settings) {
        try {
            doLogging = settings.getBoolean("doLogging");
            verboseLogging = settings.getBoolean("verboseLogging");

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

            JSONObject ambient = settings.getJSONObject("ambientThreatSettings");

            ambientThreatGlobalMultiplier = (float) ambient.getDouble("ambientThreatGlobalMultiplier");
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
        } catch (JSONException e) {
            Global.getLogger(this.getClass()).error("[RSTM] Failed to load threat assessor settings");
            throw new RuntimeException(e);
        }
    }
}
