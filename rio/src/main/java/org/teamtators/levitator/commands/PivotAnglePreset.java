package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Pivot;

public class PivotAnglePreset extends Command implements Configurable<PivotAnglePreset.Config> {
    private Config config;
    private Pivot pivot;

    public PivotAnglePreset(TatorRobot robot) {
        super("PivotAnglePreset");
        pivot = robot.getSubsystems().getPivot();
    }

    @Override
    protected void initialize() {
        pivot.setDesiredAnglePreset(config.preset);
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
        public Pivot.AnglePreset preset;
    }
}
