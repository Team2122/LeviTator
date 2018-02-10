package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Picker;

public class PickerRelease extends Command implements Configurable<PickerRelease.Config> {
    private Picker picker;
    private Config config;

    public PickerRelease(TatorRobot robot) {
        super("PickerRelease");
        picker = robot.getSubsystems().getPicker();
        requires(picker);
    }

    @Override
    protected void initialize() {
        super.initialize();
        picker.setPickerExtended(true);
    }

    @Override
    protected boolean step() {
        picker.setRollerPowers(config.powers);
        return false;
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted);
        picker.stopRollers();
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public Picker.RollerPowers powers;
    }
}
