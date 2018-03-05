package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.drive.PursuitReport;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Drive;

public class WaitForPath extends Command implements Configurable<WaitForPath.Config> {
    private final Drive drive;
    private Config config;

    public WaitForPath(TatorRobot robot) {
        super("WaitForPath");
        this.drive = robot.getSubsystems().getDrive();
    }

    @Override
    protected boolean step() {
        PursuitReport report = drive.getDriveSegmentsFollower().getReport();
        if (report == null) {
            return false;
        }
        if (!Double.isNaN(config.remainingDistance)) {
            return report.remainingDistance <= config.remainingDistance || report.isFinished;
        } else {
            return report.isFinished;
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
