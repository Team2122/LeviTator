package org.teamtators.levitator.subsystems;

import edu.wpi.first.wpilibj.Sendable;
import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.config.*;
import org.teamtators.common.config.helpers.*;
import org.teamtators.common.control.PidController;
import org.teamtators.common.hw.AnalogPotentiometer;
import edu.wpi.first.wpilibj.Encoder;
import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.hw.SpeedControllerGroup;
import org.teamtators.common.scheduler.RobotState;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.components.*;

public class Lift extends Subsystem implements Configurable<Lift.Config> {

    private SpeedControllerGroup liftMotor;
    private Encoder liftEncoder;
    private DigitalSensor limitSensorTop;
    private DigitalSensor limitSensorBottom;
    private SpeedController pivotMotor;
    private AnalogPotentiometer pivotEncoder;

    private double desiredPivotAngle;
    private double desiredHeight;

    private PidController pivotController;

    private Config config;

    public Lift() {
        super("Lift");

        pivotController = new PidController("pivotController");
        pivotController.setInputProvider(this::getCurrentPivotAngle);
        pivotController.setOutputConsumer(this::setPivotPower);
    }

    /**
     * @return height in inches
     */
    public double getCurrentHeight() {
        return liftEncoder.getDistance();
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
        setDesiredHeight(getHeightPreset(desiredHeight));
    }

    public double getHeightPreset(HeightPreset heightPreset) {
        double heightValue = 0.0;
        switch (heightPreset) {
            case PICK:
                heightValue = config.heightPresetPick;
                break;
            case SWITCH:
                heightValue = config.heightPresetSwitch;
                break;
            case SCALE_LOW:
                heightValue = config.heightPresetScaleLow;
                break;
            case SCALE_HIGH:
                heightValue = config.heightPresetScaleHigh;
                break;
            case HOME:
                heightValue = config.heightPresetHome;
        }
        return heightValue;
    }

    public double getCurrentPivotAngle() {
        return pivotEncoder.get();
    }

    public double getDesiredPivotAngle() {
        return desiredPivotAngle;
    }

    public void setDesiredPivotAngle(double desiredAngle) {
        this.desiredPivotAngle = desiredAngle;
    }

    public void setDesiredAnglePreset(AnglePreset desiredPivotAngle) {
        setDesiredPivotAngle(getAnglePreset(desiredPivotAngle));
    }

    public double getAnglePreset(AnglePreset anglePreset) {
        double angleValue = 0.0;
        switch (anglePreset) {
            case LEFT:
                angleValue = config.anglePresetLeft;
                break;
            case RIGHT:
                angleValue = config.anglePresetRight;
                break;
            case CENTER:
                angleValue = config.anglePresetCenter;
                break;
        }
        return angleValue;
    }

    public boolean isAtBottomLimit() {
        return limitSensorBottom.get();
    }

    public boolean isAtTopLimit() {
        return !limitSensorTop.get();
    }

    public void setLiftPower(double liftPower) {
        //limit to max zero if max height is triggered
        if (isAtTopLimit() && liftPower > 0.0) {
            liftPower = 0.0;
        }
        if (isAtBottomLimit() && liftPower < 0.0) {
            liftPower = 0.0;
        }
        liftMotor.set(liftPower);
    }

    public void setPivotPower(double pivotPower) {
        pivotMotor.set(pivotPower);
    }

    public PidController getPivotController() {
        return pivotController;
    }

    @Override
    public void onEnterRobotState(RobotState state) {
        switch (state) {
            case AUTONOMOUS:
            case TELEOP:
                pivotController.start();
                pivotController.setSetpoint(0.0);
                break;
            case DISABLED:
                pivotController.stop();
                break;
        }
    }

    public enum HeightPreset {
        PICK,
        SWITCH,
        SCALE_LOW,
        SCALE_HIGH,
        HOME;
    }

    public enum AnglePreset {
        LEFT,
        CENTER,
        RIGHT;
    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup tests = super.createManualTests();
        tests.addTest(new SpeedControllerTest("liftMotor", liftMotor));
        tests.addTest(new EncoderTest("liftEncoder", liftEncoder));
        tests.addTest(new DigitalSensorTest("limitSensorTop", limitSensorTop));
        tests.addTest(new DigitalSensorTest("limitSensorBottom", limitSensorBottom));
        tests.addTest(new SpeedControllerTest("pivotMotor", pivotMotor));
        tests.addTest(new AnalogPotentiometerTest("pivotEncoder", pivotEncoder));

        tests.addTest(new ControllerTest(pivotController));

        return tests;
    }

    @Override
    public void configure(Config config) {
        this.liftMotor = config.liftMotor.create();
        this.liftEncoder = config.liftEncoder.create();
        this.limitSensorTop = config.limitSensorTop.create();
        this.limitSensorBottom = config.limitSensorBottom.create();
        this.pivotMotor = config.pivotMotor.create();
        this.pivotEncoder = config.pivotEncoder.create();

        this.pivotController.configure(config.pivotController);

        liftMotor.setName("Lift", "liftMotor");
        liftEncoder.setName("Lift", "liftEncoder");
        limitSensorTop.setName("Lift", "limitSensorTop");
        limitSensorBottom.setName("Lift", "limitSensorBottom");
        ((Sendable) pivotMotor).setName("Lift", "pivotMotor");
        pivotEncoder.setName("Lift", "pivotEncoder");
    }

    public static class Config {
        public SpeedControllerGroupConfig liftMotor;
        public EncoderConfig liftEncoder;
        public DigitalSensorConfig limitSensorTop;
        public DigitalSensorConfig limitSensorBottom;
        public SpeedControllerConfig pivotMotor;
        public AnalogPotentiometerConfig pivotEncoder;

        public PidController.Config pivotController;

        public double anglePresetLeft;
        public double anglePresetCenter;
        public double anglePresetRight;

        public double heightPresetPick;
        public double heightPresetSwitch;
        public double heightPresetScaleLow;
        public double heightPresetScaleHigh;
        public double heightPresetHome;

    }
}
