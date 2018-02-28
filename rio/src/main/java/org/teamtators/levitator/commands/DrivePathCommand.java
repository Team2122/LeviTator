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
        super.initialize();
        drive.driveSegments(driveSegments);
    }

    @Override
    protected boolean step() {
        return drive.isDriveSegmentsFollowerFinished();
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted);
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
            drivePath.addPoint(point);
        }
        this.driveSegments = drivePath.toSegments();
    }

    public static class Config {
        public double speed;
        public double arcSpeed;
        public double radius;
        public List<DrivePath.Point> path;
    }
}
