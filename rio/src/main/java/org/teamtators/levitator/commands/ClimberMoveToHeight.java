package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Climber;

public class ClimberMoveToHeight extends Command implements Configurable<ClimberMoveToHeight.Config> {
    private final Climber climber;
    private Config config;

    public ClimberMoveToHeight(TatorRobot robot) {
        super("ClimberMoveToHeight");
        this.climber = robot.getSubsystems().getClimber();
    }

    @Override
    protected boolean step() {
        climber.setPower(config.power);
        return climber.isAtTopLimit() || Math.abs(climber.getPosition() - config.height) >= config.heightTolerance;
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
        public double heightTolerance;
    }
}
