package data.scripts.RSTM;

import com.fs.starfarer.api.Global;
import data.scripts.plugins.RSTMModPlugin;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.log4j.Logger;

import static data.scripts.RSTM.RSTM_MusicLayer.RSTM_LayerState.*;

public class RSTM_MusicLayer {
    enum RSTM_LayerState {
        INACTIVE,
        FADE_IN,
        FADE_OUT,
        IDLE
    }

    private final String layerID;
    private final String soundID;
    private final float fadeInTime;
    private final float fadeOutTime;
    private final float volumeModifier;

    private RSTM_LayerState state = INACTIVE;
    private float stopwatch = 0f;
    private float currentVolume = 0f;
    private boolean muted = false;

    public RSTM_MusicLayer(JSONObject settings, JSONObject defaultSettings) {
        Logger logger = Global.getLogger(this.getClass());

        try {
            layerID = settings.getString("id");
            soundID = settings.getString("sound");
        } catch (JSONException e) {
            logger.error("[RSTM] Failed to load music layer ID or sound during initial load");
            throw new RuntimeException(e);
        }

        try {
            if (settings.has("fadeInTime")) {
                fadeInTime = (float) settings.getDouble("fadeInTime");
            } else {
                fadeInTime = (float) defaultSettings.getDouble("fadeInTime");
            }

            if (settings.has("fadeOutTime")) {
                fadeOutTime = (float) settings.getDouble("fadeOutTime");
            } else {
                fadeOutTime = (float) defaultSettings.getDouble("fadeOutTime");
            }

            if (settings.has("volumeModifier")) {
                volumeModifier = (float) settings.getDouble("volumeModifier");
            } else {
                volumeModifier = (float) defaultSettings.getDouble("volumeModifier");
            }
        } catch (JSONException e) {
            logger.error("[RSTM] Failed to load layer settings, a setting either wasn't provided and the default " +
                    "failed to load, or it was provided and failed to load for some reason");
            throw new RuntimeException(e);
        }

        logger.info("[RSTM] Successfully loaded music layer " + layerID + " with sound " + soundID);
    }

    public void pulse(float deltaTime) {
        switch(state) {
            case INACTIVE:
                return;
            case FADE_IN:
                stopwatch += deltaTime;
                currentVolume = getMaxVolume() * Math.min(stopwatch / fadeInTime, 1f);

                if (stopwatch > fadeInTime) {
                    state = IDLE;
                    stopwatch = 0f;
                }

                break;
            case FADE_OUT:
                stopwatch += deltaTime;
                currentVolume = getMaxVolume() * (1f - Math.min(stopwatch / fadeOutTime, 1f));

                if (stopwatch > fadeOutTime) {
                    state = IDLE;
                    stopwatch = 0f;
                    muted = true;
                }

                break;
            case IDLE:
                currentVolume = getMaxVolume();
        }

        // Thank you to LazyWizard for suggesting that I use playUILoop() instead of playUISound(), this fixed
        // a major bug and made the code neater overall.
        Global.getSoundPlayer().playUILoop(soundID, 1f, currentVolume);
    }

    public void activate() {
        muted = true;
        state = IDLE;
    }

    public void deactivate() {
        stopwatch = 0f;
        state = INACTIVE;
        muted = false;
        currentVolume = 0f;
    }

    public void mute() {
        stopwatch = 0f;
        state = FADE_OUT;
    }

    public void unmute() {
        if (muted) {
            muted = false;
            stopwatch = 0f;
            state = FADE_IN;
        }
    }

    public String getLayerID() {
        return layerID;
    }

    private float getMaxVolume() {
        if (muted) return 0f;

        return volumeModifier * RSTMModPlugin.getGlobalVolumeModifier();
    }
}
