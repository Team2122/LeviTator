package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.BooleanSampler;
import org.teamtators.common.control.Timer;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Lift;
import org.teamtators.levitator.subsystems.OperatorInterface;

public class LiftContinuous extends Command implements Configurable<LiftContinuous.Config> {

    private TatorRobot robot;
    private Lift lift;
    private double desiredHeight;
    private double desiredPivotAngle;
    private boolean locking;
    private double sweepTarget;
    private Timer sweepTimer = new Timer();
    private Config config;
    private BooleanSampler locked = new BooleanSampler(() -> lift.isPivotLocked());
    private OperatorInterface operatorInterface;

    public LiftContinuous(TatorRobot robot) {
        super("LiftContinuous");
        this.robot = robot;
        lift = (robot.getSubsystems()).getLift();
        operatorInterface = (robot.getSubsystems()).getOI();
        requires(lift);
    }

    @Override
    protected boolean step() {
        double sliderValue = operatorInterface.getSliderValue();
        if (lift.isAtHeight() && lift.isMovementInitiatedByCommand()) {
            if (Math.abs(lift.getCurrentHeight() - lift.toHeight(sliderValue)) < config.sliderTolerance) {
                logger.info("Clearing movement flag");
                lift.clearForceMovementFlag();
            }
        }
        desiredHeight = lift.getDesiredHeight();
        desiredPivotAngle = lift.getDesiredPivotAngle();
        double centerAngle = lift.getAnglePreset(Lift.AnglePreset.CENTER);
        double currentAngle = lift.getCurrentPivotAngle();
        boolean allowSlider = !lift.isMovementInitiatedByCommand();
        if (allowSlider && Math.abs(lift.getTargetHeight() - lift.toHeight(sliderValue)) < config.sliderThreshold) {
            lift.setDesiredHeight(lift.toHeight(sliderValue), false);
            desiredHeight = lift.getDesiredHeight();
        }
        if (desiredPivotAngle != centerAngle && locking) {
            locking = false;
            logger.debug("Moving away from center, disengaging lock");
        }
        if (locking) {
            //Extend the solenoid to lock
            lift.setPivotLockSolenoid(true);
            if (sweepTarget == 0) {
                lift.setPivotPower(0.0);
            } else {
                //Check if this is the first step of locking or we overshot our target
                if (sweepTarget > 0 ? (currentAngle >= sweepTarget) : (currentAngle <= sweepTarget)) {
                    logger.warn("Pivot sweep missed lock, sweeping other direction");
                    sweepTarget = -sweepTarget;
                }
                //Apply the configured power with a sign that is the same as our target (i.e. positive power moves right, increasing angle)
                lift.setPivotPower(config.pivotSweepPower * Math.signum(sweepTarget));
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
                if (!lift.isPivotLocked()) {
                    sweepTarget = -Math.signum(currentAngle) * config.startSweepAngle;
                }
            }
        } else {
            //Retract the solenoid
            lift.setPivotLockSolenoid(false);
            //If we want to go to center and we're within the range of locking
            if (desiredPivotAngle == centerAngle) {
                if (currentAngle > -config.startSweepAngle && currentAngle < config.startSweepAngle) {
                    logger.debug("Pivot moving center, will engage lock");
                    //Start locking
                    locking = true;
                    sweepTarget = -Math.signum(currentAngle) * config.startSweepAngle;
                    //Disable PID
                    lift.setPivotControllerEnabled(false);
                    //Start the timer
                    sweepTimer.restart();
                } else {
                    lift.setPivotControllerEnabled(true);
                    lift.setTargetAngle(0);
                }
            }
            //If we want to go somewhere other than the center
            else {
                //Enable the PID Controller
                lift.setPivotControllerEnabled(true);
                //Set the pivot target angle to the desired angle
                lift.setTargetAngle(desiredPivotAngle);
            }
        }
        //Set the lift target height to the desired height
        lift.setTargetHeight(desiredHeight);
        return false;
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
    }
}
