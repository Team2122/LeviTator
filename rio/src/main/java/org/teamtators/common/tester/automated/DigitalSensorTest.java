package org.teamtators.common.tester.automated;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.tester.AutomatedTest;
import org.teamtators.common.tester.AutomatedTestMessage;

import java.util.function.BooleanSupplier;

public class DigitalSensorTest extends AutomatedTest implements Configurable<DigitalSensorTest.Config> {
    private Config config;
    private BooleanSupplier digitalSensor;
    private boolean startValue;
    private Timer timer = new Timer();

    public DigitalSensorTest(String name, BooleanSupplier digitalSensor) {
        super(name, true);
        this.digitalSensor = digitalSensor;
    }

    @Override
    protected void initialize() {
        super.initialize();
        startValue = digitalSensor.getAsBoolean();
        if (startValue != config.desiredStartValue)
            sendMessage("Started with incorrect value of " + startValue, AutomatedTestMessage.Level.ERROR);
        sendMessage("Please activate the digital sensor", AutomatedTestMessage.Level.INFO);
        timer.start();
    }

    @Override
    public boolean step() {
        if (skipped()) {
            sendMessage("Digital sensor value didn't change, test skipped", AutomatedTestMessage.Level.ERROR);
            return true;
        }
        if (digitalSensor.getAsBoolean() != startValue) {
            sendMessage("Sensor value changed, test success", AutomatedTestMessage.Level.INFO);
            return true;
        }
        return false;
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public boolean desiredStartValue;
    }
}
