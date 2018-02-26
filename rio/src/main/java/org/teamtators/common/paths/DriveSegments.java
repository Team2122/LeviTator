package org.teamtators.common.paths;

import java.util.ArrayList;
import java.util.List;

public class DriveSegments {
    private List<DriveSegment> segments = new ArrayList<>();

    public List<DriveSegment> getSegments() {
        return segments;
    }

    public void addSegment(DriveSegment segment) {
        segments.add(segment);
    }
}
