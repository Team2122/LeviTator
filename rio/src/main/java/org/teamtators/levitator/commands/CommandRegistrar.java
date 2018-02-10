package org.teamtators.levitator.commands;

import org.teamtators.common.config.ConfigCommandStore;
import org.teamtators.levitator.TatorRobot;

public class CommandRegistrar {
    private final TatorRobot robot;

    public CommandRegistrar(TatorRobot robot) {
        this.robot = robot;
    }

    public void register(ConfigCommandStore commandStore) {
        commandStore.putCommand("Drive.Tank", new DriveTank(robot));
    }
}
