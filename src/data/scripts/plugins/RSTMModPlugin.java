package data.scripts.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import data.scripts.RSTM.RSTM_LayeredMusicTrack;
import data.scripts.RSTM.RSTM_MusicLayer;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RSTMModPlugin extends BaseModPlugin {
    public static final String LAYER_SETTINGS = "data/config/layeredMusic.json";
    public static final String RSTM_SETTINGS = "data/config/RSTMSettings.json";

    private static boolean debugMode;
    private static float globalVolumeModifier;

    public static final Map<String, RSTM_MusicLayer> musicLayers = new HashMap<>();
    public static final Map<String, RSTM_LayeredMusicTrack> musicTracks = new HashMap<>();
    public static final List<RSTM_LayeredMusicTrack> tracklist = new ArrayList<>();
    private static boolean hadInvalidTrack = false;

    @Override
    public void onApplicationLoad() throws Exception {
        Logger logger = Global.getLogger(this.getClass());
        logger.info(" ");
        logger.info(" ");
        logger.info("[RSTM] Initial loading begins:");

        JSONObject layerSettingsJSON;
        JSONObject RSTMSettingsJSON;

        // Load JSONs
        try {
            layerSettingsJSON = Global.getSettings().loadJSON(LAYER_SETTINGS);
            RSTMSettingsJSON = Global.getSettings().loadJSON(RSTM_SETTINGS);
        } catch (IOException | JSONException e) {
            logger.error("[RSTM] Failed to load settings JSON");
            throw new RuntimeException(e);
        }

        // Get general settings
        try {
            debugMode = RSTMSettingsJSON.getBoolean("debugMode");
            globalVolumeModifier = (float) RSTMSettingsJSON.getDouble("globalVolumeModifier");
        } catch (JSONException e) {
            logger.error("[RSTM] Failed to load RSTM general settings");
            throw new RuntimeException(e);
        }

        // Load music layer objects from JSON
        try {
            JSONArray layers = layerSettingsJSON.getJSONArray("layers");
            JSONObject defaultLayerSettings = layerSettingsJSON.getJSONObject("defaultLayerSettings");

            for (int index = 0; index < layers.length(); index++) {
                RSTM_MusicLayer layer = new RSTM_MusicLayer(layers.getJSONObject(index), defaultLayerSettings);

                String layerID = layer.getLayerID();
                if (musicLayers.containsKey(layerID)) {
                    logger.warn("[RSTM] Duplicate music layer ID " + layerID + " encountered, discarding.");
                    continue;
                }

                musicLayers.put(layerID, layer);
            }
        } catch (JSONException e) {
            logger.error("[RSTM] Failed to load music layers");
            throw new RuntimeException(e);
        }

        // Load layered music track objects from JSON
        try {
            JSONArray tracks = layerSettingsJSON.getJSONArray("tracks");

            for (int index = 0; index < tracks.length(); index++) {
                RSTM_LayeredMusicTrack track = new RSTM_LayeredMusicTrack(tracks.getJSONObject(index));

                String trackID = track.getTrackID();

                if (track.isInvalid()) {
                    hadInvalidTrack = true;
                } else {
                    if (musicTracks.containsKey(trackID)) {
                        logger.warn("[RSTM] Duplicate music track ID " + trackID + " encountered, discarding.");
                        continue;
                    }

                    musicTracks.put(trackID, track);
                    tracklist.add(track);
                }
            }
        } catch (JSONException e) {
            logger.error("[RSTM] Failed to load layered music tracks");
            throw new RuntimeException(e);
        }

        logger.info("[RSTM] Loaded " + tracklist.size() + " layered music tracks and " + musicLayers.size() +
                " music layers.");
        if (hadInvalidTrack) {
            logger.warn("[RSTM] Some music tracks did not load properly and were discarded! See warnings tagged " +
                    "with [RSTM] above for more details.");
        }
        logger.info(" ");
        logger.info(" ");
    }

    public static boolean isDebugModeOn() {
        return debugMode;
    }

    public static float getGlobalVolumeModifier() {
        return globalVolumeModifier;
    }
}
