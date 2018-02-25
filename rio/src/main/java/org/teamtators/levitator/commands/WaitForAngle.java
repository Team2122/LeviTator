package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Lift;

public class WaitForAngle extends Command implements Configurable<WaitForAngle.Config> {
    private Lift lift;
    private Config config;

    public WaitForAngle(TatorRobot robot) {
        super("WaitForCenter");
        this.lift = robot.getSubsystems().getLift();
    }

    @Override
    protected void initialize() {
        logger.info("Waiting for center");
    }

    @Override
    protected boolean step() {
        double desiredAngle = lift.getDesiredPivotAngle();
        if (config.preset != null) {
            desiredAngle = lift.getAnglePreset(config.preset);
        }
        return Math.abs(lift.getCurrentPivotAngle() - desiredAngle) <= config.tolerance;
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public double tolerance;
        public Lift.AnglePreset preset;
    }
}
