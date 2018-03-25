package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Climber;

public class ClimberMoveToHeight extends Command implements Configurable<ClimberMoveToHeight.Config> {
    private final Climber climber;
    private Config config;
    private double direction;

    public ClimberMoveToHeight(TatorRobot robot) {
        super("ClimberMoveToHeight");
        this.climber = robot.getSubsystems().getClimber();
    }

    @Override
    protected void initialize() {
        if (!climber.isHomed()) {
            logger.error("Climber is not homed!");
            this.cancel();
            return;
        }
        direction = Math.signum(config.height - climber.getPosition());
        logger.info("Going to {} (direction is {})", config.height, direction == 1 ? "positive" : direction == -1 ? "negative" : "0");
    }

    @Override
    public boolean step() {
        double position = climber.getPosition();

        boolean atHeight = direction == 1 ? position >= config.height : position <= config.height;
        boolean limitTripped = direction == 1 ? climber.isAtTopLimit() : climber.isAtBottomLimit();

        if (atHeight) {
            logger.info("At height {} (target was {})", position, config.height);
            return true;
        }

        if (limitTripped) {
            logger.warn("Limit tripped");
            return true;
        }

        climber.setPower(config.power * direction);
        return false;
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public void finish(boolean interrupted) {
        super.finish(interrupted);
        climber.setPower(0);
    }

    public static class Config {
        public double height;
        public double power;
    }
}
