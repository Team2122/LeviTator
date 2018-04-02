package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Lift;

public class WaitForHeight extends Command implements Configurable<WaitForHeight.Config> {
    private Lift lift;
    private Config config;

    WaitForHeight(TatorRobot robot) {
        super("WaitForCenter");
        this.lift = robot.getSubsystems().getLift();
    }

    @Override
    protected void initialize() {
        logger.info("Waiting for height to be on target");
    }

    @Override
    public boolean step() {
        return lift.getCurrentHeight() + config.tolerance > lift.getDesiredHeight() && lift.getCurrentHeight() - config.tolerance < lift.getDesiredHeight();
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    @SuppressWarnings("WeakerAccess")
    public static class Config {
        public double tolerance;
    }
}
