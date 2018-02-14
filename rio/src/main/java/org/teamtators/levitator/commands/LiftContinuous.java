package org.teamtators.levitator.commands;

import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Subsystems;
import org.teamtators.levitator.subsystems.Lift;

public class LiftContinuous extends Command{

    private TatorRobot robot;
    private Lift lift;
    private double desiredHeight;
    private double desiredPivotAngle;

    public LiftContinuous(TatorRobot robot) {
        super("LiftContinuous");
        this.robot = robot;
        lift = (robot.getSubsystems()).getLift();
        requires(lift);
    }

    @Override
    protected boolean step() {
        desiredHeight = lift.getDesiredHeight();
        desiredPivotAngle = lift.getDesiredPivotAngle();

        lift.setTargetHeight(desiredHeight);
        lift.setTargetAngle(desiredPivotAngle);

        return false;
    }

}
