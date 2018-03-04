package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Elevators;

public class ElevatorsLiftLeft extends Command implements Configurable<ElevatorsLiftLeft.Config> {
    private final Elevators elevators;
    private Config config;

    public ElevatorsLiftLeft(TatorRobot robot) {
        super("ElevatorsLiftLeft");
        this.elevators = robot.getSubsystems().getElevators();
    }

    @Override
    protected boolean step() {
        if (elevators.isSafeToLiftElevators()) {
            if (elevators.isFlapDetected()) {
                elevators.setFlapPower(0.0);
                return true;
            }
            elevators.setFlapPower(config.flapPower);
            return false;
        }

        return true;
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public double flapPower;
    }
}
