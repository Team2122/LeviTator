package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.TrapezoidalProfile;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Drive;

public class DriveArc extends Command implements Configurable<DriveArc.Config> {
    private final Drive drive;
    private Config config;

    public DriveArc(TatorRobot robot) {
        super("DriveArc");
        drive = robot.getSubsystems().getDrive();
        requires(drive);
    }

    @Override
    protected void initialize() {
        double startAngle = drive.getYawAngle();
        double angleDelta = config.angle - startAngle;

        double arcLength = config.arcLength;
        if (Double.isNaN(arcLength)) {
            arcLength = 2 * Math.PI * config.radius / (360.0 / Math.abs(angleDelta));
        }

        drive.driveArcProfile(arcLength, config.angle);

        logger.info("Driving arc from angle {} to {} of distance {} (rate {})",
                startAngle, config.angle, arcLength, config.speed);
    }

    @Override
    protected boolean step() {
        return drive.isArcOnTarget();
    }

    @Override
    protected void finish(boolean interrupted) {
        double distance = drive.getStraightMotionFollower().getCurrentPosition();
        double targetDistance = drive.getStraightMotionFollower().getCalculator().getPosition();
        double angle = drive.getYawAngle();
        String logString = String.format(" at distance %s (target %s), angle %s (target %s)",
                distance, targetDistance, angle, config.angle);
        if (interrupted) {
            logger.warn("Interrupted" + logString);
        } else {
            logger.info("Finishing" + logString);
        }
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public double angle;
        public double speed;
        public double endSpeed;
        public double maxAcceleration;
        public double maxAngularAcceleration;
        public double arcLength = Double.NaN;
        public double radius;
    }
}
