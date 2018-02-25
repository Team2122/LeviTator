package org.teamtators.levitator.commands;

import org.teamtators.common.control.ControllerPredicates;
import org.teamtators.common.control.TrapezoidalProfile;
import org.teamtators.common.control.TrapezoidalProfileFollower;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Drive;
import org.teamtators.levitator.subsystems.DriveArcController;

import java.util.function.Predicate;

public abstract class DriveRotateBase extends Command {
    protected final Drive drive;

    private Config config;
    protected double angle;
    protected Predicate<DriveArcController> predicate = DriveArcController::areStraightsOnTarget;

    protected DriveRotateBase(String name, TatorRobot robot) {
        super(name);
        this.drive = robot.getSubsystems().getDrive();
        requires(drive);
    }

    @Override
    protected void initialize() {
        drive.getArcController().setMaxSpeed(config.rotationSpeed);
        drive.getArcController().setEndSpeed(0.0);
        drive.getArcController().setMaxAcceleration(config.maxAcceleration);
        drive.getArcController().setOnTargetPredicate(predicate);
        drive.driveRotationProfile(angle);
    }

    protected boolean step() {
        return drive.isArcOnTarget();
    }

    @Override
    protected void finish(boolean interrupted) {
        drive.stop();
    }

    protected void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public double rotationSpeed;
        public double maxAcceleration;
    }
}
