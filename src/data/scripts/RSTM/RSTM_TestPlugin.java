package data.scripts.RSTM;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.apache.log4j.Logger;

import java.util.List;

public class RSTM_TestPlugin extends BaseEveryFrameCombatPlugin {
    @Override
    public void init(CombatEngineAPI engine) {
        Logger logger = Global.getLogger(this.getClass());
        GameState state = Global.getCurrentState();
        if (state == GameState.COMBAT || (state == GameState.TITLE && engine.isSimulation())) {
            logger.info(" ");
            logger.info("[RSTM] TEST - IN BATTLE");
            logger.info(" ");
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        Logger logger = Global.getLogger(this.getClass());

        CombatEngineAPI engine = Global.getCombatEngine();
        GameState state = Global.getCurrentState();
        if (!(state == GameState.COMBAT || (state == GameState.TITLE && engine.isSimulation()))) return;

        if (RSTM_Utils.isCombatEnding()) {
            logger.info(" ");
            logger.info("[RSTM] TEST - COMBAT IS ENDING");
            logger.info(" ");
        }
    }
}

