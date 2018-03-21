package org.teamtators.levitator.subsystems;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.helpers.SpeedControllerConfig;
import org.teamtators.common.config.helpers.TalonSRXConfig;
import org.teamtators.common.scheduler.Subsystem;

public class Climber extends Subsystem implements Configurable<Climber.Config> {
    private WPI_TalonSRX climberMotor;
    private SpeedController slaveMotor;


    public Climber() {
        super("Climber");
    }

    public void setClimberMotorPower(double power) {
        climberMotor.set(power);
    }

    public double getClimberPosition() {
        return climberMotor.getSelectedSensorPosition(0);
    }

    @Override
    public void configure(Config config) {
        climberMotor = config.climberMotor.create();
        slaveMotor = config.slaveMotor.create();
        ((WPI_VictorSPX) slaveMotor).set(ControlMode.Follower, config.climberMotor.id);

    }

    public static class Config {
        public TalonSRXConfig climberMotor;
        public SpeedControllerConfig slaveMotor;

    }
}
