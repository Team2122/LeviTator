package org.teamtators.common.scheduler;

import java.util.Objects;

class CommandRun {
    Command command;
    boolean initialized = false;
    boolean cancel = false;
    CommandRunContext context = null;

    CommandRun(Command command) {
        this.command = command;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommandRun that = (CommandRun) o;
        return Objects.equals(command, that.command);
    }

    @Override
    public int hashCode() {
        return Objects.hash(command);
    }
}
