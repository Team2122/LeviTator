package org.teamtators.levitator.commands;

import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Lift;
import org.teamtators.levitator.subsystems.Pivot;

public class LiftRecall extends Command {
    private Lift lift;
    private Pivot pivot;
    public LiftRecall(TatorRobot robot) {
        super("LiftRecall");
        this.lift = robot.getSubsystems().getLift();
        this.pivot = robot.getSubsystems().getPivot();
    }

    @Override
    public void initialize() {
        logger.info("Recalling lift and pivot");
        logger.info("Recalling pivot");
        pivot.clearForceRotationFlag();
        lift.clearForceHeightFlag();
//        lift.recallHeight();
    }

    @Override
    public void finish(boolean interrupted) {

    }

    @Override
    protected boolean step() {
        return true;
    }
}
