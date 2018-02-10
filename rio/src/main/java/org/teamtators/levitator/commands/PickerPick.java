package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Picker;
import org.teamtators.levitator.subsystems.Subsystems;

public class PickerPick extends Command implements Configurable<PickerPick.Config> {
    Picker picker;
    Config config;
    Timer timer = new Timer();

    boolean jammed = false;
    boolean unjamLeft = true;

    public PickerPick(TatorRobot robot) {
        super("PickerPick");
        picker = ((Subsystems)robot.getSubsystemsBase()).getPicker();
        requires(picker);
    }

    @Override
    protected void initialize() {
        picker.setPickerExtended(true);
        super.initialize();
        timer.start();
    }

    @Override
    protected boolean step() {
        if(!jammed) {
            if (!picker.isCubeDetected() && !picker.isCubeDetectedRight() && !picker.isCubeDetectedLeft()) {
                picker.setRollerPowers(config.leftRollerPower, config.rightRollerPower);
            } else if (picker.isCubeDetected() && (!picker.isCubeDetectedLeft() || !picker.isCubeDetectedRight())) {
                if (timer.hasPeriodElapsed(config.waitPeriod)) {
                    jammed = true;
                    timer.reset();
                }
            }
        } else {
            timer.start();
            if(unjamLeft) {
                picker.setRollerPowers(config.unjamLeftRollerPower, 0.0);
            } else {
                picker.setRollerPowers(0.0, config.unjamRightRollerPower);
            }
            if(timer.hasPeriodElapsed(config.unjamPeriod)) {
                unjamLeft = !unjamLeft;
                timer.restart();
            }
        }
        return picker.isCubeDetected() && picker.isCubeDetectedLeft() && picker.isCubeDetectedRight();
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted);
        jammed = true;
        unjamLeft = true;
        timer.reset();
        picker.setRollerPowers(0.0,0.0);
        picker.setPickerExtended(false);
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public double leftRollerPower;
        public double rightRollerPower;
        public double unjamLeftRollerPower;
        public double unjamRightRollerPower;
        public double waitPeriod;
        public double unjamPeriod;
    }
}
