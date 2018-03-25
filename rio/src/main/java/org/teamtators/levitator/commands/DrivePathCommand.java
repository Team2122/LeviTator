package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.drive.DrivePath;
import org.teamtators.common.drive.DriveSegments;
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

    public DrivePathCommand(TatorRobot robot) {
        super("DrivePath");
        this.drive = robot.getSubsystems().getDrive();
        requires(drive);
    }

    @Override
    protected void initialize() {
        logger.info("Starting driving path at " + drive.getPose());
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
        this.drivePath = new DrivePath();
        for (DrivePath.Point point : config.path) {
            if (Double.isNaN(point.getRadius())) {
                point.setRadius(config.radius);
            }
            if (Double.isNaN(point.getSpeed())) {
                point.setSpeed(config.speed);
            }
            if (Double.isNaN(point.getArcSpeed())) {
                point.setArcSpeed(config.arcSpeed);
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
        public double radius;
        public boolean reverse = false;
        public List<DrivePath.Point> path;
    }
}
