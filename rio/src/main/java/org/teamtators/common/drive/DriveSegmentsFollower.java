package org.teamtators.common.drive;

import org.slf4j.profiler.Profiler;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.AbstractUpdatable;
import org.teamtators.common.control.PidController;
import org.teamtators.common.control.TrapezoidalProfileFollower;
import org.teamtators.common.datalogging.DataCollector;
import org.teamtators.common.datalogging.LogDataProvider;
import org.teamtators.common.math.Epsilon;
import org.teamtators.common.math.LinearInterpolationFunction;
import org.teamtators.common.math.Pose2d;
import org.teamtators.common.math.Twist2d;

import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

/**
 * @author Alex Mikhalev
 */
public class DriveSegmentsFollower extends AbstractUpdatable
        implements Configurable<DriveSegmentsFollower.Config> {
    private final TankDrive drive;
    private DoubleUnaryOperator lookAheadFunction;

    private TrapezoidalProfileFollower speedFollower;
    private DriveSegments segments = new DriveSegments();

    private int currentSegmentIdx;
    private double previousTraveled;
    private double totalLength;
    private PursuitReport report;
    private LookaheadReport lookaheadReport;
    private double speedPower;
    private LogDataProvider logDataProvider = new LogDataProvder();
    private Pose2d currentPose;
    private Twist2d twist = new Twist2d();
    private DriveOutputs driveOutputs = new DriveOutputs();
    private boolean logData;
    private Profiler profiler;
    private double lookahead;

    public DriveSegmentsFollower(TankDrive drive) {
        super("DriveSegmentsFollower");
        this.drive = drive;

        speedFollower = new TrapezoidalProfileFollower("DriveSegmentsFollower.speedFollower");
        speedFollower.setPositionProvider(this::getTraveledDistance);
        speedFollower.setVelocityProvider(() ->
                (report != null && report.isReverse) ? -drive.getCenterRate() : drive.getCenterRate());
        speedFollower.setOutputConsumer(this::setSpeedPower);
        speedFollower.setOnTargetPredicate(t -> false);

        reset();
    }

    private void setSpeedPower(double speedPower) {
        this.speedPower = speedPower;
    }

    public void reset() {
        currentSegmentIdx = -1;
        previousTraveled = 0.0;
        totalLength = segments.getArcLength();
        report = null;
        currentPose = drive.getPose();
        twist = new Twist2d();
        setSpeedPower(0.0);
    }

    public DriveSegments getSegments() {
        return segments;
    }

    public void setSegments(DriveSegments segments) {
        this.segments = segments;
        reset();
    }

    public DoubleUnaryOperator getLookAheadFunction() {
        return lookAheadFunction;
    }

    public void setLookAheadFunction(DoubleUnaryOperator lookAheadFunction) {
        this.lookAheadFunction = lookAheadFunction;
    }

    public TrapezoidalProfileFollower getSpeedFollower() {
        return speedFollower;
    }

    public double getMaxAcceleration() {
        return speedFollower.getMaxAcceleration();
    }

    public void setMaxAcceleration(double maxAcceleration) {
        speedFollower.setMaxAcceleration(maxAcceleration);
    }

    private void updateProfile() {
        if (!hasSegment()) {
            logger.warn("can not update profile when does not have segment");
            return;
        }
        DriveSegment seg = getCurrentSegment();
        speedFollower.setTravelVelocity(seg.getTravelSpeed());
        speedFollower.setEndVelocity(seg.getEndSpeed());
        speedFollower.moveDistance(lookaheadReport.remainingDistance);
        if (!speedFollower.isRunning()) {
            speedFollower.start();
        }
//        logger.trace("driving segment \n{} with profile \n{}", seg, speedFollower.getCalculator().getProfile());
    }

    void updatePursuitReport(Pose2d currentPose, double centerWheelRate) {
        if (isFinished()) {
            return;
        }
        PursuitReport report = new PursuitReport();
        if (currentSegmentIdx < 0) {
            currentSegmentIdx++;
            report.updateProfile = true;
        }
        if (!hasSegment()) {
            report.isFinished = true;
            return;
        }
        lookahead = lookAheadFunction.applyAsDouble(Math.abs(centerWheelRate));
        DriveSegment currentSegment = getCurrentSegment();
        Pose2d pose = currentSegment.isReverse() ? currentPose.invertYaw() : currentPose;
        lookaheadReport = currentSegment.getLookaheadReport(pose, lookahead);
        while (Epsilon.isEpsilonNegative(lookaheadReport.remainingDistance)) {
            currentSegmentIdx++;
            if (!hasSegment()) {
                report.isFinished = true;
                break;
            } else {
                previousTraveled += lookaheadReport.traveledDistance;
                currentSegment = getCurrentSegment();
                pose = currentSegment.isReverse() ? currentPose.invertYaw() : currentPose;
                lookaheadReport = currentSegment.getLookaheadReport(pose, lookahead);
                report.updateProfile = true;
            }
        }
        report.traveledDistance = previousTraveled + lookaheadReport.traveledDistance;
        report.remainingDistance = totalLength - report.traveledDistance;
        report.nearestPoint = lookaheadReport.nearestPoint;
        report.lookaheadPoint = lookaheadReport.lookaheadPoint;
        report.trackError = lookaheadReport.trackError;
        report.yawError = lookaheadReport.yawError;
        report.isReverse = currentSegment.isReverse();
        if (lookaheadReport.lookaheadRemainingDistance < 0 && hasNextSegment()) {
            DriveSegment nextSegment = getNextSegment();
            double nextLookahead = -lookaheadReport.lookaheadRemainingDistance;
            report.lookaheadPoint = nextSegment
                    .getLookAhead(nextSegment.getStartPose(), nextLookahead);
        }
        this.report = report;
    }

    private DriveSegment getNextSegment() {
        return segments.getSegments().get(currentSegmentIdx + 1);
    }

    private DriveSegment getCurrentSegment() {
        return segments.getSegments().get(currentSegmentIdx);
    }

    private boolean hasSegment() {
        return currentSegmentIdx < getSegments().getSegments().size();
    }

    private boolean hasNextSegment() {
        return currentSegmentIdx + 1 < getSegments().getSegments().size();
    }

    private double getTraveledDistance() {
        if (report == null) {
            return 0.0;
        }
        return report.traveledDistance;
    }

    @Override
    public synchronized void start() {
        if (!running) {
            running = true;
            reset();
            speedFollower.reset();
//            leftController.start();
//            rightController.start();
            if (logData) {
                DataCollector.getDataCollector().startProvider(logDataProvider);
            }
        }
    }

    @Override
    public synchronized void stop() {
        super.stop();
        speedFollower.stop();
//        leftController.stop();
//        rightController.stop();
        DataCollector.getDataCollector().stopProvider(logDataProvider);
    }

    public boolean isFinished() {
        return report != null && report.isFinished;
    }

    public boolean isOnTarget() {
        return report != null && report.remainingDistance < 0.5;
    }

    @Override
    protected void doUpdate(double delta) {
        if (profiler == null) {
            profiler = new Profiler(getName());
        }
        profiler.start("checkFinished");
        if (isFinished()) {
            drive.stop();
            stop();
            profiler.stop();
            return;
        }
        profiler.start("getPose");
        currentPose = drive.getPose();
        double centerWheelRate = drive.getCenterRate();
        profiler.start("updatePursuitReport");
        updatePursuitReport(currentPose, centerWheelRate);
        profiler.start("updateProfile");
        if (!isFinished() && report.updateProfile) {
            updateProfile();
        }
        profiler.start("twist2d");
        twist = Twist2d.fromTangentArc(currentPose, report.lookaheadPoint.getTranslation());
        if (report.isReverse) {
            twist = new Twist2d(twist.getDeltaYaw(), -twist.getDeltaX());
        }
        profiler.start("speedFollower");
        speedFollower.update(delta);
        profiler.start("setOutputs");

        driveOutputs = drive.getTankKinematics().calculateOutputs(twist, speedPower);
//        driveOutputs = driveOutputs.normalize();
//        drive.setPowers(driveOutputs);
        driveOutputs = driveOutputs.normalize(drive.getMaxSpeed());
        drive.setSpeeds(driveOutputs);
        if (isOnTarget()) {
            report.isFinished = true;
        }
        profiler.stop();
    }

    @Override
    public Profiler getProfiler() {
        Profiler profiler = this.profiler;
        this.profiler = null;
        return profiler;
    }

    @Override
    public void setProfiler(Profiler profiler) {
        this.profiler = profiler;
    }

    @Override
    public boolean hasProfiler() {
        return true;
    }

    @Override
    public void configure(Config config) {
        setLookAheadFunction(config.lookAhead);
        speedFollower.configure(config.speedFollower);
        this.logData = config.logData;
    }

    public PursuitReport getReport() {
        return report;
    }

    public static class Config {
        public LinearInterpolationFunction lookAhead;
        public TrapezoidalProfileFollower.Config speedFollower;
        public boolean logData = false;
    }

    private class LogDataProvder implements LogDataProvider {
        @Override
        public String getName() {
            return DriveSegmentsFollower.this.getName();
        }

        @Override
        public List<Object> getKeys() {
            return Arrays.asList("remainingDistance", "currentPoseX", "currentPoseY", "currentPoseYaw", "nearestPointX", "nearestPointY", "nearestPointYaw",
                    "lookaheadDistance", "twistdx", "twistdyaw", "speedPower", "driveOutputLeft", "driveOutputRight");
        }

        @Override
        public List<Object> getValues() {
            PursuitReport report = DriveSegmentsFollower.this.report;
            if (report == null) {
                report = new PursuitReport();
            }
            return Arrays.asList(report.remainingDistance, currentPose.getX(), currentPose.getY(), currentPose.getYaw(), report.nearestPoint.getX(),
                    report.nearestPoint.getY(), report.nearestPoint.getYaw(), lookahead,
                    twist.getDeltaX(), twist.getDeltaYaw(), speedPower, driveOutputs.getLeft(), driveOutputs.getRight());
        }
    }
}
