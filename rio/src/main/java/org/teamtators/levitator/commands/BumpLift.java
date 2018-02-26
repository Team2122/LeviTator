package org.teamtators.levitator.commands;

import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Lift;

public class BumpLift extends Command{

    private Lift lift;
    private double bumpheight;

    public BumpLift(TatorRobot robot, boolean goUp, double bumpHeight) {
        super("bumpLift");
        lift = robot.getSubsystems().getLift();
        requires(lift);
    }

    @Override
    protected boolean step() {
            lift.setDesiredHeight(lift.getCurrentHeight() + bumpheight);
        return true;
    }
}
