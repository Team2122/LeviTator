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
    private boolean locking;
    private double sweepTarget;
    private Timer sweepTimer = new Timer();
    private Config config;
    private BooleanSampler locked = new BooleanSampler(() -> pivot.isPivotLocked());
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
    }

    @Override
    protected boolean step() {
        boolean isTeleop = robot.getState() == RobotState.TELEOP;
        if (isTeleop) {
            updateSlider(operatorInterface.getSliderValue());
            updateKnob(operatorInterface.getPivotKnob() * 90);
        }
        updatePivot();
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

    private void updatePivot() {
        double centerAngle = pivot.getAnglePreset(Pivot.AnglePreset.CENTER);
        double currentAngle = pivot.getCurrentPivotAngle();
        double safePivotAngle = pivot.getSafePivotAngle(desiredPivotAngle);
        boolean isWithinLockAngle = currentAngle > -config.lockAngle && currentAngle < config.lockAngle;
        if ((safePivotAngle != centerAngle || !isWithinLockAngle) && locking) {
            locking = false;
            logger.debug("Moving away from center, disengaging lock");
        }
        if (locking) {
            //Extend the solenoid to lock
            pivot.setPivotLockSolenoid(true);
            if (sweepTarget == 0) {
                pivot.setPivotPower(0.0);
            } else {
                //Check if this is the first step of locking or we overshot our target
                if (sweepTarget > 0 ? (currentAngle >= sweepTarget) : (currentAngle <= sweepTarget)) {
                    logger.warn("Pivot sweep missed lock, sweeping other direction");
                    sweepTarget = -sweepTarget;
                }
                //Apply the configured power with a sign that is the same as our target (i.e. positive power moves right, increasing angle)
                pivot.setPivotPower(config.pivotSweepPower * Math.signum(sweepTarget));
                //If our solenoid is locked in or the timer ran out
            }
            if (sweepTarget != 0) {
                if (locked.get()) {
                    //Reset sweep target
                    logger.debug("Pivot locked");
                    sweepTarget = 0;
                } else if (sweepTimer.periodically(config.sweepTimeoutSeconds)) {
                    logger.warn("Pivot could not lock, timeout elapsed");
                }
            } else {
                if (!pivot.isPivotLocked()) {
                    sweepTarget = -Math.signum(currentAngle) * config.startSweepAngle;
                }
            }
        } else {
            //Retract the solenoid
            pivot.setPivotLockSolenoid(false);
            //If we want to go to center and we're within the range of locking
            if (safePivotAngle == centerAngle) {
                boolean isWithinSweepAngle = currentAngle > -config.startSweepAngle && currentAngle < config.startSweepAngle;
                if (isWithinSweepAngle) {
                    logger.debug("Pivot moving center, will engage lock");
                    //Start locking
                    locking = true;
                    sweepTarget = -Math.signum(currentAngle) * config.startSweepAngle;
                    //Disable PID
                    pivot.disablePivotController();
                    //Start the timer
                    sweepTimer.restart();
                } else {
                    pivot.enablePivotController();
                    pivot.setTargetAngle(0);
                }
            }
            //If we want to go somewhere other than the center
            else {
                //Enable the PID Controller
                pivot.enablePivotController();
                //Set the pivot target angle to the desired angle
                pivot.setTargetAngle(desiredPivotAngle);
            }
        }
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted);
        lift.disableLiftController();
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        locked.setPeriod(config.lockedPeriod);
    }

    public static class Config {
        public double pivotSweepPower;
        public double startSweepAngle;
        public double sweepTimeoutSeconds;
        public double lockedPeriod;
        public double sliderTolerance;
        public double sliderThreshold;
        public double knobTolerance;
        public double lockAngle;
    }
}
