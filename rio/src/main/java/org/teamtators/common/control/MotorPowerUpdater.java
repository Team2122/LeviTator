package org.teamtators.common.control;

import com.google.common.util.concurrent.AtomicDouble;
import edu.wpi.first.wpilibj.SpeedController;

public class MotorPowerUpdater implements Updatable {
    private SpeedController motor;
    private AtomicDouble power = new AtomicDouble();

    public MotorPowerUpdater(SpeedController motor) {
        this.motor = motor;
    }

    public void set(double power) {
        this.power.set(power);
    }

    @Override
    public void update(double delta) {
        motor.set(power.get());
    }
}
