package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Lift;

public class LiftHeightPreset extends Command implements Configurable<LiftHeightPreset.Config> {
    private Config config;
    private Lift lift;

    public LiftHeightPreset(TatorRobot robot) {
        super("LiftHeightPreset");
        lift = robot.getSubsystems().getLift();
    }

    @Override
    protected void initialize() {
        lift.setDesiredHeightPreset(config.preset);
    }

    @Override
    public boolean step() {
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
        public Lift.HeightPreset preset;
    }
}
