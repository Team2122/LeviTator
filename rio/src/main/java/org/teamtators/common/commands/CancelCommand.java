package org.teamtators.common.commands;

import org.teamtators.common.TatorRobotBase;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.scheduler.CommandStore;

/**
 * Cancels another command
 */
public class CancelCommand extends Command implements Configurable<CancelCommand.Config> {
    private CommandStore commandStore;
    private Command command;

    public CancelCommand(TatorRobotBase robot) {
        super("CancelCommand");
        commandStore = robot.getCommandStore();
    }

    @Override
    public boolean step() {
        if (command.isRunning()) {
            command.cancel();
        } else {
            logger.debug(command.getName() + " not running, can't cancel it");
        }
        return true;
    }

    @Override
    public void configure(Config config) {
        command = commandStore.getCommand(config.command);
    }

    public static class Config {
        public String command;
    }
}
