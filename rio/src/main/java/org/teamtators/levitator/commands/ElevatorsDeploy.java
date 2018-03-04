package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Elevators;

public class ElevatorsDeploy extends Command implements Configurable<ElevatorsDeploy.Config>{

    private final Elevators elevators;
    public Timer retractTimer = new Timer();
    private Config config;

    public ElevatorsDeploy(TatorRobot robot) {
        super("ElevatorsDeploy");
        this.elevators = robot.getSubsystems().getElevators();
    }

    @Override
    protected void initialize() {
        retractTimer.start();
    }

    @Override
    protected boolean step() {
        if(retractTimer.hasPeriodElapsed(config.retractTime)) {
            elevators.slide(false);
            return false;
        }
        elevators.slide(true);
        elevators.deployElevators();
        return true;
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public static class Config{
        public double retractTime;
    }
}

