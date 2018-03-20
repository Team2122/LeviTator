package org.teamtators.levitator.commands;

import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Lift;

public class LiftRecall extends Command {
    private Lift lift;
    public LiftRecall(TatorRobot robot) {
        super("LiftRecall");
        this.lift = robot.getSubsystems().getLift();
    }

    @Override
    public void initialize() {
        logger.info("Recalling pivot");
        lift.clearForceRotationFlag();
        lift.recallHeight();
    }

    @Override
    public void finish(boolean interrupted) {

    }

    @Override
    protected boolean step() {
        return true;
    }
}
