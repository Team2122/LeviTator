package org.teamtators.levitator.commands;

import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Pivot;

public class WaitForLock extends Command {
    private Pivot pivot;

    public WaitForLock(TatorRobot robot) {
        super("WaitForLock");
        this.pivot = robot.getSubsystems().getPivot();
    }

    @Override
    protected void initialize() {
        logger.info("Waiting for pivot to lock");
    }

    @Override
    public boolean step() {
        return pivot.isPivotLocked();
    }
}
