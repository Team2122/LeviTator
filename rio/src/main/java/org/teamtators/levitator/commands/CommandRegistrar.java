package org.teamtators.levitator.commands;

import org.teamtators.common.config.ConfigCommandStore;
import org.teamtators.common.scheduler.Commands;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Picker;

public class CommandRegistrar {
    private final TatorRobot robot;

    public CommandRegistrar(TatorRobot robot) {
        this.robot = robot;
    }

    public void register(ConfigCommandStore commandStore) {
        commandStore.registerCommand("DriveTank", () -> new DriveTank(robot));


        Picker picker = robot.getSubsystems().getPicker();
        commandStore.registerCommand("PickerPick", () -> new PickerPick(robot));
        commandStore.registerCommand("PickerRelease", () -> new PickerRelease(robot));

        commandStore.putCommand("LiftContinuous", new LiftContinuous(robot));
        commandStore.registerCommand("LiftHeightPreset", () -> new LiftHeightPreset(robot));
        commandStore.registerCommand("PivotAnglePreset", () -> new PivotAnglePreset(robot));

        commandStore.putCommand("PickerExtend", Commands.instant(picker::extend, picker));
        commandStore.putCommand("PickerRetract", Commands.instant(picker::retract, picker));

    }
}
