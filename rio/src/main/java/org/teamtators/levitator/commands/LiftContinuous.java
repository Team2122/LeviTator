package org.teamtators.levitator.commands;

import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Subsystems;
import org.teamtators.levitator.subsystems.Lift;

public class LiftContinuous extends Command{

    private TatorRobot robot;
    private Lift lift;
    private double currentHeight;
    private double desiredHeight;
    private double currentPivotAngle;
    private double desiredPivotAngle;

    public LiftContinuous(TatorRobot robot) {
        super("LiftContinuous");
        this.robot = robot;
        lift = (robot.getSubsystems()).getLift();
        // oi = ((Subsystems) robot.getSubsystemsBase()).getOI();
        requires(lift);
    }

    @Override
    protected boolean step() {
        currentHeight = lift.getCurrentHeight();
        desiredHeight = lift.getDesiredHeight();
        currentPivotAngle = lift.getCurrentPivotAngle();
        desiredPivotAngle = lift.getDesiredPivotAngle();

        if(currentHeight != desiredHeight) {
            //set the target height here
        }
        if(currentPivotAngle != desiredPivotAngle) {
            //set the target angle here
        }

        return false;
    }

}
