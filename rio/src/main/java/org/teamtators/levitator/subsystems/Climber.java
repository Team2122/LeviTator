package org.teamtators.levitator.subsystems;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.helpers.DigitalSensorConfig;
import org.teamtators.common.config.helpers.EncoderConfig;
import org.teamtators.common.config.helpers.SpeedControllerConfig;
import org.teamtators.common.config.helpers.TalonSRXConfig;
import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.scheduler.Subsystem;

public class Climber extends Subsystem implements Configurable<Climber.Config> {
    private WPI_TalonSRX climberMotor;
    private SpeedController slaveMotor;
    private DigitalSensor topLimit;
    private DigitalSensor bottomLimit;
    private Config config;

    public Climber() {
        super("Climber");
    }

    public void setPower(double power) {
        climberMotor.set(power);
    }

    public double getPosition() {
        return climberMotor.getSelectedSensorPosition(0) * config.distancePerPulse;
    }

    public boolean isAtBottomLimit() {
        return bottomLimit.get();
    }

    public boolean isAtTopLimit() {
        return topLimit.get();
    }

    public void resetPosition() {
        climberMotor.setSelectedSensorPosition(0, 0, 0);
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        climberMotor = config.climberMotor.create();
        slaveMotor = config.slaveMotor.create();
        topLimit = config.topLimit.create();
        bottomLimit = config.bottomLimit.create();
        ((WPI_VictorSPX) slaveMotor).set(ControlMode.Follower, config.climberMotor.id);

    }

    public static class Config {
        public TalonSRXConfig climberMotor;
        public SpeedControllerConfig slaveMotor;
        public double distancePerPulse;
        public DigitalSensorConfig topLimit;
        public DigitalSensorConfig bottomLimit;

    }
}
