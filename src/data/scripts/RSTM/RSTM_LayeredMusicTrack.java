package data.scripts.RSTM;

import com.fs.starfarer.api.Global;
import data.scripts.plugins.RSTMModPlugin;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RSTM_LayeredMusicTrack {
    private final Logger logger;
    private final List<RSTM_MusicLayer> layers = new ArrayList<>();
    private final List<Float> threatCurve = new ArrayList<>();

    private final String trackID;

    private boolean invalid = false;
    private int threatLevel = -1;

    public RSTM_LayeredMusicTrack(JSONObject settings) {
        logger = Global.getLogger(this.getClass());

        try {
            trackID = settings.getString("id");
        } catch (JSONException e) {
            logger.error("[RSTM] Failed to load track ID");
            throw new RuntimeException(e);
        }

        try {
            JSONArray layersArray = settings.getJSONArray("layers");

            for (int index = 0; index < layersArray.length(); index++) {
                String layerID = layersArray.getString(index);

                if (RSTMModPlugin.musicLayers.containsKey(layerID)) {
                    layers.add(RSTMModPlugin.musicLayers.get(layerID));
                } else {
                    invalid = true;
                    logger.warn("[RSTM] Failed to load music track " + trackID + ", could not find music layer " +
                            "with ID " + layerID);
                    return;
                }
            }
        } catch (JSONException e) {
            logger.error("[RSTM] Failed to load layers into track " + trackID);
            throw new RuntimeException(e);
        }

        try {
            JSONArray threatCurveArray = settings.getJSONArray("threatCurve");

            for (int index = 0; index < threatCurveArray.length(); index++) {
                float threatPoint = (float) threatCurveArray.getDouble(index);

                if (threatPoint < 0) {
                    invalid = true;
                    logger.warn("[RSTM] failed to load threat curve points into track " + trackID +
                            ", point at index " + index + " was less than 0. All threat curve points must " +
                            "be greater than or equal to 0.");
                    return;
                }

                if (index != 0) {
                    if (threatPoint < threatCurve.get(index - 1)) {
                        invalid = true;
                        logger.warn("[RSTM] Failed to load threat curve points into track " + trackID +
                                ", point at index " + index + " was less than previous point. Threat curve " +
                                "points must be equal to or greater than all previous points.");
                        return;
                    }
                }

                threatCurve.add(threatPoint);
            }
        } catch (JSONException e) {
            logger.error("[RSTM] Failed to load threat curve points into track " + trackID);
            throw new RuntimeException(e);
        }

        if (threatCurve.size() < layers.size()) {
            logger.warn("[RSTM] Track " + trackID + " has more music layers than threat curve points. Excess music " +
                    "layers can only be accessed by debug tools.");
        }

        if (threatCurve.size() > layers.size()) {
            invalid = true;
            logger.warn("[RSTM] Track " + trackID + " has more threat curve points than music layers. Track will be " +
                    "discarded, as the number of threat curve points must be less than or equal to the number of " +
                    "music layers.");
            return;
        }

        logger.info("[RSTM] Successfully loaded track " + trackID + " with " + layers.size() + " music layers.");
    }

    public void setThreatLevelByThreatCurve(float threat) {
        int threatSum = -1;

        for (float point : threatCurve) {
            if (threat >= point) {
                threatSum++;
            } else {
                break;
            }
        }

        setThreatLevel(threatSum);
    }

    public void stop() {
        setThreatLevel(-1);
    }

    public void forceStop() {
        threatLevel = -1;
        for (RSTM_MusicLayer layer : layers) {
            layer.hardStop();
        }
    }

    public int getThreatLevel() {
        return threatLevel;
    }

    public String getTrackID() {
        return trackID;
    }

    public boolean isInvalid() {
        return invalid;
    }

    public void setThreatLevel(int threatLevel) {
        this.threatLevel = threatLevel;

        if (threatLevel == -1) return;

        for (int index = 0; index < layers.size(); index++) {
            RSTM_MusicLayer layer = layers.get(index);

            if (index <= threatLevel) {
                layer.start();
            } else {
                layer.stop();
            }
        }
    }

    public void mute() {
        for (RSTM_MusicLayer layer : layers) {
            layer.mute();
        }
    }

    public void unmute() {
        for (RSTM_MusicLayer layer : layers) {
            layer.unmute();
        }
    }
}
