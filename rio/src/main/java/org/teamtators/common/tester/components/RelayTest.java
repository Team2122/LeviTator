package org.teamtators.common.tester.components;

import edu.wpi.first.wpilibj.Relay;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.tester.ManualTest;

public class RelayTest extends ManualTest {
    private final Relay relay;

    public RelayTest(String name, Relay relay) {
        super(name);
        this.relay = relay;
    }

    @Override
    public void start() {
        printTestInstructions("Testing relay " + getName() + ". Press A for off, B for forward, Y for reverse, X for on");
    }

    @Override
    public void onButtonDown(LogitechF310.Button button) {
        switch (button) {
            case A:
                printTestInfo("Relay Off");
                relay.set(Relay.Value.kOff);
                break;
            case B:
                printTestInfo("Relay Forward");
                relay.set(Relay.Value.kForward);
                break;
            case Y:
                printTestInfo("Relay Reverse");
                relay.set(Relay.Value.kReverse);
                break;
            case X:
                printTestInfo("Relay On");
                relay.set(Relay.Value.kOn);
                break;
        }
    }

    @Override
    public void stop() {
        relay.set(Relay.Value.kOff);
    }
}
