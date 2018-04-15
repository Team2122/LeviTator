package org.teamtators.common.scheduler;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.teamtators.common.util.FMSData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Scheduler implements CommandRunContext, RobotStateListener, FMSDataListener {
    private static Logger logger = LoggerFactory.getLogger(Scheduler.class);

    private Map<TriggerSource, List<TriggerScheduler>> triggerSchedulers = new HashMap<>();
    private Map<String, CommandRun> runningCommands = new ConcurrentHashMap<>();
    private Set<Command> defaultCommands = new HashSet<>();

    private Set<RobotStateListener> stateListeners = new HashSet<>();
    private Set<FMSDataListener> dataListeners = new HashSet<>();

    private RobotState robotState = RobotState.DISABLED;
    private FMSData fmsData;
    private Profiler profiler;

    public Scheduler() {
    }

    public void registerDefaultCommand(Command defaultCommand) {
        defaultCommands.add(defaultCommand);
    }

    public void registerDefaultCommands(Collection<Command> defaultCommands) {
        this.defaultCommands.addAll(defaultCommands);
    }

    public void clearDefaultCommands() {
        defaultCommands.clear();
    }

    /**
     * Register a StateListener to be updated on RobotState change
     *
     * @param subsystem Subsystem to add to the set
     */
    public void registerStateListener(RobotStateListener subsystem) {
        Preconditions.checkNotNull(subsystem);
        stateListeners.add(subsystem);
    }

    public void unregisterStateListener(RobotStateListener subsystem) {
        stateListeners.remove(subsystem);
    }

    public void clearStateListeners() {
        stateListeners.clear();
    }

    public void registerFMSDataListener(FMSDataListener listener) {
        Preconditions.checkNotNull(listener);
        dataListeners.add(listener);
    }

    public void unregisterFMSDataListener(FMSDataListener listener) {
        dataListeners.remove(listener);
    }

    public void clearFMSDataListeners() {
        dataListeners.clear();
    }

    public void addTrigger(TriggerSource source, TriggerScheduler scheduler) {
        List<TriggerScheduler> schedulers = triggerSchedulers.get(source);
        if (schedulers == null) {
            triggerSchedulers.put(source, new ArrayList<>(Collections.singletonList(scheduler)));
        } else {
            schedulers.add(scheduler);
        }
    }

    public TriggerAdder onTrigger(TriggerSource triggerSource) {
        return new TriggerAdder(this, triggerSource);
    }

    public void clearTriggers() {
        triggerSchedulers.clear();
    }

    public void execute() {
//        logger.trace("Scheduler in state {}, {} triggers, {} commands", robotState, triggerSchedulers.size(),
//                runningCommands.size());
        profiler.start("triggers");
        for (Map.Entry<TriggerSource, List<TriggerScheduler>> entry : triggerSchedulers.entrySet()) {
            TriggerSource triggerSource = entry.getKey();
            boolean active = triggerSource.getActive();
            for (TriggerScheduler scheduler : entry.getValue()) {
                scheduler.processTrigger(active);
            }
        }
        for (CommandRun run : runningCommands.values()) {
            profiler.start(run.command.getName());
            if (run.cancel) {
//                logger.trace("Cancelling command {} by request", run.command.getName());
                finishRun(run, true);
                continue;
            } else if (!run.command.isValidInState(robotState)) {
//                logger.trace("Cancelling command {} because of state conflict in {}", run.command.getName(),
//                        robotState);
                finishRun(run, true);
                continue;
            } else if (!run.initialized) {
                if (!run.command.startRun(run.context)) {
//                    logger.trace("Command {} not ready to run yet because of requirements", run.command.getName());
                    continue;
                } else {
//                    logger.trace("Initialized command {}", run.command.getName());
                }
                run.initialized = true;
            }
            boolean finished = run.command.step();
            if (finished || run.cancel) {
//                logger.trace("Command {} finished, it was cancelled?: {}", run.command.getName(), run.cancel);
                finishRun(run, run.cancel);
            }
        }
        profiler.start("defaultCommands");
        for (Command command : defaultCommands) {
            if (command.checkRequirements()
                    && command.isValidInState(robotState)
                    && !command.isRunning()) {
                startCommand(command);
            }
        }
        profiler.stop();
    }

    private void finishRun(CommandRun run, boolean cancelled) {
        run.command.finishRun(cancelled);
        runningCommands.remove(run.command.getName());
    }

    /**
     * Check if the current command is running or waiting to run
     *
     * @param name Name of the command to check
     * @return Whether the named command is running or queued
     */
    public boolean containsCommand(String name) {
        return runningCommands.containsKey(name);
    }

    @Override
    public void startCommand(Command command) {
        startWithContext(command, this);
    }

    @Override
    public CommandRunContext getContext() {
        return this;
    }

    @Override
    public void startWithContext(Command command, CommandRunContext context) throws CommandException {
        checkNotNull(command);
        CommandRun run = runningCommands.get(command.getName());
        if (run != null || !command.isValidInState(robotState))
            return;
        if (command.getContext() != null) {
            command.cancel();
        }
        CommandRun commandRun = new CommandRun(command);
        commandRun.context = context;
        runningCommands.put(command.getName(), commandRun);
    }

    public void cancelCommand(String commandName) {
        checkNotNull(commandName);
        CommandRun run = runningCommands.get(commandName);
        if (run == null)
            logger.debug("Attempted to cancel not command that was not running: {}", commandName);
        else
            run.cancel = true;
    }

    @Override
    public void cancelCommand(Command command) {
        checkNotNull(command);
        cancelCommand(command.getName());
    }

    public RobotState getRobotState() {
        return robotState;
    }

    @Override
    public void onEnterRobotState(RobotState currentState) {
        this.robotState = currentState;
        for (RobotStateListener listener : stateListeners) {
            listener.onEnterRobotState(currentState);
        }
    }

    @Override
    public void onFMSData(FMSData data) {
        this.fmsData = data;
        for (FMSDataListener listener : dataListeners) {
            listener.onFMSData(data);
        }
    }

    public void setProfiler(Profiler profiler) {
        this.profiler = profiler;
    }

    public Profiler getProfiler() {
        return profiler;
    }
}
