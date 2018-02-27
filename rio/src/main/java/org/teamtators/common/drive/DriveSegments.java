package org.teamtators.common.drive;

import org.teamtators.common.math.Pose2d;

import java.util.ArrayList;
import java.util.List;

public class DriveSegments {
    private List<DriveSegment> segments = new ArrayList<>();
    private int currentSegment;

    public List<DriveSegment> getSegments() {
        return segments;
    }

    public void addSegment(DriveSegment segment) {
        segments.add(segment);
    }

    public void reset() {
        currentSegment = 0;
    }

    public PursuitReport getPursuitReport(Pose2d robotPose) {

    }
}
