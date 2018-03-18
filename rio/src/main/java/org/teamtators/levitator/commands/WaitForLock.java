package org.teamtators.levitator.commands;

import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Lift;

public class WaitForLock extends Command {
    private Lift lift;

    public WaitForLock(TatorRobot robot) {
        super("WaitForLock");
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
