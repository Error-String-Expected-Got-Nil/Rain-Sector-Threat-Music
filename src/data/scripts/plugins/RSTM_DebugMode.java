package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import data.scripts.RSTM.RSTM_LayeredMusicTrack;
import data.scripts.RSTM.RSTM_Utils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class RSTM_DebugMode extends BaseEveryFrameCombatPlugin {
    private static final char jukeboxToggleKey = 'p';
    private static final char jukeboxDisplayKey = 'k';
    private static final char jukeboxThreatLevelIncKey = 'o';
    private static final char jukeboxThreatLevelDecKey = 'm';
    private static final char jukeboxTrackIncKey = 'l';
    private static final char jukeboxTrackDecKey = 'j';
    private static final char threatAssessorDisplayToggleKey = 'n';
    private static final char showNearestShipThreatKey = 'b';

    private static final int minimumTrack = -1;
    private static int maximumTrack = -1;
    private static final int minimumThreatLevel = -1;
    private static int maximumThreatLevel = -1;

    private boolean disabled = true;

    private boolean jukeboxOn = false;
    private boolean statusDisplayOn = true;
    private boolean threatAssessorDisplayOn = true;
    private final Color floatingThreatTextColor = new Color(145, 255, 200, 179);
    private int trackNumber = -1;
    private int threatLevel = -1;

    @Override
    public void init(CombatEngineAPI engine) {
        if (RSTMModPlugin.isDebugModeOn() && RSTM_Utils.isInCombat()) disabled = false;

        if (disabled) return;

        maximumTrack = RSTMModPlugin.tracklist.size() - 1;
        maximumThreatLevel = -1;
        for (String trackID : RSTMModPlugin.tracklist) {
            RSTM_LayeredMusicTrack track = RSTMModPlugin.musicTracks.get(trackID);
            int trackMaxThreat = track.getLayerCount() - 1;
            if (trackMaxThreat > maximumThreatLevel) maximumThreatLevel = trackMaxThreat;
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (disabled) return;
        RSTM_MusicPlayer player = RSTM_MusicPlayer.getCurrentMusicPlayer();

        if (player == null) return;

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

        RSTM_ThreatAssessor assessor = RSTM_ThreatAssessor.currentThreatAssessor;

        if (threatAssessorDisplayOn && assessor != null) {
            engine.maintainStatusForPlayerShip("RSTM_threatAssessorDisplay_trackThreatLevel", "",
                    "Current Track Threat Level:", Integer.toString(player.getTrackThreatLevel()), false);
            engine.maintainStatusForPlayerShip("RSTM_threatAssessorDisplay_localThreat", "",
                    "Assessed Local Threat:", Float.toString(assessor.lastLocalThreat), false);
            engine.maintainStatusForPlayerShip("RSTM_threatAssessorDisplay_situationalThreat", "",
                    "Assessed Situational Threat:", Float.toString(assessor.lastSituationalThreat), false);
            engine.maintainStatusForPlayerShip("RSTM_threatAssessorDisplay_ambientThreat", "",
                    "Assessed Ambient Threat:", Float.toString(assessor.lastAmbientThreat), false);
            engine.maintainStatusForPlayerShip("RSTM_threatAssessorDisplay_currentThreat", "",
                    "Current Threat:", Float.toString(assessor.lastCurrentThreat), false);
            engine.maintainStatusForPlayerShip("RSTM_threatAssessorDisplay_totalThreat", "",
                    "Total Threat:", Float.toString(assessor.lastTotalThreat), false);
            engine.maintainStatusForPlayerShip("RSTM_threatAssessorDisplay_isOn", "",
                    "Threat Assessor Display On", "Press n to toggle visibility", false);
        }
    }

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
        RSTM_MusicPlayer player = RSTM_MusicPlayer.getCurrentMusicPlayer();
        if (player == null) return;

        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();

        for (InputEventAPI input : events) {
            if (input.isConsumed() || !input.isKeyDownEvent()) continue;

            switch (Character.toLowerCase(input.getEventChar())) {
                case jukeboxToggleKey:
                    if (jukeboxOn) {
                        jukeboxOn = false;
                        soundPlayer.playUISound("ui_right_click_command_given", 1f, 1f);
                        player.exitOverrideMode();
                    } else {
                        jukeboxOn = true;
                        soundPlayer.playUISound("ui_right_click_command_given", 1f, 1f);
                        player.enterOverrideMode();

                        player.setOverrideThreatLevel(threatLevel);

                        if (trackNumber == -1) {
                            player.switchTrack(null);
                        } else {
                            player.switchTrack(RSTMModPlugin.musicTracks.get(RSTMModPlugin.tracklist.get(trackNumber)));
                        }
                    }
                    break;

                case jukeboxDisplayKey:
                    statusDisplayOn = !statusDisplayOn;
                    soundPlayer.playUISound("ui_right_click_command_given", 1f, 1f);
                    break;

                case jukeboxThreatLevelIncKey:
                    if (threatLevel < maximumThreatLevel) {
                        soundPlayer.playUISound("ui_right_click_command_given", 1f, 1f);
                        threatLevel++;
                        player.setOverrideThreatLevel(threatLevel);
                    } else {
                        soundPlayer.playUISound("ui_out_of_command_points", 1f, 1f);
                    }
                    break;

                case jukeboxThreatLevelDecKey:
                    if (threatLevel > minimumThreatLevel) {
                        soundPlayer.playUISound("ui_right_click_command_given", 1f, 1f);
                        threatLevel--;
                        player.setOverrideThreatLevel(threatLevel);
                    } else {
                        soundPlayer.playUISound("ui_out_of_command_points", 1f, 1f);
                    }
                    break;

                case jukeboxTrackIncKey:
                    if (trackNumber < maximumTrack) {
                        soundPlayer.playUISound("ui_create_waypoint", 1f, 1f);
                        trackNumber++;

                        RSTM_LayeredMusicTrack nextTrack;
                        if (trackNumber == -1) {
                            nextTrack = null;
                        } else {
                            nextTrack = RSTMModPlugin.musicTracks.get(RSTMModPlugin.tracklist.get(trackNumber));
                        }

                        if (jukeboxOn) player.switchTrack(nextTrack);
                    } else {
                        soundPlayer.playUISound("ui_selection_cleared", 1f, 1f);
                    }
                    break;

                case jukeboxTrackDecKey:
                    if (trackNumber > minimumTrack) {
                        soundPlayer.playUISound("ui_create_waypoint", 1f, 1f);
                        trackNumber--;

                        RSTM_LayeredMusicTrack nextTrack;
                        if (trackNumber == -1) {
                            nextTrack = null;
                        } else {
                            nextTrack = RSTMModPlugin.musicTracks.get(RSTMModPlugin.tracklist.get(trackNumber));
                        }

                        if (jukeboxOn) player.switchTrack(nextTrack);
                    } else {
                        soundPlayer.playUISound("ui_selection_cleared", 1f, 1f);
                    }
                    break;

                case threatAssessorDisplayToggleKey:
                    threatAssessorDisplayOn = !threatAssessorDisplayOn;
                    soundPlayer.playUISound("ui_right_click_command_given", 1f, 1f);
                    break;

                case showNearestShipThreatKey:
                    if (RSTM_ThreatAssessor.currentThreatAssessor == null) return;

                    CombatEngineAPI engine = Global.getCombatEngine();

                    ShipAPI playerShip = engine.getPlayerShip();

                    if (playerShip == null || playerShip.isShuttlePod() || !playerShip.isAlive()) return;

                    soundPlayer.playUISound("ui_right_click_command_given", 1f, 1f);

                    Vector2f playerMouse = playerShip.getMouseTarget();

                    ShipAPI nearestShip = null;
                    float nearestShipDistance = Float.MAX_VALUE;
                    for (ShipAPI ship : engine.getShips()) {
                        float distance = MathUtils.getDistance(playerMouse, ship.getLocation());

                        if (distance < nearestShipDistance) {
                            nearestShip = ship;
                            nearestShipDistance = distance;
                        }
                    }

                    if (nearestShip != null) {
                        FleetMemberAPI fleetMember = CombatUtils.getFleetMember(nearestShip);

                        if (fleetMember != null) {
                            MutableStat ambientThreat = RSTM_ThreatAssessor.currentThreatAssessor
                                    .getFleetMemberThreatRatingCopy(fleetMember);
                            MutableStat localThreat = RSTM_ThreatAssessor.currentThreatAssessor
                                    .calculateShipLocalThreatContribution(nearestShip);

                            String floatingTextString =
                                      "Ship: " + fleetMember + "\n"
                                    + "Threat Rating: " + ambientThreat.getModifiedValue() + "\n"
                                    + "Local Threat Contribution: " + localThreat.getModifiedValue();

                            engine.addFloatingText(playerMouse, floatingTextString, 30f, floatingThreatTextColor,
                                    nearestShip, 1f, 0f);
                        }
                    }

                    break;
            }
        }
    }

    private String getJukeboxStatusString() {
        if (jukeboxOn) return "on";
        return "off";
    }

    private String getCurrentTrackName() {
        if (trackNumber == -1) return "none";
        return RSTMModPlugin.tracklist.get(trackNumber);
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
