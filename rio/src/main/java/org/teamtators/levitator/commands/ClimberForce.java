package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Climber;

public class ClimberForce extends Command implements Configurable<ClimberForce.Config> {
    private final Climber climber;
    private Config config;

    public ClimberForce(TatorRobot robot) {
        super("ClimberForce");
        this.climber = robot.getSubsystems().getClimber();
        requires(climber);
    }

    @Override
    protected void initialize() {
        logger.info("Forcing climber at power {}", config.power);
    }

    @Override
    public boolean step() {
        climber.setPower(config.power, true);
        return false;
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public void finish(boolean interrupted) {
        super.finish(interrupted);
        climber.setPower(0, false);
    }

    public static class Config {
        public double power;
    }
}
