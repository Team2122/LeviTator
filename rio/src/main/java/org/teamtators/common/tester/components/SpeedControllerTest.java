package org.teamtators.common.tester.components;

import edu.wpi.first.wpilibj.PowerDistributionPanel;
import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.config.helpers.SpeedControllerConfig;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.tester.ManualTest;

public class SpeedControllerTest extends ManualTest {

    private SpeedController motor;
    private int fullspeed;
    private double axisValue;
    private PowerDistributionPanel pdp;
    private SpeedControllerConfig motorConfig;

    public SpeedControllerTest(String name, SpeedController motor) {
        super(name);
        this.motor = motor;
    }

    public SpeedControllerTest(String name, SpeedController motor, PowerDistributionPanel pdp,
                               SpeedControllerConfig motorConfig) {
        this(name, motor);
        this.pdp = pdp;
        this.motorConfig = motorConfig;
    }

    @Override
    public void start() {
        printTestInstructions("Push joystick in direction to move (forward +, backward -), back/start to drive +/- at full speed");
        if (this.pdp != null) {
            printTestInstructions("Press A to get current usage");
        }
        fullspeed = 0;
        axisValue = 0;
    }

    private double getSpeed() {
        if (fullspeed != 0) {
            return fullspeed;
        } else {
            return axisValue;
        }
    }

    public double getCurrent() {
        return motorConfig.getTotalCurrent(pdp);
    }

    @Override
    public void update(double delta) {
        motor.set(getSpeed());
    }

    @Override
    public void stop() {
        motor.set(0);
    }

    @Override
    public void onButtonDown(LogitechF310.Button button) {
        if (button == LogitechF310.Button.BACK) fullspeed--;
        else if (button == LogitechF310.Button.START) fullspeed++;
        else if (button == LogitechF310.Button.A && pdp != null) {
            logger.info("Total current usage: " + this.getCurrent());
        }
    }

    @Override
    public void onButtonUp(LogitechF310.Button button) {
        if (button == LogitechF310.Button.BACK) fullspeed++;
        else if (button == LogitechF310.Button.START) fullspeed--;
    }

    @Override
    public void updateAxis(double value) {
        axisValue = value;
    }
}
