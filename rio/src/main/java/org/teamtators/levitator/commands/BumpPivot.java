package org.teamtators.levitator.commands;

import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Lift;

public class BumpPivot extends Command {

    private Lift lift;
    private double bumpAngle;

    public BumpPivot(TatorRobot robot, boolean pivotRight, double bumpAngle) {
        super("bumpPivot");
        lift = robot.getSubsystems().getLift();
        requires(lift);
    }

    @Override
    protected boolean step() {
        lift.setDesiredPivotAngle(lift.getCurrentPivotAngle() + bumpAngle);
        return true;
    }

}
