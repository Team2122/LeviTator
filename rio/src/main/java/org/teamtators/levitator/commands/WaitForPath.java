package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.drive.PursuitReport;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Drive;

public class WaitForPath extends Command implements Configurable<WaitForPath.Config> {
    private final Drive drive;
    private Config config;
    private PursuitReport report;

    public WaitForPath(TatorRobot robot) {
        super("WaitForPath");
        this.drive = robot.getSubsystems().getDrive();
    }

    @Override
    protected void initialize() {
    }

    @Override
    public boolean step() {
        report = drive.getDriveSegmentsFollower().getReport();
        if (report == null) {
            return false;
        }
        if (!Double.isNaN(config.remainingDistance)) {
            if (report.remainingDistance <= config.remainingDistance || report.isFinished){
                logger.info("WaitForPath finished remaining distance {} < {}", report.remainingDistance, config.remainingDistance);
                return true;
            }
        }
        if (report.isFinished) {
            logger.info("WaitForPath finished because path finished");
            return true;
        }
        else return false;
    }

    @Override
    protected void finish(boolean interrupted) {
        if (interrupted) {
            logger.error("WaitForPath interrupted");
        }
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public double remainingDistance = Double.NaN;
    }
}
