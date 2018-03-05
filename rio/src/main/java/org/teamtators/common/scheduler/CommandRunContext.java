package org.teamtators.common.scheduler;

public interface CommandRunContext {
    void cancelCommand(Command command) throws CommandException;

    default void startCommand(Command command) throws CommandException {
        startWithContext(command, this);
    }

    void startWithContext(Command command, CommandRunContext context) throws CommandException;

    CommandRunContext getContext();

    default CommandRunContext getRootContext() {
        if (getContext() == null) {
            return null;
        }
        if (getContext() == this) {
            return this;
        }
        return getContext().getRootContext();
    }
}
