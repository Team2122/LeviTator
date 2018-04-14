package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.math.Epsilon;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.scheduler.RobotState;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Lift;
import org.teamtators.levitator.subsystems.OperatorInterface;
import org.teamtators.levitator.subsystems.Pivot;

public class LiftContinuous extends Command implements Configurable<LiftContinuous.Config> {

    private TatorRobot robot;
    private Lift lift;
    private Pivot pivot;
    private Config config;
    private OperatorInterface operatorInterface;

    private boolean sliderSafe = false;
    private boolean knobSafe = false;

    public LiftContinuous(TatorRobot robot) {
        super("LiftContinuous");
        this.robot = robot;
        lift = (robot.getSubsystems()).getLift();
        pivot = robot.getSubsystems().getPivot();
        operatorInterface = (robot.getSubsystems()).getOI();
        requires(lift);
    }

    @Override
    protected void initialize() {
        super.initialize();
        lift.enable();
        pivot.enable();
    }

    @Override
    public boolean step() {
        boolean isTeleop = robot.getState() == RobotState.TELEOP;
        if (isTeleop) {
            updateSlider(operatorInterface.getSliderHeight());
            updateKnob(operatorInterface.getPivotKnob());
        }
        return false;
    }

    private void updateKnob(double knobAngle) {
        if (pivot.isRotationForced() &&
                Math.abs(knobAngle - pivot.getDesiredPivotAngle()) < config.knobTolerance) {
            pivot.clearForceRotationFlag();
        }
        boolean allowKnob = !pivot.isRotationForced();
        double angleDelta = Math.abs(pivot.getDesiredPivotAngle() - knobAngle);
        if (allowKnob && angleDelta > 2.0) {
            pivot.setDesiredPivotAngle(knobAngle, false);
        }


        if (pivot.isManualOverride()) {
            if (!knobSafe && Epsilon.isEpsilonZero(knobAngle, 5)) {
                knobSafe = true;
            }
            if (knobSafe) {
                pivot.setPivotPower(operatorInterface.getPivotKnobRaw());
            }
        } else {
            knobSafe = false;
        }
    }

    private void updateSlider(double sliderHeight) {
        boolean atHeight = lift.isAtHeight();

        double heightDelta = Math.abs(lift.getDesiredHeight() - sliderHeight);
        if (atHeight && lift.isHeightForced()) {
            //logger.info("Abs {} Tolerance?: {}", heightDelta, config.sliderTolerance);
            if (heightDelta < config.sliderTolerance) {
                lift.clearForceHeightFlag();
            }
        }

        boolean allowSlider = !lift.isHeightForced();
        if (allowSlider && heightDelta > config.sliderThreshold) {
            lift.setDesiredHeight(sliderHeight, false);
        }

        if (lift.isManualOverride()) {
            double sliderValueRaw = operatorInterface.getSliderValueRaw();
            if (!sliderSafe && Epsilon.isEpsilonZero(sliderValueRaw, 0.05)) {
                sliderSafe = true;
            }
            if (sliderSafe) {
                lift.setLiftPower(sliderValueRaw);
            }
        } else {
            sliderSafe = false;
        }
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted);
        lift.disable();
        pivot.disable();
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public double sliderTolerance;
        public double sliderThreshold;
        public double knobTolerance;
    }
}
