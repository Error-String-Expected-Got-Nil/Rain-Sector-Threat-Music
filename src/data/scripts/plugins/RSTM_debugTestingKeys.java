package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import org.json.JSONException;

public class RSTM_debugTestingKeys extends BaseEveryFrameCombatPlugin {
    private static final char testOn1 = 'j';
    private static final char testOff1 = 'n';
    private static final char testOn2 = 'k';
    private static final char testOff2 = 'm';

    private static final char test3 = 'l';

    private SoundAPI debugSong1 = null;
    private SoundAPI debugSong2 = null;

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
        Logger logger = Global.getLogger(this.getClass());
        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();

        for (InputEventAPI event : events) {
            if (event.isKeyDownEvent()) {
                if (event.isConsumed()) continue;

                char key = event.getEventChar();
                if (key == testOn1) {
                    logger.info("RSTM debug ON 1 key pressed");
                    if (debugSong1 == null || !debugSong1.isPlaying()) {
                        debugSong1 = soundPlayer.playUISound("RSTM_debug_HI_SHAKER", 1, 1);
                    } else {
                        logger.info("Already playing song 1");
                    }
                } else if (key == testOff1) {
                    logger.info("RSTM debug OFF 1 key pressed");
                    if (debugSong1 != null && debugSong1.isPlaying()) {
                        debugSong1.stop();
                    } else {
                        logger.info("Not playing song 1");
                    }
                } else if (key == testOn2) {
                    logger.info("RSTM debug ON 2 key pressed");
                    if (debugSong2 == null || !debugSong2.isPlaying()) {
                        debugSong2 = soundPlayer.playUISound("RSTM_debug_HI_PERCUSSION", 1, 1);
                    } else {
                        logger.info("Already playing song 2");
                    }
                } else if (key == testOff2) {
                    logger.info("RSTM debug OFF 2 key pressed");
                    if (debugSong2 != null && debugSong2.isPlaying()) {
                        debugSong2.stop();
                    } else {
                        logger.info("Not playing song 2");
                    }
                } else if (key == test3) {
                    try {
                        Object value = Global.getSettings().getSettingsJSON().get("campaignMusicVolumeMult");
                        logger.info("[RSTM] campaignMusicVolumeMult value: " + value);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}
