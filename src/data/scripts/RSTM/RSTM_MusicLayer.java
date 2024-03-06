package data.scripts.RSTM;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
import data.scripts.plugins.RSTMModPlugin;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.log4j.Logger;

import static data.scripts.RSTM.RSTM_MusicLayer.RSTM_LayerState.*;

public class RSTM_MusicLayer {
    enum RSTM_LayerState {
        STOPPED,
        FADE_IN,
        PLAYING,
        FADE_OUT
    }

    private final Logger logger;

    private final String layerID;
    private final String soundID;
    private final float fadeInTime;
    private final float fadeOutTime;
    private final float volumeModifier;

    private RSTM_LayerState state = STOPPED;
    private float stopwatch = 0f;
    private float currentVolume = 0f;
    private SoundAPI sound = null;
    private boolean muted = false;

    public RSTM_MusicLayer(JSONObject settings, JSONObject defaultSettings) {
        logger = Global.getLogger(this.getClass());

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

    public void hardStart() {
        discardCurrentSound();
        sound = Global.getSoundPlayer().playUISound(soundID, 1f, getMaxVolume());
        state = PLAYING;
    }

    public void hardStop() {
        discardCurrentSound();
        state = STOPPED;
    }

    public void start() {
        if (state == FADE_IN || state == PLAYING) return;

        if (state == FADE_OUT) {
            stopwatch = fadeInTime * (1f - stopwatch / fadeOutTime);
            state = FADE_IN;
            return;
        }

        sound = Global.getSoundPlayer().playUISound(soundID, 1f, 0f);
        stopwatch = 0f;
        state = FADE_IN;
    }

    public void stop() {
        if (state == FADE_OUT || state == STOPPED) return;

        if (state == FADE_IN) {
            stopwatch = fadeOutTime * (1f - stopwatch / fadeInTime);
            state = FADE_OUT;
            return;
        }

        stopwatch = 0f;
        state = FADE_OUT;
    }

    public void pulse(float deltaTime) {
        float maxVolume = getMaxVolume();

        if (state != STOPPED && !sound.isPlaying()) {
            discardCurrentSound();
            sound = Global.getSoundPlayer().playUISound(soundID, 1f, currentVolume);
        }

        switch (state) {
            case STOPPED:
                return;
            case FADE_IN:
                stopwatch += deltaTime;
                currentVolume = maxVolume * stopwatch / fadeInTime;

                if (stopwatch > fadeInTime) {
                    stopwatch = 0f;
                    state = PLAYING;
                }

                break;
            case PLAYING:
                currentVolume = maxVolume;
                break;
            case FADE_OUT:
                stopwatch += deltaTime;
                currentVolume = maxVolume * (1f - stopwatch / fadeOutTime);

                if (stopwatch > fadeOutTime) {
                    discardCurrentSound();
                    currentVolume = 0f;
                    stopwatch = 0f;
                    state = STOPPED;
                    return;
                }

                break;
        }

        sound.setVolume(currentVolume);
    }

    public String getLayerID() {
        return layerID;
    }

    public void mute() {
        muted = true;
    }

    public void unmute() {
        muted = false;
    }

    private float getMaxVolume() {
        if (muted) return 0f;

        return volumeModifier * RSTMModPlugin.getGlobalVolumeModifier();
    }

    private void discardCurrentSound() {
        if (sound != null) {
            sound.stop();
            sound = null;
        }
    }
}
