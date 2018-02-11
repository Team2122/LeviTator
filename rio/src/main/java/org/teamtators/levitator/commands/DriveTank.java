package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.util.JoystickModifiers;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Drive;
import org.teamtators.levitator.subsystems.OperatorInterface;
import org.teamtators.levitator.subsystems.Subsystems;

public class DriveTank extends Command implements Configurable<DriveTank.Config> {
    private final Drive drive;
    private final OperatorInterface oi;

    private JoystickModifiers modifiers;

    public DriveTank(TatorRobot robot) {
        super("DriveTank");
        drive = robot.getSubsystems().getDrive();
        oi = robot.getSubsystems().getOI();
        requires(drive);
    }

    @Override
    protected boolean step() {
        double left = oi.getDriveLeft();
        double right = oi.getDriveRight();

        left = modifiers.apply(left);
        right = modifiers.apply(right);

        drive.drivePowers(left, right);
        return false;
    }

    @Override
    public void configure(Config config) {
        this.modifiers = config.modifiers;
    }

    public static class Config {
        public JoystickModifiers modifiers;
    }
}
