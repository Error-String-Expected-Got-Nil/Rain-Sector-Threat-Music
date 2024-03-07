package data.scripts.RSTM;

import com.fs.graphics.util.Fader;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatUIAPI;

public class RSTM_Utils {
    // This is an extremely scuffed function. Yes, it uses reflection to check the current fade-to-black progress to
    // tell if combat is ending. Why use this and not CombatEngineAPI.isCombatOver()? Because that doesn't work inside
    // a BaseEveryFrameCombatPlugin's advance() method, and there's no easily-accessible methods that activate when
    // combat ends.
    //
    // Starsector's API is well-designed and has no random glaring flaws whatsoever.
    // Addendum: This causes a crash if run on the title screen. That took me an hour to figure out.
    // Addendum: Note that this also triggers on the fade-in to combat. Account for accordingly.
    public static boolean isCombatEnding() {
        CombatUIAPI ui = Global.getCombatEngine().getCombatUI();
        if (ui == null || !isInCombat()) {
            return false;
        } else {
            Fader fader = ((Fader) ReflectionUtils.get("fader", ui));
            if (fader == null) {
                return false;
            }
            return fader.getBrightness() < 0.05f;
        }
    }

    // Returns whether or not the game is currently in combat, accounting for the fact that, for some reason,
    // using the simulator from the mission refit screen leaves GameState as TITLE.
    public static boolean isInCombat() {
        CombatEngineAPI engine = Global.getCombatEngine();
        GameState state = Global.getCurrentState();
        return (state == GameState.COMBAT || (state == GameState.TITLE && engine.isSimulation()));
    }
}
