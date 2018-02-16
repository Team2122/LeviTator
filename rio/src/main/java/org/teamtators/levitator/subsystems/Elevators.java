package org.teamtators.levitator.subsystems;

import edu.wpi.first.wpilibj.Solenoid;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.SolenoidConfig;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.components.SolenoidTest;

public class Elevators extends Subsystem implements Configurable<Elevators.Config> {

    private Solenoid rightElevatorSolenoid;
    private Solenoid leftElevatorSolenoid;
    private Solenoid releaser;

    private boolean isDeployed;

    private Config config;

    public Elevators() {
        super("Elevators");
    }

    public void reset() {
        rightElevatorSolenoid.set(false);
        leftElevatorSolenoid.set(false);
        releaser.set(false);
        isDeployed = false;
    }

    public void liftRightElevator() {
        if(isSafeToLiftElevators() == true) {
            rightElevatorSolenoid.set(true);
        }
    }

    public void liftLeftElevator() {
        if(isSafeToLiftElevators() == true) {
            leftElevatorSolenoid.set(true);
        }
    }

    public void deployElevators() {
        releaser.set(true);
        isDeployed = true;
    }

    public boolean isSafeToLiftElevators() {
        if(isDeployed == true) {
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup tests = super.createManualTests();
        tests.addTest(new SolenoidTest("rightElevatorSolenoid", rightElevatorSolenoid));
        tests.addTest(new SolenoidTest("leftElevatorSolenoid", leftElevatorSolenoid));
        tests.addTest(new SolenoidTest("releaser", releaser));
        return tests;
    }

    public void configure(Config config) {
        this.config = config;
        this.rightElevatorSolenoid = config.rightElevatorSolenoid.create();
        this.leftElevatorSolenoid = config.leftElevatorSolenoid.create();
        this.releaser = config.releaser.create();
        reset();
    }

    public static class Config{
        public SolenoidConfig rightElevatorSolenoid;
        public SolenoidConfig leftElevatorSolenoid;
        public SolenoidConfig releaser;
    }
}
