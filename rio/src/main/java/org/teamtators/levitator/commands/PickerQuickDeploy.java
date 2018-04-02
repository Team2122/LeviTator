package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Picker;

public class PickerQuickDeploy extends Command implements Configurable<PickerQuickDeploy.Config> {
    private Picker picker;
    private Timer timer = new Timer();
    private Config config;

    PickerQuickDeploy(TatorRobot robot) {
        super("PickerQuickDeploy");
        this.picker = robot.getSubsystems().getPicker();
        requires(picker);
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    @Override
    public void initialize() {
        super.initialize();
        timer.start();
    }

    @Override
    public boolean step() {
        boolean canStart = timer.hasPeriodElapsed(config.timeBeforeKick);
        boolean startRetracting = timer.hasPeriodElapsed(config.timeBeforeRetract);
        boolean endKick = timer.hasPeriodElapsed(config.timeToKick);
        if (canStart && !endKick) {
            picker.setRollerPower(-config.kickPower);
        } else {
            picker.setRollerPower(0.0);
        }
        if (startRetracting) {
            if (!Double.isNaN(config.timeBeforeRetract)) {
                picker.setPickerExtended(false);
            }
        } else {
            picker.setPickerExtended(true);
        }
        return endKick;
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted);
        picker.setRollerPower(0.0);
        if (!Double.isNaN(config.timeBeforeRetract)) {
            picker.setPickerExtended(false);
        }
        picker.unlockArms();
    }

    @SuppressWarnings("WeakerAccess")
    public static class Config {
        public double kickPower;
        public double timeBeforeKick;
        public double timeToKick;
        public double timeBeforeRetract;
    }
}
