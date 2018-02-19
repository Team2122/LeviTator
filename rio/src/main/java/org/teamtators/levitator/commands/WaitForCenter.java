package org.teamtators.levitator.commands;

import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Lift;

public class WaitForCenter extends Command {
    private Lift lift;

    public WaitForCenter(TatorRobot robot) {
        super("WaitForCenter");
        this.lift = robot.getSubsystems().getLift();
    }

    @Override
    protected void initialize() {
        logger.info("Waiting for center");
    }

    @Override
    protected boolean step() {
        return lift.isPivotLocked();
    }
}
