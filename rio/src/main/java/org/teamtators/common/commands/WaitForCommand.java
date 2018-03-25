package org.teamtators.common.commands;

import org.teamtators.common.TatorRobotBase;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.scheduler.CommandStore;

/**
 * Waits for another command
 */
public class WaitForCommand extends Command implements Configurable<WaitForCommand.Config> {
    private Command command;
    private CommandStore commandStore;

    public WaitForCommand(TatorRobotBase robot) {
        super("WaitForCommand");
        commandStore = robot.getCommandStore();
    }

    @Override
    protected void initialize() {
        if (!command.isRunning()) {
            logger.debug("{} is not running", command.getName());
        } else {
            logger.debug("Waiting for {}", command.getName());
        }
    }

    @Override
    public boolean step() {
        return !command.isRunning();
    }

    @Override
    public void configure(Config config) {
        this.command = commandStore.getCommand(config.command);
    }

    public static class Config {
        public String command;
    }
}
