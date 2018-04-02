package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Climber;

public class ClimberHome extends Command implements Configurable<ClimberHome.Config> {
    private Config config;
    private Climber climber;

    ClimberHome(TatorRobot robot) {
        super("ClimberHome");
        this.climber = robot.getSubsystems().getClimber();
        requires(climber);
    }

    @Override
    public boolean step() {
        climber.setPower(config.power);
        return climber.isAtBottomLimit();
    }

    protected void finish(boolean interrupted) {
        climber.setPower(0.0);
        if (!interrupted) {
            logger.info("Climber has been homed");
            climber.resetPosition();
            climber.setHomed(true);
        } else {
            logger.warn("Climber home interrupted");
        }
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public double power;
    }
}
