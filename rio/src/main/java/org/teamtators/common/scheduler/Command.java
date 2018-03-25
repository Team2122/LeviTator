package org.teamtators.common.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class Command implements CommandRunContext {
    protected Logger logger;
    private String name;
    private CommandRunContext context = null;
    private Set<Subsystem> requirements = null;
    private EnumSet<RobotState> validStates = EnumSet.of(RobotState.AUTONOMOUS, RobotState.TELEOP);

    public Command(String name) {
        checkNotNull(name);
        setName(name);
    }

    protected void initialize() {
        logger.debug("{} initializing", getName());
    }

    public abstract boolean step();

    protected void finish(boolean interrupted) {
        if (interrupted) {
            logger.debug("{} interrupted", getName());
        } else {
            logger.debug("{} ended", getName());
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        String loggerName = String.format("%s.(%s)", this.getClass().getName(), name);
        this.logger = LoggerFactory.getLogger(loggerName);
    }

    public CommandRunContext getContext() {
        return context;
    }

    void setContext(CommandRunContext context) {
        this.context = context;
    }

    public boolean isRunning() {
        return this.context != null;
    }

    @Override
    public void startWithContext(Command command, CommandRunContext context) {
        if (this.context == null) {
            logger.error("Tried add command {} in parent context while not running", command.getName());
        } else
            this.context.startWithContext(command, context);
    }

    @Override
    public void cancelCommand(Command command) {
        if (this.context == null) {
            logger.debug("Tried to cancel command WHILE not running: {}", command.getName());
        } else if (command.getContext() == null) {
            logger.debug("Tried to cancel command that is not running: {}", command.getName());
        } else
            this.context.cancelCommand(command);
    }

    public void cancel() {
        this.cancelCommand(this);
    }

    protected void requires(Subsystem subsystem) {
        checkNotNull(subsystem, "Cannot require a null subsystem");
        if (requirements == null) {
            requirements = new HashSet<>();
        }
        requirements.add(subsystem);
    }

    protected void requiresAll(Collection<Subsystem> subsystems) {
        checkNotNull(subsystems);
        if (requirements == null) {
            requirements = new HashSet<>();
        }
        requirements.addAll(subsystems);
    }

    public Set<Subsystem> getRequirements() {
        return requirements;
    }

    protected void setRequirements(Set<Subsystem> requirements) {
        this.requirements = requirements;
    }

    public boolean doesRequire(Subsystem subsystem) {
        return requirements != null && requirements.contains(subsystem);
    }

    public void updateRequirements() {

    }

    private boolean isRequiring(Subsystem subsystem, CommandRunContext context) {
        Command requiringCommand = subsystem.getRequiringCommand();
        return requiringCommand == this ||
                context instanceof Command &&
                        ((Command) context).isRequiring(subsystem);
    }

    public boolean isRequiring(Subsystem subsystem) {
        return isRequiring(subsystem, getContext());
    }

    protected boolean checkRequirements(Iterable<Subsystem> requirements) {
        if (requirements == null)
            return true;
        for (Subsystem subsystem : requirements) {
            Command requiringCommand = subsystem.getRequiringCommand();
            if (requiringCommand == null || isRequiring(subsystem))
                continue;
            return false;
        }
        return true;
    }

    public boolean checkRequirements() {
        return checkRequirements(getRequirements());
    }

    private boolean takeRequirements(Iterable<Subsystem> requirements, CommandRunContext context) {
        if (requirements == null) return true;
        boolean anyRequiring = false;
        for (Subsystem subsystem : requirements) {
            Command requiringCommand = subsystem.getRequiringCommand();
            if (requiringCommand == null || !requiringCommand.isRunning()) {
                subsystem.setRequiringCommand(this);
                continue;
            }
//            logger.trace("Potential requiring command for subsystem {}: {}", subsystem.getName(), requiringCommand.getName());
            if (isRequiring(subsystem, context))
                continue;
            logger.trace("Command needs subsystem requirement to start {}: {}", subsystem.getName(), requiringCommand.getName());
            anyRequiring = true;
            requiringCommand.cancel();
        }
        return !anyRequiring;
    }

    protected boolean cancelRequiring(Subsystem... requirements) {
        return takeRequirements(Arrays.asList(requirements), null);
    }

    private boolean takeRequirements(CommandRunContext context) {
        return takeRequirements(this.requirements, context);
    }

    public boolean startRun(CommandRunContext context) {
        if (isRunning() || !takeRequirements(context))
            return false;
        setContext(context);
        initialize();
        return true;
    }

    public void finishRun(boolean cancelled) {
        if (isRunning()) {
            finish(cancelled);
            setContext(null);
        }
        releaseRequirements();
    }

    protected void releaseRequirements(Set<Subsystem> requirements) {
        if (requirements == null)
            return;
        for (Subsystem subsystem : requirements) {
            if (subsystem.getRequiringCommand() == this)
                subsystem.setRequiringCommand(null);
        }
    }

    void releaseRequirements() {
        releaseRequirements(this.requirements);
    }

    public boolean isValidInState(RobotState state) {
        return validStates.contains(state);
    }

    protected void validIn(RobotState... states) {
        setValidStates(EnumSet.copyOf(Arrays.asList(states)));
    }

    public EnumSet<RobotState> getValidStates() {
        return validStates;
    }

    protected void setValidStates(EnumSet<RobotState> validStates) {
        this.validStates = validStates;
    }
}
