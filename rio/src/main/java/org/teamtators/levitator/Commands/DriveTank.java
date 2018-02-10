package org.teamtators.levitator.Commands;

import org.teamtators.common.SubsystemsBase;
import org.teamtators.common.control.Timer;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Drive;
import org.teamtators.levitator.subsystems.Subsystems;

public class DriveTank extends Command {
    private TatorRobot robot;
    private Drive drive;
    //private Oi oi;
    private Timer timer = new Timer();

    public DriveTank(TatorRobot robot) {
        super("DriveTank");
        this.robot = robot;
        drive = ((Subsystems)robot.getSubsystemsBase()).getDrive();

        requires(drive);
    }

    @Override
    protected boolean step() {
        return false;
    }
}
