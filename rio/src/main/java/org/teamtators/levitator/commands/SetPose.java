package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.math.Pose2d;
import org.teamtators.common.math.Rotation;
import org.teamtators.common.math.Translation2d;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Drive;

public class SetPose extends Command implements Configurable<SetPose.Config> {
    private final Drive drive;
    private Config config;

    public SetPose(TatorRobot robot) {
        super("SetPose");
        this.drive = robot.getSubsystems().getDrive();
    }

    @Override
    protected void initialize() {
        Pose2d pose = new Pose2d(config.translation, config.rotation);
        logger.info("Setting pose to " + pose);
        drive.getPoseEstimator().setPose(pose);
    }

    @Override
    public boolean step() {
        return true;
    }

    @Override
    protected void finish(boolean interrupted) {
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public Translation2d translation = Translation2d.zero();
        public Rotation rotation = Rotation.identity();
        public void setX(double x) {
            translation = translation.withX(x);
        }
        public void setY(double y) {
            translation = translation.withY(y);
        }
        public void setYaw(double yawDegrees) {
            rotation = Rotation.fromDegrees(yawDegrees);
        }
    }
}
