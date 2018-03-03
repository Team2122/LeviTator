package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Lift;

public class LiftHomingSequence extends Command implements Configurable<LiftHomingSequence.Config> {

    private TatorRobot robot;
    private Lift lift;
    private Config config;

    public LiftHomingSequence(TatorRobot robot) {
        super("LiftHomingSequence");
        this.robot = robot;
        lift = (robot.getSubsystems()).getLift();
        requires(lift);
    }

    @Override
    protected boolean step() {
        lift.setDesiredAnglePreset(Lift.AnglePreset.CENTER);

        if(lift.isPivotLocked() == true) {
            if (lift.isAtBottomLimit() == false) {
                lift.setLiftPower(config.homingPower);
                return false;
            } else {
                lift.setLiftPower(0);
                lift.resetHeightEncoder();
                return true;
            }
        }
        return false;
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public double homingPower;
    }
}
