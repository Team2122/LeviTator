package org.teamtators.levitator.commands;

import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Drive;
import org.teamtators.levitator.subsystems.OperatorInterface;
import org.teamtators.levitator.subsystems.Subsystems;

public class DriveTank extends Command {
    private TatorRobot robot;
    private Drive drive;
    private OperatorInterface oi;

    public DriveTank(TatorRobot robot) {
        super("DriveTank");
        this.robot = robot;
        drive = ((Subsystems) robot.getSubsystemsBase()).getDrive();
        oi = ((Subsystems) robot.getSubsystemsBase()).getOI();
        requires(drive);
    }

    @Override
    protected boolean step() {
        drive.drivePowers(oi.getDriveLeft(), oi.getDriveRight());
        return false;
    }
}
