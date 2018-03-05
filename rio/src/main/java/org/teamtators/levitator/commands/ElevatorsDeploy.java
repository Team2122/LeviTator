package org.teamtators.levitator.commands;

import edu.wpi.first.wpilibj.DoubleSolenoid;
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
        if(!elevators.isDeployed()) {
            if (!retractTimer.hasPeriodElapsed(config.retractTime)) {
                elevators.slide(DoubleSolenoid.Value.kReverse);
                return false;
            }
            elevators.slide(DoubleSolenoid.Value.kForward);
            elevators.deployElevators();
            return true;
        } else {
            elevators.undeploy();
            return true;
        }
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public static class Config{
        public double retractTime;
    }
}

