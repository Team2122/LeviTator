package org.teamtators.levitator.commands;

import org.teamtators.common.TatorRobotBase;
import org.teamtators.common.config.ConfigCommandStore;
import org.teamtators.common.scheduler.Commands;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Drive;
import org.teamtators.levitator.subsystems.Picker;
import org.teamtators.levitator.subsystems.Lift;

public class CommandRegistrar {
    private final TatorRobot robot;

    public CommandRegistrar(TatorRobot robot) {
        this.robot = robot;
    }

    public void register(ConfigCommandStore commandStore) {
        // Drive commands
        Drive drive = robot.getSubsystems().getDrive();
        commandStore.registerCommand("DriveTank", () -> new DriveTank(robot));
        commandStore.registerCommand("DriveStraight", () -> new DriveStraight(robot));
        commandStore.registerCommand("DriveRotate", () -> new DriveRotate(robot));
        commandStore.registerCommand("DriveArc", () -> new DriveArc(robot));
        commandStore.registerCommand("DrivePath", () -> new DrivePathCommand(robot));

        // Picker commands
        Picker picker = robot.getSubsystems().getPicker();
        commandStore.registerCommand("PickerPick", () -> new PickerPick(robot));
        commandStore.registerCommand("PickerRelease", () -> new PickerRelease(robot));
        commandStore.registerCommand("PickerQuickDeploy", () -> new PickerQuickDeploy(robot));
        commandStore.registerCommand("PickerRegrip", () -> new PickerRegrip(robot));

        // Lift commands
        Lift lift = robot.getSubsystems().getLift();
        commandStore.registerCommand("LiftContinuous", () -> new LiftContinuous(robot));
        commandStore.registerCommand("LiftHeightPreset", () -> new LiftHeightPreset(robot));
        commandStore.registerCommand("PivotAnglePreset", () -> new PivotAnglePreset(robot));
        commandStore.registerCommand("WaitForAngle", () -> new WaitForAngle(robot));
        commandStore.registerCommand("WaitForHeight", () -> new WaitForHeight(robot));
        commandStore.putCommand("WaitForCenter", new WaitForCenter(robot));

        commandStore.putCommand("PickerExtend", Commands.instant(picker::extend, picker));
        commandStore.putCommand("PickerRetract", Commands.instant(picker::retract, picker));

        commandStore.putCommand("LiftEnabled", Commands.instant(() -> lift.setSubsystemEnabled(true)));
        commandStore.putCommand("LiftDisabled", Commands.instant(() ->lift.setSubsystemEnabled(false)));

        commandStore.putCommand("BumpLiftUp", Commands.instant(lift::bumpLiftUp));
        commandStore.putCommand("BumpLiftDown", Commands.instant(lift::bumpLiftDown));
        commandStore.putCommand("BumpPivotRight", Commands.instant(lift::bumpPivotRight));
        commandStore.putCommand("BumpPivotLeft", Commands.instant(lift::bumpPivotRight));

        commandStore.registerCommand("AutoSelector", () -> new AutoSelector(robot));

        // FMS Data commands
        commandStore.putCommand("WaitForData", new WaitForData(robot));

        commandStore.putCommand("PrintPose", Commands.instant(() ->
                TatorRobotBase.logger.info("Pose: " + drive.getPose())));
    }
}
