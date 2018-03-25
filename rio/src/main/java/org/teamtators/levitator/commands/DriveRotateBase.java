package org.teamtators.levitator.commands;

import org.teamtators.common.control.ControllerPredicates;
import org.teamtators.common.control.TrapezoidalProfile;
import org.teamtators.common.control.TrapezoidalProfileFollower;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Drive;

import java.util.function.Predicate;

public abstract class DriveRotateBase extends Command {
    protected final Drive drive;

    private Config config;
    protected double angle;
    protected Predicate<TrapezoidalProfileFollower> predicate = ControllerPredicates.finished();

    protected DriveRotateBase(String name, TatorRobot robot) {
        super(name);
        this.drive = robot.getSubsystems().getDrive();
        requires(drive);
    }

    @Override
    protected void initialize() {
        drive.getRotationMotionFollower().setTravelVelocity(config.rotationSpeed);
        drive.getRotationMotionFollower().resetEndVelocity();
        drive.getRotationMotionFollower().setMaxAcceleration(config.maxAcceleration);
        drive.getRotationMotionFollower().setOnTargetPredicate(predicate);
        drive.driveRotationProfile(angle);
    }

    public boolean step() {
        return drive.isRotationProfileOnTarget();
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
