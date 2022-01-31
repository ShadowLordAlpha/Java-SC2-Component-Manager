package com.shadowlordalpha.sc2.manager;

import com.github.ocraft.s2client.bot.ClientError;
import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Upgrade;
import com.github.ocraft.s2client.protocol.observation.Alert;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * S2AgentManager extends S2Agent and provides management for components added to the running bot with the addComponent
 * method. These components allow everything to be moved out of the bot class and into its own sections though any
 * communication between the different components must be handled by the components though you can get a component you
 * are looking for though the manager to help facilitate this.
 *
 * NOTE: Components can add more components are they are given a reference to their containing agent to allow access to
 * everything it could need instead of just passing references to observation, query, debug, or any of those methods
 * within the agent.
 *
 * NOTE: components generally run a thread SEPARATE from the thread running the agent, this allows them all to execute
 * at the same time but is something that must be kept in mind when talking between components.
 *
 * @author Josh "ShadowLordAlpha" - 01-30-2022
 * @since 1.0.0
 * @version 1.0.0
 */
@Slf4j
public abstract class S2AgentManager extends S2Agent {

    /**
     * As we don't care about the order of our components we use a set to store them
     */
    private final Set<Component> componentSet = new HashSet<>();

    /**
     * We need to be able to work on the component set and we don't really want threads or other things
     */
    private final ReadWriteLock componentLock = new ReentrantReadWriteLock(true);

    /**
     *
     */
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     *
     * @param component
     */
    public void addComponent(Component component) {
        componentLock.writeLock().lock();
        componentSet.add(component);
        componentLock.writeLock().unlock();

        component.onInitialized(this);
    }

    /**
     *
     * @param clazz
     * @param <E>
     * @return
     */
    public <E extends Component> Set<E> findComponent(Class<E> clazz) {

        Set<E> tempSet = new HashSet<>();
        for(Component component: componentSet) {
            if(clazz.isInstance(component)) {
                tempSet.add((E) component);
            }
        }

        return tempSet;
    }

    /**
     *
     * @param component
     */
    public void removeManager(Component component) {
        componentLock.writeLock().lock();
        componentSet.remove(component);
        componentLock.writeLock().unlock();
    }

    /**
     *
     */
    public void clearComponentSet() {
        componentLock.writeLock().lock();
        componentSet.clear();
        componentLock.writeLock().unlock();
    }

    /**
     *
     * @param execute
     */
    private void runComponentParallel(Function<Component, Runnable> execute) {
        try {
            Set<Future<?>> futureSet = new HashSet<>();
            componentLock.readLock().lock();
            for (Component component : componentSet) {
                futureSet.add(executorService.submit(execute.apply(component)));
            }
            componentLock.readLock().unlock();

            for (Future<?> future : futureSet) {
                try {
                    future.get();
                } catch (ExecutionException | CancellationException e) {
                    log.warn("Future Exception", e);
                }
            }
        } catch (InterruptedException e) {
            log.warn("Thread Interrupted", e);
        }
    }

    @Override
    public void onGameFullStart() {
        runComponentParallel((component) -> () -> component.onGameFullStart(this));
    }

    @Override
    public void onGameStart() {
        runComponentParallel((component) -> () -> component.onGameStart(this));
    }

    @Override
    public void onGameEnd() {
        runComponentParallel((component) -> () -> component.onGameEnd(this));
    }

    @Override
    public void onStep() {
        runComponentParallel((component) -> () -> component.onStep(this));
    }

    @Override
    public void onUnitDestroyed(UnitInPool unitInPool) {
        runComponentParallel((component) -> () -> component.onUnitDestroyed(this, unitInPool));
    }

    @Override
    public void onUnitCreated(UnitInPool unitInPool) {
        runComponentParallel((component) -> () -> component.onUnitCreated(this, unitInPool));
    }

    @Override
    public void onUnitIdle(UnitInPool unitInPool) {
        runComponentParallel((component) -> () -> component.onUnitIdle(this, unitInPool));
    }

    @Override
    public void onUpgradeCompleted(Upgrade upgrade) {
        runComponentParallel((component) -> () -> component.onUpgradeCompleted(this, upgrade));
    }

    @Override
    public void onBuildingConstructionComplete(UnitInPool unitInPool) {
        runComponentParallel((component) -> () -> component.onBuildingConstructionComplete(this, unitInPool));
    }

    @Override
    public void onNydusDetected() {
        runComponentParallel((component) -> () -> component.onNydusDetected(this));
    }

    @Override
    public void onNuclearLaunchDetected() {
        runComponentParallel((component) -> () -> component.onNuclearLaunchDetected(this));
    }

    @Override
    public void onUnitEnterVision(UnitInPool unitInPool) {
        runComponentParallel((component) -> () -> component.onUnitEnterVision(this, unitInPool));
    }

    @Override
    public void onError(List<ClientError> clientErrors, List<String> protocolErrors) {
        runComponentParallel((component) -> () -> component.onError(this, clientErrors, protocolErrors));
    }

    @Override
    public void onAlert(Alert alert) {
        runComponentParallel((component) -> () -> component.onAlert(this, alert));
    }
}
