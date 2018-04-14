package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.drive.DrivePath;
import org.teamtators.common.drive.DriveSegments;
import org.teamtators.common.drive.PathPoint;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Drive;

import java.util.List;

/**
 * @author Alex Mikhalev
 */
public class DrivePathCommand extends Command implements Configurable<DrivePathCommand.Config> {
    private final Drive drive;
    private DrivePath drivePath;
    private DriveSegments driveSegments;
    private Config config;

    public DrivePathCommand(TatorRobot robot) {
        super("DrivePath");
        this.drive = robot.getSubsystems().getDrive();
        requires(drive);
    }

    @Override
    protected void initialize() {
        logger.info("Starting driving path at " + drive.getPose());
        drive.getDriveSegmentsFollower().setMaxAcceleration(config.maxAcceleration);
        drive.driveSegments(driveSegments);
    }

    @Override
    public boolean step() {
        return drive.isDriveSegmentsFollowerFinished();
    }

    @Override
    protected void finish(boolean interrupted) {
        logger.info((interrupted ? "Interrupted" : "Finished") + " driving path at " + drive.getPose());
        drive.stop();
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        this.drivePath = new DrivePath();
        for (int i = 0; i < config.path.size(); i++) {
            PathPoint point = config.path.get(i);
            boolean isLast = i == config.path.size() - 1;
            if (Double.isNaN(point.getRadius())) {
                point.setRadius(config.radius);
            }
            if (Double.isNaN(point.getSpeed())) {
                if (isLast) {
                    point.setSpeed(0);
                } else {
                    point.setSpeed(config.speed);
                }
            }
            if (Double.isNaN(point.getArcSpeed())) {
                if (isLast) {
                    point.setArcSpeed(0);
                } else {
                    point.setArcSpeed(config.arcSpeed);
                }
            }
            if (point.isReverse() == null) {
                point.setReverse(config.reverse);
            }
            drivePath.addPoint(point);
        }
        this.driveSegments = drivePath.toSegments();
        logger.trace("segments: " + driveSegments);
    }

    public static class Config {
        public double speed;
        public double arcSpeed;
        public double maxAcceleration;
        public double radius;
        public boolean reverse = false;
        public List<PathPoint> path;
    }
}
