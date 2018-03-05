package org.teamtators.common.commands;

import org.teamtators.common.TatorRobotBase;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.scheduler.Command;

/**
 * A command which does nothing for a specified amount of time
 */
public class WaitCommand extends Command implements Configurable<WaitCommand.Config> {
    private Config config;
    private Timer timer;

    public WaitCommand(TatorRobotBase robot) {
        super("WaitCommand");
        timer = new Timer();
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    @Override
    protected void initialize() {
        logger.info("Waiting for a period of {} seconds", config.period);
        timer.start();
    }

    @Override
    public boolean step() {
        return timer.hasPeriodElapsed(config.period);
    }

    public static class Config {
        public double period;
    }
}
