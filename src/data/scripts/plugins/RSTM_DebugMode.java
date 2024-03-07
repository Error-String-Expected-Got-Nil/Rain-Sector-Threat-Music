package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import data.scripts.RSTM.RSTM_LayeredMusicTrack;
import data.scripts.RSTM.RSTM_Utils;

import java.util.List;

public class RSTM_DebugMode extends BaseEveryFrameCombatPlugin {
    private static final char jukeboxToggleKey = 'p';
    private static final char jukeboxDisplayKey = 'k';
    private static final char jukeboxThreatLevelIncKey = 'o';
    private static final char jukeboxThreatLevelDecKey = 'm';
    private static final char jukeboxTrackIncKey = 'l';
    private static final char jukeboxTrackDecKey = 'j';

    private static final int minimumTrack = -1;
    private static int maximumTrack = -1;
    private static final int minimumThreatLevel = -1;
    private static int maximumThreatLevel = -1;

    private boolean disabled = true;

    private boolean jukeboxOn = true;
    private boolean statusDisplayOn = true;
    private int trackNumber = -1;
    private int threatLevel = -1;

    @Override
    public void init(CombatEngineAPI engine) {
        if (RSTMModPlugin.isDebugModeOn() && RSTM_Utils.isInCombat()) disabled = false;

        if (disabled) return;

        maximumTrack = RSTMModPlugin.tracklist.size() - 1;
        maximumThreatLevel = -1;
        for (RSTM_LayeredMusicTrack track : RSTMModPlugin.tracklist) {
            int trackMaxThreat = track.getLayerCount() - 1;
            if (trackMaxThreat > maximumThreatLevel) maximumThreatLevel = trackMaxThreat;
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (disabled) return;

        CombatEngineAPI engine = Global.getCombatEngine();

        if (statusDisplayOn) {
            engine.maintainStatusForPlayerShip("RSTM_jukebox_threatLevel", "",
                    "Threat Level:", getThreatLevelString() + " (inc. or dec. with o m)",
                    false);
            engine.maintainStatusForPlayerShip("RSTM_jukebox_currentTrack", "",
                    "Currently Playing:", "Track [" + getCurrentTrackName() + "] (switch with j l)",
                    false);
            engine.maintainStatusForPlayerShip("RSTM_jukebox_onOrOff", "",
                    "Jukebox Status:", "The jukebox is " + getJukeboxStatusString() + " (toggle with p)",
                    false);
            engine.maintainStatusForPlayerShip("RSTM_jukebox_statusDisplayOn", "",
                    "Jukebox Display On", "Press k to toggle visibility", false);
        }
    }

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
        for (InputEventAPI input : events) {
            if (input.isConsumed() || !input.isKeyDownEvent()) continue;

            switch (input.getEventChar()) {
                // TODO: Handle jukebox debug controls and such
            }
        }
    }

    private String getJukeboxStatusString() {
        if (jukeboxOn) return "on";
        return "off";
    }

    private String getCurrentTrackName() {
        if (trackNumber == -1) return "<none>";
        return RSTMModPlugin.tracklist.get(trackNumber).getTrackID();
    }

    private String getThreatLevelString() {
        switch (threatLevel) {
            case -1:
                return "-1 (No Threat)";
            case 0:
                return "0 (Present Threat)";
            case 1:
                return "1 (Minimal Threat)";
            case 2:
                return "2 (Minor Threat)";
            case 3:
                return "3 (Lesser Threat)";
            case 4:
                return "4 (Medium Threat)";
            case 5:
                return "5 (Greater Threat)";
            case 6:
                return "6 (Major Threat)";
            case 7:
                return "7 (Dangerous Threat)";
            case 8:
                return "8 (Lethal Threat)";
            default:
                return threatLevel + " (Extreme Threat)";
        }
    }
}
