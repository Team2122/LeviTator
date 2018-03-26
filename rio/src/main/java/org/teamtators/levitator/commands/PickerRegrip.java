package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Picker;

public class PickerRegrip extends Command implements Configurable<PickerRegrip.Config> {
    private Picker picker;
    private Config config;

    public PickerRegrip(TatorRobot robot) {
        super("PickerRegrip");
        picker = robot.getSubsystems().getPicker();
        requires(picker);
    }

    @Override
    protected void initialize() {
        super.initialize();
    }

    @Override
    public boolean step() {
        picker.setRollerPower(config.gripPower);
        return picker.isCubeInPicker();
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted);
        picker.setRollerPower(config.holdPower);
        picker.lockArms();
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public double gripPower;
        public double holdPower;
    }
}
