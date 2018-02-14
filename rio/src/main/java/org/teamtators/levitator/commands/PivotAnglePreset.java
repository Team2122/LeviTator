package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Lift;

public class PivotAnglePreset extends Command implements Configurable<PivotAnglePreset.Config> {
    private Config config;
    private Lift lift;

    public PivotAnglePreset(TatorRobot robot) {
        super("PivotAnglePreset");
        lift = robot.getSubsystems().getLift();
    }

    @Override
    protected void initialize() {
        double angle = lift.getAnglePreset(config.preset);
        logger.info("Setting desired pivot angle to preset {} ({})", config.preset, angle);
        lift.setDesiredPivotAngle(angle);
    }

    @Override
    protected boolean step() {
        return true;
    }

    @Override
    protected void finish(boolean interrupted) {

    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public Lift.AnglePreset preset;
    }
}
