package org.teamtators.common.tester.components;

import edu.wpi.first.wpilibj.DoubleSolenoid;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.tester.ManualTest;

public class DoubleSolenoidTest extends ManualTest {
    private DoubleSolenoid doubleSolenoid;

    public DoubleSolenoidTest(String name, DoubleSolenoid doubleSolenoid) {
        super(name);
        this.doubleSolenoid = doubleSolenoid;
    }

    @Override
    public void start() {
        printTestInstructions("Press A to make the solenoid go forward, B to go backward, and X to turn off");
    }

    @Override
    public void onButtonDown(LogitechF310.Button button) {
        if (button == LogitechF310.Button.A) {
            doubleSolenoid.set(DoubleSolenoid.Value.kForward);
            printTestInfo("Solenoid set to go forward");
        } else if (button == LogitechF310.Button.B) {
            doubleSolenoid.set(DoubleSolenoid.Value.kReverse);
            printTestInfo("Solenoid going in reverse direction");
        } else if (button == LogitechF310.Button.X) {
            doubleSolenoid.set(DoubleSolenoid.Value.kOff);
        }
    }
}
