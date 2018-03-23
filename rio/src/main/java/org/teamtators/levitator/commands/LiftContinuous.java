package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.BooleanSampler;
import org.teamtators.common.control.Timer;
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
    private double desiredHeight;
    private double desiredPivotAngle;
    private Config config;
    private OperatorInterface operatorInterface;

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
        lift.enableLiftController();
        pivot.enablePivotController();
    }

    @Override
    protected boolean step() {
        boolean isTeleop = robot.getState() == RobotState.TELEOP;
        if (isTeleop) {
            updateSlider(operatorInterface.getSliderValue());
            updateKnob(operatorInterface.getPivotKnob() * 90);
        }
        return false;
    }

    private void updateKnob(double knobAngle) {
        if (Math.abs(knobAngle) < 4) {
            knobAngle = 0;
        }
        if(pivot.isRotationForced() &&
                Math.abs(knobAngle - pivot.getDesiredPivotAngle()) < config.knobTolerance) {
            pivot.clearForceRotationFlag();
        }
        boolean allowKnob = !pivot.isRotationForced();
        if (allowKnob) {
            pivot.setDesiredPivotAngle(knobAngle, false);
            desiredPivotAngle = pivot.getDesiredPivotAngle();
        }
    }

    private void updateSlider(double sliderValue) {
        boolean atHeight = lift.isAtHeight();

        if (atHeight && lift.isHeightForced()) {
            double knobDelta = Math.abs(lift.getDesiredHeight() - lift.sliderToHeight(sliderValue));
            //logger.info("Abs {} Tolerance?: {}", knobDelta, config.sliderTolerance);
            if (knobDelta < config.sliderTolerance) {
                lift.clearForceHeightFlag();
            }
        }

        desiredHeight = lift.getDesiredHeight();
        desiredPivotAngle = pivot.getDesiredPivotAngle();
        boolean allowSlider = !lift.isHeightForced();
        double sliderDelta = Math.abs(lift.getTargetHeight() - lift.sliderToHeight(sliderValue));
        if (allowSlider && sliderDelta > config.sliderThreshold) {
            lift.setDesiredHeight(lift.sliderToHeight(sliderValue), false);
            desiredHeight = lift.getDesiredHeight();
        }
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted);
        lift.disableLiftController();
        pivot.disablePivotController();
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
