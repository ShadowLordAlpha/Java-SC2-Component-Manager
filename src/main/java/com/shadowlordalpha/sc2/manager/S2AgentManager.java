package com.shadowlordalpha.sc2.manager;

import com.github.ocraft.s2client.bot.ClientError;
import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Upgrade;
import com.github.ocraft.s2client.protocol.observation.Alert;
import lombok.Getter;
import lombok.Setter;
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
     * The Component Set is used to store a set of components that need to run with each game method. This can be
     * controlled using the different component methods allowing dynamic adding and removal of components as well as
     * searching though the set for specific types of components.
     */
    private final Set<Component> componentSet = new HashSet<>();

    /**
     * The Component Set is used in a highly multithreaded style environment, due to the way it is used though this
     * could lead to the set getting modified at times when it would cause concurrent modification exceptions. This lock
     * is to prevent modifications when the set is in use but allow reading of the set to all happen at about the same
     * time as it is only writes/removals that will cause this issue.
     */
    private final ReadWriteLock componentLock = new ReentrantReadWriteLock(true);

    /**
     * The ExecutorService provides us the ability to run a number of components at the same time and watch the returned
     * futures for when each one has finished. Once all are finished we are able to continue and finish the method. This
     * allows us to run all components at the same time while still needing all components to finish before we are done
     * with a specific step.
     *
     * If different behavior is needed for any reason the ExecutorService may be replaced with a custom one and it will
     * function according to that ExecutorService's rules.
     */
    @Getter @Setter private ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * Add a Component to the active set of components and run the initialized method of the added Component.
     *
     * @param component The Component to be added
     */
    public void addComponent(Component component) {
        componentLock.writeLock().lock();
        componentSet.add(component);
        componentLock.writeLock().unlock();

        component.onInitialized(this);

        // Now the new component needs to know all the currnet ones
        componentLock.readLock().lock();
        for(Component send: componentSet) {
            component.onComponentAdded(this, send);
        }
        componentLock.readLock().unlock();

        // FIXME:  This needs to be run in a thread, otherwise it seems to break everything similar to our workforce
        //  manager... not actually sure why
        executorService.submit(() -> onComponentAdded(component));
    }

    /**
     * Find all Components and return a set of the Components that are an instance of the given class.
     *
     * @param clazz The class of the Component we are looking for
     * @param <E> The type we are looking for
     * @return A set of components that are an instance of the given class
     */
    public <E extends Component> Set<E> findComponent(Class<E> clazz) {

        componentLock.writeLock().lock();
        Set<E> tempSet = new HashSet<>();
        for(Component component: componentSet) {
            if(clazz.isInstance(component)) {
                tempSet.add((E) component);
            }
        }
        componentLock.writeLock().unlock();

        return tempSet;
    }

    /**
     * Remove a specific component from the component set.
     *
     * @param component The component to be removed
     */
    public void removeComponent(Component component) {
        componentLock.writeLock().lock();
        componentSet.remove(component);
        componentLock.writeLock().unlock();

        // FIXME: See add for comment here...
        executorService.submit(() -> onComponentRemoved(component));
    }

    /**
     * Clear the Component Set of all Components.
     */
    public void clearComponentSet() {

        // As tihs removes all components we don't need to update any of them, just tell them that they were removed
        componentLock.readLock().lock();
        for(Component send: componentSet) {
            send.onComponentRemoved(this, send);
        }
        componentLock.readLock().unlock();

        componentLock.writeLock().lock();
        componentSet.clear();
        componentLock.writeLock().unlock();

    }

    /**
     * Allows the components to run in parallel. As the code to do this is nearly identical for all methods with just a
     * slight change for what method is executed a method that takes in what we need to do using a Function to do so
     * for the list of each component was determined to be the best method without duplicating code.
     *
     * @param execute The Function that allows us to do what we need to with a component and returns the method to run
     */
    public void runComponentParallel(Function<Component, Runnable> execute) {
        try {
            Set<Future<?>> futureSet = new HashSet<>();
            componentLock.readLock().lock();
            for (Component component : componentSet) {
                futureSet.add(executorService.submit(execute.apply(component)));
            }
            componentLock.readLock().unlock();

            for (Future<?> future : futureSet) {
                try {
                    future.get(30, TimeUnit.SECONDS);
                } catch (CancellationException e) {
                    log.warn("Future Cancelled", e);
                } catch (ExecutionException e) {
                    log.warn("Future Exception Found", e.getCause());
                } catch (TimeoutException e) {
                    log.warn("Timeout Exception", e);
                }
            }
        } catch (InterruptedException e) {
            log.warn("Thread Interrupted", e);
        }
    }

    /**
     * Method called when we add a component to our list so all our components can update as needed
     *
     * @param added
     */
    public void onComponentAdded(Component added) {
        runComponentParallel((component) -> () -> component.onComponentAdded(this, added));
    }

    /**
     * Method called when we remove a component so all our components can update as needed
     * @param removed
     */
    public void onComponentRemoved(Component removed) {
        runComponentParallel((component) -> () -> component.onComponentRemoved(this, removed));
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
