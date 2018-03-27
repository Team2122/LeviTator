package org.teamtators.common.scheduler;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class SequentialCommand extends Command implements CommandRunContext {
    private List<SequentialCommandRun> sequence;
    private int currentPosition;

    public SequentialCommand(String name, Collection<Command> sequence) {
        super(name);
        setSequence(sequence);
    }

    public SequentialCommand(String name, Command... sequence) {
        this(name, Arrays.asList(sequence));
    }

    public SequentialCommand(Collection<Command> sequence) {
        this("SequentialCommand", sequence);
    }

    public SequentialCommand(Command... sequence) {
        this("SequentialCommand", Arrays.asList(sequence));
    }

    private SequentialCommandRun currentRun() {
        if (currentPosition >= sequence.size()) return null;
        return sequence.get(currentPosition);
    }

    public Command currentCommand() {
        SequentialCommandRun currentRun = currentRun();
        if (currentRun == null)
            return null;
        return currentRun.command;
    }

    public int sequenceLength() {
        return sequence.size();
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    protected void setRunSequence(List<SequentialCommandRun> sequence) {
        checkNotNull(sequence);

        this.sequence = sequence;

        updateValidStates();
        updateRequirements();
    }

    protected void setSequence(Collection<Command> commands) {
        checkNotNull(commands);
        setRunSequence(commands.stream()
                .map(SequentialCommandRun::new)
                .collect(Collectors.toList()));
    }

    private void updateValidStates() {
        // The valid states of a sequential command are the intersection of the valid states of the
        // child commands. So if a single command cannot run in Teleop, the whole sequential command
        // can not either.
        EnumSet<RobotState> validStates = EnumSet.allOf(RobotState.class);
        sequence.forEach(run -> {
            if (run.command instanceof SequentialCommand)
                ((SequentialCommand) run.command).updateValidStates();
            validStates.retainAll(run.command.getValidStates());
        });
        setValidStates(validStates);
    }

    @Override
    public void updateRequirements() {
        // A sequential command requires all subsystems required by all child commands
        sequence.forEach(run -> {
            run.command.updateRequirements();
            if (run.command.getRequirements() != null)
                requiresAll(run.command.getRequirements());
        });
    }

    @Override
    protected void initialize() {
        logger.debug("SequentialCommand initializing");
        for (CommandRun run : sequence) {
            run.initialized = false;
            run.cancel = false;
        }
        currentPosition = 0;
    }

    @Override
    public boolean step() {
        if (sequence.size() == 0) return true;
        boolean finished;
        do {
            SequentialCommandRun run = currentRun();
            if (run == null) {
                logger.warn("Sequential command run is null???");
                return true;
            }
            if (run.cancel) {
                cancelRun(run);
                return true;
            }
            if (run.parallel) {
                releaseRequirements(run.command.getRequirements());
                startWithContext(run.command, getRootContext());
                finished = true;
            } else {
                if (!run.initialized) {
                    releaseRequirements(run.command.getRequirements());
                    if (run.command.isRunning()) {
                        if (run.command.getContext() == this && run.command.checkRequirements()) {
                            logger.trace("Command was already running with myself as parent: {}", run.command.getName());
                            run.initialized = true;
                        } else {
                            logger.trace("Command was already running with other parent, cancelling: {}", run.command.getName());
                            run.command.cancel();
                            return false;
                        }
                    } else if (run.command.startRun(this)) {
                        run.initialized = true;
                    } else {
                        logger.trace("Command could not be initialized at this time: {}", run.command.getName());
                    }
                }
                if (run.initialized) {
                    finished = run.command.step();
                } else {
                    finished = false;
                }
            }
            if (run.cancel) {
                cancelRun(run);
                return true;
            }
            if (finished) {
                if (!run.parallel) {
                    run.command.finishRun(false);
                }
                currentPosition++;
                if (currentPosition >= sequence.size()) {
                    logger.trace("Sequential command finished");
                    return true;
                }
                logger.trace("Sequential command advancing to command {}", currentPosition);
            }
        } while (finished);
        return false;
    }

    private void cancelRun(SequentialCommandRun run) {
        if (run.parallel) {
            cancelCommand(run.command);
        } else {
            run.command.finishRun(true);
        }
        cancelCommand(this);
    }

    @Override
    protected void finish(boolean interrupted) {
        if (interrupted) {
            logger.debug("SequentialCommand interrupted");
        } else {
            logger.debug("SequentialCommand finished");
        }
        CommandRunContext rootContext = getRootContext();
        if (rootContext == null) {
            logger.error("rootContext is null??");
        }
        sequence
            .forEach(r -> {
                if (r.command.isRunning() && r.parallel) {
//                    r.command.setContext(rootContext);
                    if (interrupted) {
                        r.command.cancel();
                    }
                }
            });
        if (sequence.size() == 0 || currentPosition >= sequence.size()) return;
        Command currentCommand = currentRun().command;
        if (interrupted && currentCommand.isRunning()) {
            currentCommand.finishRun(true);
        }

    }

    public void cancelCurrentCommand() {
        cancelRun(currentRun());
    }

    @Override
    public void cancelCommand(Command command) {
        checkNotNull(command);
        Optional<SequentialCommandRun> inGroup = findCommand(command);
        if (inGroup.isPresent() && !inGroup.get().parallel) {
            inGroup.get().cancel = true;
        } else {
            super.cancelCommand(command);
        }
    }

    private Optional<SequentialCommandRun> findCommand(Command command) {
        return sequence.stream()
                .filter(run -> run.command == command)
                .findFirst();
    }

    public static class SequentialCommandRun extends CommandRun {
        public boolean parallel = false;

        public SequentialCommandRun(Command command) {
            super(command);
        }
    }
}
