package com.shadowlordalpha.sc2.manager;

import com.github.ocraft.s2client.bot.ClientError;
import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Upgrade;
import com.github.ocraft.s2client.protocol.observation.Alert;

import java.util.List;

public interface Component {

    /**
     * This method is called after we have added the agent and outside of a loop of all managers allowing us to add
     * any components needed as well as check any components that have already been added. If you need a specific
     * component it is recommended to check for it in one of the GameStart methods and not here as it could be added
     * after this component is added in which case you will end up with duplicate components.
     */
    default void onInitialized(S2AgentManager agent) {

    }

    default void onGameFullStart(S2AgentManager agent) {

    }

    default void onGameStart(S2AgentManager agent) {

    }

    default void onStep(S2Agent agent) {

    }

    default void onUnitEnterVision(S2Agent agent, UnitInPool unitInPool) {

    }

    default void onUnitCreated(S2Agent agent, UnitInPool unitInPool) {

    }

    default void onUnitDestroyed(S2Agent agent, UnitInPool unitInPool) {

    }

    default void onUnitIdle(S2Agent agent, UnitInPool unitInPool) {

    }

    default void onBuildingConstructionComplete(S2Agent agent, UnitInPool unitInPool) {

    }

    default void onUpgradeCompleted(S2Agent agent, Upgrade upgrade) {

    }

    default void onGameEnd(S2Agent agent) {

    }

    default void onNydusDetected(S2Agent agent) {

    }

    default void onNuclearLaunchDetected(S2Agent agent) {

    }

    default void onError(S2Agent agent, List<ClientError> clientErrors, List<String> protocolErrors) {

    }

    default void onAlert(S2Agent agent, Alert alert) {

    }
}
