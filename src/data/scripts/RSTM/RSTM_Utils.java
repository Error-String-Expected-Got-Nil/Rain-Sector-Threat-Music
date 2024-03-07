package data.scripts.RSTM;

import com.fs.graphics.util.Fader;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatUIAPI;

public class RSTM_Utils {

    // This is an extremely scuffed function. Yes, it uses reflection to check the current fade-to-black progress to
    // tell if combat is ending. Why use this and not CombatEngineAPI.isCombatOver()? Because it doesn't work inside
    // a BaseEveryFrameCombatPlugin's advance() method, and there's no easily-accessible methods that activate when
    // combat ends.
    //
    // Starsector's API is well-designed and has no random glaring flaws whatsoever.
    // Addendum: This causes a crash if run on the title screen. That took me an hour to figure out.
    public static boolean isCombatEnding() {
        CombatEngineAPI engine = Global.getCombatEngine();
        CombatUIAPI ui = engine.getCombatUI();
        GameState state = Global.getCurrentState();
        if (ui == null || !(state == GameState.COMBAT || (state == GameState.TITLE && engine.isSimulation()))) {
            return false;
        } else {
            Fader fader = ((Fader) ReflectionUtils.get("fader", ui));
            if (fader == null) {
                return false;
            }
            return fader.getBrightness() < 0.1f;
        }
    }
}
