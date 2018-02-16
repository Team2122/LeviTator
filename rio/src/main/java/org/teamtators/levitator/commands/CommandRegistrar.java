package org.teamtators.levitator.commands;

import org.teamtators.common.config.ConfigCommandStore;
import org.teamtators.common.scheduler.Commands;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Picker;
import org.teamtators.levitator.subsystems.Lift;

public class CommandRegistrar {
    private final TatorRobot robot;

    public CommandRegistrar(TatorRobot robot) {
        this.robot = robot;
    }

    public void register(ConfigCommandStore commandStore) {
        // Drive commands
        commandStore.registerCommand("DriveTank", () -> new DriveTank(robot));

        // Picker commands
        Picker picker = robot.getSubsystems().getPicker();
        commandStore.registerCommand("PickerPick", () -> new PickerPick(robot));
        commandStore.registerCommand("PickerRelease", () -> new PickerRelease(robot));

        // Lift commands
        Lift lift = robot.getSubsystems().getLift();
        commandStore.putCommand("LiftContinuous", new LiftContinuous(robot));
        commandStore.registerCommand("LiftHeightPreset", () -> new LiftHeightPreset(robot));
        commandStore.registerCommand("PivotAnglePreset", () -> new PivotAnglePreset(robot));

        commandStore.putCommand("PickerExtend", Commands.instant(picker::extend, picker));
        commandStore.putCommand("PickerRetract", Commands.instant(picker::retract, picker));

        commandStore.registerCommand("BumpLift", () -> new BumpLift(robot, true, 8));
        commandStore.registerCommand("BumpPivot", () -> new BumpPivot(robot, false, 56));
    }
}
