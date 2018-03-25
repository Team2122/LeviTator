package org.teamtators.levitator.commands;

import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Auto;

public class WaitForData extends Command {
    private Auto auto;

    public WaitForData(TatorRobot robot) {
        super("WaitForData");
        this.auto = robot.getSubsystems().getAuto();
    }

    @Override
    public boolean step() {
        return auto.getData() != null;
    }
}
