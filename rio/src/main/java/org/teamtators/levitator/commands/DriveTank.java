package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.scheduler.RobotState;
import org.teamtators.common.util.JoystickModifiers;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Drive;
import org.teamtators.levitator.subsystems.Lift;
import org.teamtators.levitator.subsystems.OperatorInterface;
import org.teamtators.levitator.subsystems.Subsystems;

public class DriveTank extends Command implements Configurable<DriveTank.Config> {
    private final Drive drive;
    private final OperatorInterface oi;
    private final Lift lift;

    private JoystickModifiers modifiers;
    private Config config;

    public DriveTank(TatorRobot robot) {
        super("DriveTank");
        drive = robot.getSubsystems().getDrive();
        oi = robot.getSubsystems().getOI();
        lift = robot.getSubsystems().getLift();
        requires(drive);
        validIn(RobotState.TELEOP);
    }

    @Override
    public boolean step() {
        double left = oi.getDriveLeft();
        double right = oi.getDriveRight();

        double liftHeight = lift.getCurrentHeight();
        double scale = 1;

        if (liftHeight > config.slowHeight) {
            scale = config.slowScaler;
        }

        modifiers.scale = scale;

        left = modifiers.apply(left);
        right = modifiers.apply(right);

        drive.drivePowers(left, right);
        return false;
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        this.modifiers = config.modifiers;
    }

    public static class Config {
        public JoystickModifiers modifiers;
        public double slowHeight;
        public double slowScaler;
    }
}
