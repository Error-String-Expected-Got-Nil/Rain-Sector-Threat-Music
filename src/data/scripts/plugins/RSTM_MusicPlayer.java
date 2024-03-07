package data.scripts.plugins;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.List;

public class RSTM_MusicPlayer extends BaseEveryFrameCombatPlugin {
    private static RSTM_MusicPlayer currentMusicPlayer = null;

    @Override
    public void init(CombatEngineAPI engine) {

    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {

    }
}
