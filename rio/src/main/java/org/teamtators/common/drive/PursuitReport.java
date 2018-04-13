package org.teamtators.common.drive;

import org.teamtators.common.math.Pose2d;
import org.teamtators.common.math.Rotation;

public class PursuitReport {
    public boolean isFinished = false;
    public double traveledDistance;
    public double remainingDistance;
    public Pose2d nearestPoint = Pose2d.zero();
    public Pose2d lookaheadPoint = Pose2d.zero();
    public double trackError;
    public Rotation yawError = Rotation.identity();
    public boolean updateProfile = false;
    public boolean isReverse;

    @Override
    public String toString() {
        return "PursuitReport{" +
                "isFinished=" + isFinished +
                ", traveledDistance=" + traveledDistance +
                ", remainingDistance=" + remainingDistance +
                ", nearestPoint=" + nearestPoint +
                ", lookaheadPoint=" + lookaheadPoint +
                ", trackError=" + trackError +
                ", yawError=" + yawError +
                '}';
    }
}
