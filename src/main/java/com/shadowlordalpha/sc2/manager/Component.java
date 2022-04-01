package com.shadowlordalpha.sc2.manager;

import com.github.ocraft.s2client.bot.ClientError;
import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Upgrade;
import com.github.ocraft.s2client.protocol.observation.Alert;

import java.util.List;

/**
 * A Component is some code that is allowed access to the agent and has methods that will be called by the corresponding
 * agent method. Components may execute in parallel however the Agent method will not complete until all Components
 * using a specific method, such as onStep, have also finished and returned. As such execution will be as slow or
 * slightly slower than the longest Component to execute the called method.
 *
 * @author Josh "ShadowLordAlpha" - 02-03-2022
 * @since 1.0.0
 * @version 1.0.0
 */
public interface Component {

    /**
     * This method is called after we have added the agent and outside of a loop of all managers allowing us to add
     * any components needed as well as check any components that have already been added. If you need a specific
     * component it is recommended to check for it in one of the GameStart methods and not here as it could be added
     * after this component is added in which case you will end up with duplicate components.
     *
     * @param agent The agent that this component belongs to
     */
    default void onInitialized(S2AgentManager agent) {

    }

    /**
     * This method is called when a new component is added to the list so that all of our other components can update
     * themselves and are able to get info from or talk with the correct components that are added or removed.
     *
     * @param agent The agent that this component belongs to
     * @param component The component that was added, this may be ourselves, ignore that one
     */
    default void onComponentAdded(S2AgentManager agent, Component component) {

    }

    /**
     *
     * @param agent The agent that this component belongs to
     * @param component The component that was removed, This will trigger for ourselves but it will not trigger after
     *                  that and the component is expected to cleanup and stop after being called with itself.
     */
    default void onComponentRemoved(S2AgentManager agent, Component component) {

    }

    /**
     * Called when a game is started after a load. Fast restarting will not call this.
     *
     * @param agent The agent that this component belongs to
     */
    default void onGameFullStart(S2AgentManager agent) {

    }

    /**
     * Called when a game is started or restarted.
     *
     * @param agent The agent that this component belongs to
     */
    default void onGameStart(S2AgentManager agent) {

    }

    /**
     * In non realtime games this function gets called after each step as indicated by step size. In realtime this
     * function gets called as often as possible after request/responses are received from the game gathering
     * observation state.
     *
     * @param agent The agent that this component belongs to
     */
    default void onStep(S2Agent agent) {

    }

    /**
     * Called when an enemy unit enters vision from out of fog of war.
     *
     * @param agent The agent that this component belongs to
     * @param unitInPool The unit that entered vision
     */
    default void onUnitEnterVision(S2Agent agent, UnitInPool unitInPool) {

    }

    /**
     * Called when a Unit has been created by the player.
     *
     * @param agent The agent that this component belongs to
     * @param unitInPool The unit that was created
     */
    default void onUnitCreated(S2Agent agent, UnitInPool unitInPool) {

    }

    /**
     * Called whenever one of the player's units has been destroyed.
     *
     * @param agent The agent that this component belongs to
     * @param unitInPool The unit that was destroyed
     */
    default void onUnitDestroyed(S2Agent agent, UnitInPool unitInPool) {

    }

    /**
     * Called when a unit becomes idle, this will only occur as an event so will only be called when the unit becomes
     * idle and not a second time. Being idle is defined by having orders in the previous step and not currently having
     * orders or if it did not exist in the previous step and now does, a unit being created, for instance, will call
     * both onUnitCreated and onUnitIdle if it does not have a rally set.
     *
     * @param agent The agent that this component belongs to
     * @param unitInPool The unit that is idle
     */
    default void onUnitIdle(S2Agent agent, UnitInPool unitInPool) {

    }

    /**
     * Called when the unit in the previous step had a build progress less than 1.0 but is greater than or equal to 1.0
     * in the current step.
     *
     * @param agent The agent that this component belongs to
     * @param unitInPool The unit that has finished building
     */
    default void onBuildingConstructionComplete(S2Agent agent, UnitInPool unitInPool) {

    }

    /**
     * Called when an upgrade is finished, warp gate, ground weapons, baneling speed, etc.
     *
     * @param agent The agent that this component belongs to
     * @param upgrade The upgrade that has completed
     */
    default void onUpgradeCompleted(S2Agent agent, Upgrade upgrade) {

    }

    /**
     * Called when a game has ended.
     *
     * @param agent The agent that this component belongs to
     */
    default void onGameEnd(S2Agent agent) {

    }

    /**
     * Called when a nydus is placed.
     *
     * @param agent The agent that this component belongs to
     */
    default void onNydusDetected(S2Agent agent) {

    }

    /**
     * Called when a nuclear launch is detected.
     *
     * @param agent The agent that this component belongs to
     */
    default void onNuclearLaunchDetected(S2Agent agent) {

    }

    /**
     * Called for various errors the library can encounter. See ClientError enum for possible errors.
     *
     * @param agent The agent that this component belongs to
     * @param clientErrors What Client errors are being reported
     * @param protocolErrors What protocol errors are being reported
     */
    default void onError(S2Agent agent, List<ClientError> clientErrors, List<String> protocolErrors) {

    }

    /**
     * Called on alert. WARNING: for NYDUS_DETECTED and NUCLEAR_LAUNCH_DETECTED alert there are defined separate client
     * events. Think of these like the announcer voice that plays in a normal game, it's basically just as
     * useless/useful
     *
     * @param agent The agent that this component belongs to
     * @param alert The Alert being called
     */
    default void onAlert(S2Agent agent, Alert alert) {

    }
}
