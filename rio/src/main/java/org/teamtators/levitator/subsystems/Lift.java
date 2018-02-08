package org.teamtators.levitator.subsystems;

import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.config.*;
import org.teamtators.common.hw.AnalogPotentiometer;
import edu.wpi.first.wpilibj.Encoder;
import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.components.AnalogPotentiometerTest;
import org.teamtators.common.tester.components.DigitalSensorTest;
import org.teamtators.common.tester.components.EncoderTest;
import org.teamtators.common.tester.components.SpeedControllerTest;

public class Lift extends Subsystem implements Configurable<Lift.Config> {

    private SpeedController liftMotor;
    private DigitalSensor limitSensorTop;
    private DigitalSensor limitSensorBottom;
    private SpeedController pivotMotor;
    private AnalogPotentiometer pivotEncoder;
    private Encoder liftEncoder;

    private double desiredPivotAngle;
    private double desiredHeight;
    private HeightPreset desiredPresetHeight;
    private AnglePreset desiredAngle;

    private Config config;

    public Lift() {
        super("Lift");
        config.ticksPerInch = 360;
        config.angleOffset = 360;
    }

    /**
     * @return height in inches
     */
    public double getCurrentHeight() {
        return liftEncoder.getDistance() / config.ticksPerInch;
    }

    /**
     * @return height in inches
     */
    public double getDesiredHeight() {
        return desiredHeight;
    }

    /**
     * @param desiredHeight height in inches
     */
    public void setDesiredHeight(double desiredHeight) {
        this.desiredHeight = desiredHeight;
    }

    public void setDesiredHeightPreset(HeightPreset desiredHeight) {
        this.desiredPresetHeight = desiredHeight;
    }

    public double getCurrentPivotAngle() {
        return ((pivotEncoder.get() / 5.0) * 360) + config.angleOffset;
    }

    public double getDesiredPivotAngle() {
        return desiredPivotAngle;
    }

    public void setDesiredPivotAngle(double desiredAngle) {
        this.desiredPivotAngle = desiredAngle;
    }

    public void setDesiredAnglePreset(AnglePreset desiredPivotAngle) {
        this.desiredAngle = desiredPivotAngle;
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        this.liftMotor = config.liftMotor.create();
        this.liftEncoder = config.liftEncoder.create();
        this.limitSensorTop = config.limitSensorTop.create();
        this.limitSensorBottom = config.limitSensorBottom.create();
        this.pivotMotor = config.pivotMotor.create();
        this.pivotEncoder = config.pivotEncoder.create();
        this.liftEncoder = config.liftEncoder.create();
    }

    public enum HeightPreset {
        PICK,
        SWITCH,
        SCALE_LOW,
        SCALE_HIGH;
    }

    public enum AnglePreset {
        LEFT,
        CENTER,
        RIGHT;
    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup tests = super.createManualTests();
        /*tests.addTest(new SpeedControllerTest("liftMotor", liftMotor));
        tests.addTest(new DigitalSensorTest("limitSensorTop", limitSensorTop));
        tests.addTest(new DigitalSensorTest("limitSensorBottom", limitSensorBottom));
        tests.addTest(new SpeedControllerTest("pivotMotor", pivotMotor));
        tests.addTest(new AnalogPotentiometerTest("pivotEncoder", pivotEncoder));
        tests.addTest(new EncoderTest("liftEncoder", liftEncoder));
        */return tests;
    }

    public class Config {
        SpeedControllerConfig liftMotor;
        DigitalSensorConfig limitSensorTop;
        DigitalSensorConfig limitSensorBottom;
        SpeedControllerConfig pivotMotor;
        AnalogPoteniometerConfig pivotEncoder;
        EncoderConfig liftEncoder;

        private double ticksPerInch;
        private double angleOffset;
    }
}
