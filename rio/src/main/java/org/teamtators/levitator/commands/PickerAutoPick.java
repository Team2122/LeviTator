package org.teamtators.levitator.commands;

import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;

public class PickerAutoPick extends Command {
    public PickerAutoPick(TatorRobot robot) {
        super("PickerAutoPick");
    }

    @Override
    protected boolean step() {
        return false;
    }

    public static class Config {

    }
}
