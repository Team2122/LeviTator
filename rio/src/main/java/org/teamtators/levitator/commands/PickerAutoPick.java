package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Drive;
import org.teamtators.levitator.subsystems.Picker;
import org.teamtators.levitator.subsystems.Vision;
import org.teamtators.levitator.subsystems.VisionOutput;

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
        requires(picker);
    }

    @Override
    protected void initialize() {
        logger.info("Driving to cube");
        startDistance = drive.getAverageDistance();
    }

    @Override
    protected boolean step() {
        VisionOutput output = vision.getLastOutput();
        Double angle = vision.getNewRobotAngle(output);
        Double distance = vision.getDistance(output);
        logger.trace(String.format("Distance from cube %5.3f at angle %5.3f", distance, angle));
        if(angle != null && distance != null) {
            drive.driveHeading(angle, config.velocity);
        }
        return drive.getAverageDistance() - startDistance >= config.maxDriveDistance || picker.isCubeDetectedAny();
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
