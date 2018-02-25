package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Drive;
import org.teamtators.levitator.subsystems.DriveArcController;

public class DriveStraight extends Command implements Configurable<DriveStraight.Config> {
    private Drive drive;
    private Config config;

    public DriveStraight(TatorRobot robot) {
        super("DriveStraight");
        drive = robot.getSubsystems().getDrive();
        requires(drive);
    }

    @Override
    protected void initialize() {
        String angleStr;
        double angle;
        if (Double.isNaN(config.angle)) {
            angle = drive.getYawAngle();
            angleStr = "current angle (" + angle + ")";
        } else {
            angle = config.angle;
            angleStr = "angle " + angle + " (currently at " + drive.getYawAngle() + ")";
        }
        logger.info("Driving at {} for distance of {} at top speed of {}",
                angleStr, config.distance, config.speed);
        drive.getArcController().setMaxSpeed(config.speed);
        drive.getArcController().setEndVelocity(Math.copySign(config.endSpeed, config.distance));
        drive.getArcController().setMaxAcceleration(config.maxAcceleration);
        drive.getArcController().setOnTargetPredicate(DriveArcController::areStraightsOnTarget);
        drive.driveStraightProfile(angle, config.distance);
    }

    @Override
    protected boolean step() {
        return drive.isArcOnTarget();
    }

    @Override
    protected void finish(boolean interrupted) {
        double distance = drive.getArcController().getAverageDistance();
        double angle = drive.getYawAngle();
        String logString = String.format(" at distance %s (target %s), angle %s (target %s)",
                distance, config.distance, angle, drive.getArcController().getYawAngleController().getSetpoint());
        if (interrupted) {
            logger.warn("Interrupted" + logString);
            drive.stop();
        } else {
            logger.info("Finishing" + logString);
        }
    }

    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public double angle = Double.NaN;
        public double speed;
        public double endSpeed;
        public double maxAcceleration;
        public double distance;
    }
}

