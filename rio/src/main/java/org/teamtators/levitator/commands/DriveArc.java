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
        TrapezoidalProfile straightProfile = new TrapezoidalProfile(arcLength, drive.getAverageRate(),
                Math.copySign(config.speed, arcLength), Math.copySign(config.endSpeed, arcLength),
                config.maxAcceleration);
        drive.getStraightMotionFollower().setTravelVelocity(config.speed);
        drive.getStraightMotionFollower().setEndVelocity(Math.copySign(config.endSpeed, arcLength));
        drive.getStraightMotionFollower().setMaxAcceleration(config.maxAcceleration);


        double straightTime = straightProfile.createCalculator().getTotalTime();
        double max_a_a = config.maxAngularAcceleration;
        double max_a_v = Math.abs(-max_a_a * (-straightTime + Math.sqrt(Math.pow(straightTime, 2) - 4 * angleDelta / max_a_a)) / 2.0);

        if (Double.isNaN(max_a_v)) {
            logger.warn("Trying to arc too quickly (arcLength={}, deltaAngle={}). Falling back to default max_a_v",
                    arcLength, angleDelta);
            max_a_v = 100;
        }

        TrapezoidalProfile rotationProfile = new TrapezoidalProfile(angleDelta, 0, Math.copySign(max_a_v, angleDelta),
                0, config.maxAngularAcceleration);
        drive.getRotationMotionFollower().setTravelVelocity(max_a_v);
        drive.getRotationMotionFollower().resetEndVelocity();
        drive.getRotationMotionFollower().setMaxAcceleration(config.maxAngularAcceleration);
        double rotationTime = rotationProfile.createCalculator().getTotalTime();

        logger.debug("straightTime={}, max_a_v={}, rotationTime={}", straightTime, max_a_v, rotationTime);
        drive.driveArcProfile(arcLength, config.angle);

        logger.info("Driving arc from angle {} to {} (rate {}), of distance {} (rate {})",
                startAngle, config.angle, max_a_v, arcLength, config.speed);
    }

    @Override
    protected boolean step() {
//        return drive.isArcOnTarget();
        return drive.isStraightProfileOnTarget();
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
