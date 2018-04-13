package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Ramper;
import org.teamtators.common.control.Timer;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.scheduler.RobotState;
import org.teamtators.common.util.JoystickModifiers;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Drive;
import org.teamtators.levitator.subsystems.Lift;
import org.teamtators.levitator.subsystems.OperatorInterface;

public class DriveTank extends Command implements Configurable<DriveTank.Config> {
    private final Drive drive;
    private final OperatorInterface oi;
    private final Lift lift;

    private JoystickModifiers modifiers;
    private Config config;

    private Ramper rightRamper = new Ramper();
    private Ramper leftRamper = new Ramper();
    private Timer timer = new Timer();


    public DriveTank(TatorRobot robot) {
        super("DriveTank");
        drive = robot.getSubsystems().getDrive();
        oi = robot.getSubsystems().getOI();
        lift = robot.getSubsystems().getLift();
        requires(drive);
        validIn(RobotState.TELEOP);
    }

    @Override
    protected void initialize() {
        super.initialize();
        timer.start();
    }

    @Override
    public boolean step() {
        double left = oi.getDriveLeft();
        double right = oi.getDriveRight();

        double liftHeight = lift.getCurrentHeight();
        double scale = 1;
        double maxAcceleration;

        if (liftHeight > config.slowerHeight) {
            scale = config.slowerScaler;
            maxAcceleration = config.maxAccelerationSlower;
        } else if (liftHeight > config.slowHeight) {
            scale = config.slowScaler;
            maxAcceleration = config.maxAccelerationSlow;
        } else {
            maxAcceleration = config.maxAcceleration;
        }

        leftRamper.setMaxAcceleration(maxAcceleration);
        rightRamper.setMaxAcceleration(maxAcceleration);
        modifiers.scale = scale;

        left = modifiers.apply(left);
        right = modifiers.apply(right);

        double delta = timer.restart();

        leftRamper.setValue(left);
        rightRamper.setValue(right);
        leftRamper.update(delta);
        rightRamper.update(delta);

        drive.drivePowers(leftRamper.getOutput(), rightRamper.getOutput());
        return false;
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        leftRamper.setOnlyUp(false);
        rightRamper.setOnlyUp(false);
        this.modifiers = config.modifiers;
    }

    public static class Config {
        public JoystickModifiers modifiers;
        public double maxAcceleration;
        public double slowHeight;
        public double slowScaler;
        public double maxAccelerationSlow;
        public double slowerHeight;
        public double slowerScaler;
        public double maxAccelerationSlower;
    }
}
