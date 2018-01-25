package org.teamtators.common.tester.components;

import edu.wpi.first.wpilibj.Encoder;
import org.teamtators.common.hw.LogitechF310;
import org.teamtators.common.tester.ManualTest;

public class EncoderTest extends ManualTest {

    private Encoder encoder;

    public EncoderTest(String name, Encoder encoder) {
        super(name);
        this.encoder = encoder;
    }

    @Override
    public void start() {
        printTestInstructions("Press 'A' to display the current values, 'B' to reset the encoder values");
    }

    @Override
    public void onButtonDown(LogitechF310.Button button) {
        if (button == LogitechF310.Button.B) {
            encoder.reset();
            printTestInfo("Encoder reset");
        } else if (button == LogitechF310.Button.A) {
            printTestInfo("Distance: {} (ticks: {}), Rate: {}", encoder.getDistance(), encoder.get(), encoder.getRate());
        }
    }
}
