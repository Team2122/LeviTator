package org.teamtators.common.drive;

import org.teamtators.common.math.Pose2d;
import org.teamtators.common.math.Rotation;

public class PursuitReport {
    public boolean isFinished;
    public double traveledDistance;
    public double remainingDistance;
    public Pose2d nearestPoint;
    public Pose2d lookaheadPoint;
    public double trackError;
    public Rotation yawError;
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
