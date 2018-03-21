package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;

public class ClimberHome extends Command implements Configurable<ClimberHome>{
    private ClimberHome config;

    public ClimberHome(TatorRobot robot) {
        super("ClimberHome");
    }

    @Override
    protected boolean step() {
        return false;
    }

    @Override
    public void configure(ClimberHome config) {
        this.config=config;
    }

    public static class Config{

    }
}
