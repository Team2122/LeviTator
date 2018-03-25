package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Pivot;

public class WaitForAngle extends Command implements Configurable<WaitForAngle.Config> {
    private Pivot pivot;
    private Config config;

    public WaitForAngle(TatorRobot robot) {
        super("WaitForCenter");
        this.pivot = robot.getSubsystems().getPivot();
    }

    @Override
    protected void initialize() {
        logger.info("Waiting for angle: {} with tolerance {}", config.preset, config.tolerance);
    }

    @Override
    public boolean step() {
        double desiredAngle = pivot.getDesiredPivotAngle();
        if (config.preset != null) {
            desiredAngle = pivot.getAnglePreset(config.preset);
        }
        return Math.abs(pivot.getCurrentPivotAngle() - desiredAngle) <= config.tolerance;
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public double tolerance;
        public Pivot.AnglePreset preset;
    }
}
