package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.levitator.TatorRobot;

public class DriveRotate extends DriveRotateBase implements Configurable<DriveRotate.Config> {
    private Config config;

    public DriveRotate(TatorRobot robot) {
        super("DriveRotate", robot);
        requires(drive);
    }

    @Override
    public void configure(Config config) {
        super.configure(config);
        this.config = config;
        this.angle = config.angle;
    }

    @Override
    protected void initialize() {
        super.initialize();
        double initialAngle = drive.getYawAngle();
        logger.info("Drive rotating from angle {} to {} (max rate {})",
                initialAngle, angle, config.rotationSpeed);
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted);
        String logLine = String.format(" at angle %f (target %f)",
                drive.getYawAngle(), angle);
        if (interrupted) {
            logger.warn("DriveRotate Interrupted" + logLine);
        } else {
            logger.info("DriveRotate Finished" + logLine);
        }
    }

    public static class Config extends DriveRotateBase.Config {
        public double angle;
    }
}

