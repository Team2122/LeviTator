package org.teamtators.common.drive;

import org.teamtators.common.math.Pose2d;
import org.teamtators.common.math.Translation2d;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.stream.Collectors;

/**
 * @author Alex Mikhalev
 */
public class DrivePathTest {
    public static DrivePath getTestPath() {
        DrivePath path = new DrivePath();
        DrivePath.Point point = new DrivePath.Point();
        point.setTranslation(new Translation2d(0, 0));
        point.setRadius(12);
        point.setSpeed(30);
        point.setArcSpeed(20);
        point.setReverse(false);
        path.addPoint(point.copy());
        point.setTranslation(new Translation2d(0, 30));
        path.addPoint(point.copy());
        point.setTranslation(new Translation2d(30, 30));
        path.addPoint(point.copy());
        point.setTranslation(new Translation2d(30, 70));
        path.addPoint(point.copy());
        point.setTranslation(new Translation2d(-30, 70));
        path.addPoint(point.copy());
        point.setTranslation(new Translation2d(-30, 30));
        path.addPoint(point.copy());
        point.setTranslation(new Translation2d(0, 30));
        path.addPoint(point.copy());
        point.setTranslation(new Translation2d(0, 0));
        path.addPoint(point.copy());
        return path;
    }

    @Test
    public void testToSegments() throws Exception {
        DrivePath path = getTestPath();

        DriveSegments segments = path.toSegments();
        System.out.println(segments.getSegments().stream().map(Object::toString).collect(Collectors.joining("\n")));
        Pose2d lastPose = null;
        DriveSegment lastSegment = null;
        for (DriveSegment segment : segments.getSegments()) {
            if (lastPose != null) {
                Assert.assertTrue(lastPose.epsilonEquals(segment.getStartPose()),
                        "Invalid path segment connection: \n" + lastSegment + "\n==>\n" + segment);
            }
            lastPose = segment.getEndPose();
            lastSegment = segment;
        }
    }
}