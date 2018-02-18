package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
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
    private Config config;

    public LiftContinuous(TatorRobot robot) {
        super("LiftContinuous");
        this.robot = robot;
        lift = (robot.getSubsystems()).getLift();
        requires(lift);
    }

    @Override
    protected boolean step() {
        if (locking) {
            //Extend the solenoid to lock
            lift.setPivotLockSolenoid(true);
            //Check if this is the first step of locking or we overshot our target
            if (sweepTarget == 0 || sweepTarget > 0 ?
                    lift.getCurrentPivotAngle() > sweepTarget : lift.getCurrentPivotAngle() < sweepTarget) {
                //Set our target to the negative counterpart of our configured angle based on our current angle
                sweepTarget = -Math.signum(lift.getCurrentPivotAngle()) * config.startSweepAngle;
            }
            //Apply the configured power with a sign that is the same as our target (i.e. positive power moves right, increasing angle)
            lift.setPivotPower(config.pivotSweepPower * Math.signum(sweepTarget));
            //If our solenoid is locked in
            if (lift.isPivotLocked()) {
                //Reset sweep target
                sweepTarget = 0;
                //Stop locking
                locking = false;
            }
        } else {
            desiredHeight = lift.getDesiredHeight();
            desiredPivotAngle = lift.getDesiredPivotAngle();
            //If we want to go to center and we're within the range of locking
            if (desiredPivotAngle == lift.getAnglePreset(Lift.AnglePreset.CENTER) &&
                    (lift.getCurrentPivotAngle() > -config.startSweepAngle &&
                            lift.getCurrentPivotAngle() < config.startSweepAngle)) {
                //Start locking
                locking = true;
                //Disable PID
                lift.setPivotControllerEnabled(false);
            }
            //If we want to go somewhere other than the center
            if (desiredPivotAngle != lift.getAnglePreset(Lift.AnglePreset.CENTER)) {
                //Retract the solenoid
                lift.setPivotLockSolenoid(false);
                //Enable the PID Controller
                lift.setPivotControllerEnabled(true);
            }
            lift.setTargetHeight(desiredHeight);
            lift.setTargetAngle(desiredPivotAngle);
        }
        return false;
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public double pivotSweepPower;
        public double startSweepAngle;
    }
}
