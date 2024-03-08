package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import data.scripts.RSTM.RSTM_LayeredMusicTrack;
import data.scripts.RSTM.RSTM_Utils;
import org.apache.log4j.Logger;

import java.util.List;

public class RSTM_MusicPlayer extends BaseEveryFrameCombatPlugin {
    private static RSTM_MusicPlayer currentMusicPlayer = null;
    private static RSTM_LayeredMusicTrack currentTrack = null;

    private boolean disabled = true;
    private float threat = 0f;

    private float stopwatch = 0f;

    private boolean overrideMode = false;
    private int overrideThreatLevel = -1;

    @Override
    public void init(CombatEngineAPI engine) {
        if (RSTM_Utils.isInCombat()) {
            switchTrack(selectTrack());
            currentMusicPlayer = this;
            disabled = false;
            Global.getSoundPlayer().setSuspendDefaultMusicPlayback(true);
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (disabled) return;

        // RSTM_Utils.isCombatEnding() also triggers on battle start for reasons so we just wait 0.25 seconds before
        // the battle can be considered "ended." Probably won't cause issues in most practical circumstances.
        if (stopwatch < 0.45f) stopwatch += amount;

        if (RSTM_Utils.isCombatEnding() && stopwatch > 0.40f) {
            Logger logger = Global.getLogger(this.getClass());
            logger.info("[RSTM] Music player plugin thinks combat is ending");

            disabled = true;
            if (currentTrack != null) currentTrack.deactivate();
            currentTrack = null;
            currentMusicPlayer = null;
            Global.getSoundPlayer().setSuspendDefaultMusicPlayback(false);
            return;
        }

        if (currentTrack == null) return;

        currentTrack.pulse(amount);
    }

    public void enterOverrideMode() {
        overrideMode = true;
        overrideThreatLevel = -1;
        if (currentTrack != null) currentTrack.setThreatLevel(-1);
    }

    public void exitOverrideMode() {
        overrideMode = false;
        overrideThreatLevel = -1;
        if (currentTrack != null) currentTrack.setThreatLevelByThreatCurve(threat);
    }

    public void setOverrideThreatLevel(int threatLevel) {
        overrideThreatLevel = threatLevel;

        if (overrideMode && currentTrack != null) {
            currentTrack.setThreatLevel(overrideThreatLevel);
        }
    }

    public int getOverrideThreatLevel() {
        return overrideThreatLevel;
    }

    public void switchTrack(RSTM_LayeredMusicTrack track) {
        if (currentTrack != null) currentTrack.deactivate();
        currentTrack = track;

        if (currentTrack == null) return;

        currentTrack.activate();

        if (overrideMode) {
            currentTrack.setThreatLevel(overrideThreatLevel);
        } else {
            currentTrack.setThreatLevelByThreatCurve(threat);
        }
    }

    public void setThreat(float threat) {
        this.threat = threat;

        if (!overrideMode && currentTrack != null) {
            currentTrack.setThreatLevelByThreatCurve(threat);
        }
    }

    public boolean isDisabled() {
        return disabled;
    }

    public static RSTM_MusicPlayer getCurrentMusicPlayer() {
        return currentMusicPlayer;
    }

    // Debug placeholder for now. Will make a random weighted selection based on combat participants and whether the
    // battle is a simulator fight in the future.
    private RSTM_LayeredMusicTrack selectTrack() {
        return RSTMModPlugin.musicTracks.get("sky_islands");
    }
}
