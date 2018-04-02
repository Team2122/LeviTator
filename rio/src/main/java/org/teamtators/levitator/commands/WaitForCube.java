package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Picker;

public class WaitForCube extends Command implements Configurable<WaitForCube.Config> {
    private Picker picker;

    WaitForCube(TatorRobot robot) {
        super("WaitForCube");
        this.picker = robot.getSubsystems().getPicker();
    }

    @Override
    protected void initialize() {
        logger.info("Waiting for cube detect");
    }

    @Override
    public boolean step() {
        return picker.isCubeDetected();
    }

    @Override
    public void configure(Config config) {
    }

    public static class Config {
    }
}
