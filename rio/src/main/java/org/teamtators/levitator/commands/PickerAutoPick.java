package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Drive;
import org.teamtators.levitator.subsystems.Picker;
import org.teamtators.levitator.subsystems.Vision;
import org.teamtators.levitator.subsystems.DetectedObject;

public class PickerAutoPick extends Command implements Configurable<PickerAutoPick.Config> {
    private final Vision vision;
    private final Drive drive;
    private final Picker picker;
    private double startDistance;
    private Config config;

    public PickerAutoPick(TatorRobot robot) {
        super("PickerAutoPick");
        this.vision = robot.getSubsystems().getVision();
        this.drive = robot.getSubsystems().getDrive();
        this.picker = robot.getSubsystems().getPicker();
        requires(vision);
        requires(drive);
    }

    @Override
    protected void initialize() {
        logger.info("Driving to cube");
        startDistance = drive.getCenterDistance();
    }

    @Override
    public boolean step() {
        DetectedObject detected = vision.getLastDetectedObject();
        Double angle = vision.getNewRobotAngle(detected);
        Double distance = vision.getDistance(detected);
        double currentAngle = drive.getYawAngle();
        logger.trace(String.format("Distance from cube %5.3f, target angle %5.3f, drive angle %5.3f", distance, angle, currentAngle));
        if(angle != null && distance != null) {
            drive.driveHeading(angle, config.velocity);
        }
        return drive.getCenterDistance() - startDistance >= config.maxDriveDistance || picker.isCubeDetectedAny();
    }

    @Override
    protected void finish(boolean interrupted) {
        drive.stop();
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public double velocity;
        public double maxDriveDistance;
    }
}
