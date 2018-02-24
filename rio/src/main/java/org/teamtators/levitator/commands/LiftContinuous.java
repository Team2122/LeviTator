package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Lift;

public class LiftContinuous extends Command implements Configurable<LiftContinuous.Config> {

    private TatorRobot robot;
    private Lift lift;
    private double desiredHeight;
    private double desiredPivotAngle;
    private boolean locking;
    private double sweepTarget;
    private Timer sweepTimer = new Timer();
    private Config config;

    public LiftContinuous(TatorRobot robot) {
        super("LiftContinuous");
        this.robot = robot;
        lift = (robot.getSubsystems()).getLift();
        requires(lift);
    }

    @Override
    protected boolean step() {
        desiredHeight = lift.getDesiredHeight();
        desiredPivotAngle = lift.getDesiredPivotAngle();
        double centerAngle = lift.getAnglePreset(Lift.AnglePreset.CENTER);
        double currentAngle = lift.getCurrentPivotAngle();
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
                if (lift.isPivotLocked()) {
                    //Reset sweep target
                    logger.debug("Pivot locked");
                    sweepTarget = 0;
                } else if (sweepTimer.hasPeriodElapsed(config.sweepTimeoutSeconds)) {
                    //Reset sweep target
                    logger.warn("Pivot could not lock, timeout elapsed");
                    sweepTarget = 0;
                }
            }
        } else {
            lift.setPivotLockSolenoid(false);
            //If we want to go to center and we're within the range of locking
            if (desiredPivotAngle == centerAngle &&
                    (currentAngle > -config.startSweepAngle && currentAngle < config.startSweepAngle)) {
                logger.debug("Pivot moving center, will engage lock");
                //Start locking
                locking = true;
                sweepTarget = -Math.signum(currentAngle) * config.startSweepAngle;
                //Disable PID
                lift.setPivotControllerEnabled(false);
                //Start the timer
                sweepTimer.restart();
            }
            //If we want to go somewhere other than the center
            if (desiredPivotAngle != centerAngle) {
                //Retract the solenoid
                lift.setPivotLockSolenoid(false);
                //Enable the PID Controller
                lift.setPivotControllerEnabled(true);
            }
            //Set the pivot target angle to the desired angle
            lift.setTargetAngle(desiredPivotAngle);
        }
        //Set the lift target height to the desired height
        lift.setTargetHeight(desiredHeight);
        return false;
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public double pivotSweepPower;
        public double startSweepAngle;
        public double sweepTimeoutSeconds;
    }
}