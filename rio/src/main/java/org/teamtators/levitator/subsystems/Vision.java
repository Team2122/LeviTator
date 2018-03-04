package org.teamtators.levitator.subsystems;

import edu.wpi.cscore.*;
import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.Deconfigurable;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.math.Polynomial3;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTest;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.levitator.TatorRobot;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Vision extends Subsystem implements Configurable<Vision.Config>, Deconfigurable {
    public static final Scalar REJECTED_CONTOUR_COLOR = new Scalar(255, 0, 0);
    public static final Scalar FILTERED_CONTOUR_COLOR = new Scalar(0, 255, 0);
    private Drive drive;
    private TatorRobot robot;
    private Config config;
    private SendableChooser<DisplayMode> displayModeChooser;
    private AtomicReference<DetectedObject> lastDetectedObject = new AtomicReference<>(new DetectedObject());
    private Thread processing;
    private MjpegServer server;
    private CameraServer cs;
    private UsbCamera usbCamera;
    private CvSink cvSink;
    private CvSource outputStream;

    public Vision(Drive drive) {
        super("Vision");
        cs = CameraServer.getInstance();
        this.drive = drive;
        displayModeChooser = new SendableChooser<>();
        displayModeChooser.addObject("Source", DisplayMode.Source);
        displayModeChooser.addObject("HSV", DisplayMode.HSV);
        displayModeChooser.addObject("Threshold", DisplayMode.Threshold);
        displayModeChooser.addDefault("AllContours", DisplayMode.AllContours);
        displayModeChooser.addObject("FilteredContours", DisplayMode.FilteredContours);
        displayModeChooser.setName("Vision", "displayMode");
        SmartDashboard.putData(displayModeChooser);
    }

    public void setupCameraThread() {
        killThread();
        processing = new Thread(this::processingThread, "TatorVision2Processing");
        processing.start();
    }

    private void processingThread() {
        if (usbCamera == null) {
            UsbCameraInfo[] usbCameras = getCameras();
            if (usbCameras.length == 0) {
                logger.warn("No USB webcam!");
                return;
            }
            logger.debug("Starting vision thread with source: " + usbCameras[0].name);
            usbCamera = cs.startAutomaticCapture("USBWebCam_Pick", usbCameras[0].dev);
            cvSink = cs.getVideo();
            outputStream = cs.putVideo("Vision_Pick", config.width, config.height);
            server = cs.addServer("CameraServer2122");
        } else {
            logger.debug("Starting vision thread with existing source");
        }

        usbCamera.setResolution(config.width, config.height);
//        usbCamera.setExposureManual(config.exposure);
        usbCamera.setExposureAuto();

        for (VideoProperty property : usbCamera.enumerateProperties()) {
            if (property.getName().equalsIgnoreCase("contrast")) {
                property.set(config.contrast);
            }
            if (property.getName().equalsIgnoreCase("saturation")) {
                property.set(config.saturation);
            }
        }

        //return;

        VideoProperty displayModeProp = outputStream.createProperty("displayMode", VideoProperty.Kind.kEnum, 0, 4, 1, 0, 0);
        DisplayMode[] displayModeValues = DisplayMode.values();
        String[] displayModeOpts = new String[displayModeValues.length];
        int i = 0;
        for (DisplayMode value : displayModeValues) {
            displayModeOpts[i++] = value.toString();
        }
        outputStream.SetEnumPropertyChoices(displayModeProp, displayModeOpts);

        Scalar lowerThreshold = new Scalar(config.lowerThreshold);
        Scalar upperThreshold = new Scalar(config.upperThreshold);
        VideoProperty minH = outputStream.createIntegerProperty("minH", 0, 255, 1, 0,
                (int) lowerThreshold.val[0]);
        VideoProperty minS = outputStream.createIntegerProperty("minS", 0, 255, 1, 0,
                (int) lowerThreshold.val[1]);
        VideoProperty minV = outputStream.createIntegerProperty("minV", 0, 255, 1, 0,
                (int) lowerThreshold.val[2]);
        VideoProperty maxH = outputStream.createIntegerProperty("maxH", 0, 255, 1, 255,
                (int) upperThreshold.val[0]);
        VideoProperty maxS = outputStream.createIntegerProperty("maxS", 0, 255, 1, 255,
                (int) upperThreshold.val[1]);
        VideoProperty maxV = outputStream.createIntegerProperty("maxV", 0, 255, 1, 255,
                (int) upperThreshold.val[2]);
        VideoProperty minWidth = outputStream.createIntegerProperty("minWidth", 0, config.width, 1, 0,
                (int) config.minWidth);
        VideoProperty maxWidth = outputStream.createIntegerProperty("maxWidth", 0, config.width, 1, config.width,
                (int) config.maxWidth);
        VideoProperty minHeight = outputStream.createIntegerProperty("minHeight", 0, config.height, 1, 0,
                (int) config.minHeight);
        VideoProperty maxHeight = outputStream.createIntegerProperty("maxHeight", 0, config.height, 1, config.height,
                (int) config.maxHeight);

        Mat source = Mat.zeros(config.width, config.height, CvType.CV_8UC3);
        Mat hsv = Mat.zeros(config.width, config.height, CvType.CV_8UC3);
        Mat threshold = Mat.zeros(config.width, config.height, CvType.CV_8UC1);
        Mat hierarchy = new Mat();
        Mat output = Mat.zeros(config.width, config.height, CvType.CV_8UC3);
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        List<ContourInfo> filteredContours;
        while (!Thread.interrupted()) {
            contours.clear();
            long ret = cvSink.grabFrame(source);
            if (ret == 0) {
                logger.warn("frame not grabbed: " + cvSink.getError());
                continue;
            }
            DisplayMode displayMode = displayModeValues[displayModeProp.get()];
            if (displayMode == DisplayMode.Source || displayMode == DisplayMode.AllContours
                    || displayMode == DisplayMode.FilteredContours) {
                source.copyTo(output);
            }
            Imgproc.cvtColor(source, hsv, Imgproc.COLOR_BGR2HSV);
            if (displayMode == DisplayMode.HSV) {
                hsv.copyTo(output);
            }
            lowerThreshold.val[0] = minH.get();
            lowerThreshold.val[1] = minS.get();
            lowerThreshold.val[2] = minV.get();
            upperThreshold.val[0] = maxH.get();
            upperThreshold.val[1] = maxS.get();
            upperThreshold.val[2] = maxV.get();
            Core.inRange(hsv, lowerThreshold, upperThreshold, threshold);
            if (displayMode == DisplayMode.Threshold) {
                threshold.copyTo(output);
            }
            Imgproc.findContours(threshold, contours, hierarchy, Imgproc.RETR_LIST,
                    Imgproc.CHAIN_APPROX_TC89_L1);
    /*        List<MatOfPoint> hulls = new ArrayList<>(contours.size());
            for (int idx = 0; idx < contours.size(); idx++) {
                MatOfInt convexHull = new MatOfInt();
                Imgproc.convexHull(contours.get(i), convexHull, false);
                hulls.set(i, convexHull);
            }*/
            if (displayMode == DisplayMode.AllContours) {
                Imgproc.drawContours(output, contours, -1, REJECTED_CONTOUR_COLOR);
            }
            filteredContours = contours.stream()
                    .map(ContourInfo::new)
                    .filter(contour -> {
                        double width = contour.size.width;
                        double height = contour.size.height;
                        return (width >= minWidth.get() && width <= maxWidth.get())
                                && (height >= minHeight.get() && height <= maxHeight.get());
                    })
                    .sorted(Comparator.comparingDouble((ContourInfo a) -> Math.abs(a.x)))
                    .collect(Collectors.toList());
            if (displayMode == DisplayMode.FilteredContours || displayMode == DisplayMode.AllContours) {
                List<MatOfPoint> filteredRawContours = filteredContours.stream().map(ContourInfo::getContour).collect(Collectors.toList());
                Imgproc.drawContours(output, filteredRawContours, -1, FILTERED_CONTOUR_COLOR);
            }
            if (filteredContours.size() == 0) {
                lastDetectedObject.set(new DetectedObject()); //null output, essentially
            } else {
                ContourInfo info = filteredContours.get(0);
                double x = ((info.moments.m10 / info.moments.m00) * 2.0 / config.width) - 1.0;
                double y = ((info.moments.m01 / info.moments.m00) * 2.0 / config.height) - 1.0;
                double width = info.boundingRect.width / config.width;
                double height = info.boundingRect.height / config.height;
                double area = info.area;
                if (displayMode == DisplayMode.FilteredContours || displayMode == DisplayMode.AllContours) {
                    Imgproc.drawContours(output, Collections.singletonList(info.contour),
                            -1, FILTERED_CONTOUR_COLOR, 2);
                    Imgproc.drawMarker(output, new Point(x, y), FILTERED_CONTOUR_COLOR);
                }
                lastDetectedObject.set(new DetectedObject(x, y, area, width, height));
            }
            outputStream.putFrame(output);
        }
//        usbCamera.free();
//        cvSink.free();
//        outputStream.free();
//        server.free();
        source.release();
        hsv.release();
        threshold.release();
        hierarchy.release();
        output.release();
    }

    private UsbCameraInfo[] getCameras() {
        UsbCameraInfo[] usbCameras = UsbCamera.enumerateUsbCameras();
        logger.info("CameraServer reports {} usb cameras", usbCameras.length);
        return usbCameras;
    }

    public void configure(Config config) {
        this.config = config;
        setupCameraThread();
    }

    public void deconfigure() {
        killThread();
    }

    public Double getNewRobotAngle(DetectedObject output) {
        Double yawOffset = getYawOffset(output);
        if (yawOffset == null) {
            return null;
        }
        return yawOffset + drive.getYawAngle();
    }

    public Double getYawOffset(DetectedObject detectedObject) {
        if (detectedObject == null || detectedObject.x == null) return null;
        return (.5 * detectedObject.x * config.fovX) + config.yawOffset;
    }

    public Double getLastYawOffset() {
        return getYawOffset(getLastDetectedObject());
    }

    public DetectedObject getLastDetectedObject() {
        return lastDetectedObject.get();
    }

    public Double getDistance(DetectedObject output) {
        if (output == null || output.y == null) {
            return null;
        }
        //noinspection SuspiciousNameCombination
        return config.distanceCalculator.calculate(output.y);
    }

    public Double getLastDistance() {
        return getDistance(getLastDetectedObject());
    }

    public void killThread() {
        if (processing != null) {
            if (!processing.isInterrupted()) {
                processing.interrupt();
            }
            try {
                processing.join();
            } catch (InterruptedException ignored) {
            }
        }
    }

    public enum DisplayMode {
        Source, HSV, Threshold, AllContours, FilteredContours
    }

    public static class Config {
        public int width;
        public int height;

        public int exposure;
        public int contrast;
        public int saturation;

        public double[] lowerThreshold;
        public double[] upperThreshold;

        public double minWidth;
        public double maxWidth;
        public double minHeight;
        public double maxHeight;
        public double minArea;
        public double maxArea;

        public double fovX;
        public double yawOffset;
        public Polynomial3 distanceCalculator;
    }

    private class ContourInfo {
        public MatOfPoint contour;
        public Rect boundingRect;
        public Size size;
        public double area;
        public Moments moments;
        public double x;
        public double y;

        public ContourInfo(MatOfPoint contour) {
            this.contour = contour;
            this.boundingRect = Imgproc.boundingRect(contour);
            this.size = new Size(this.boundingRect.width, this.boundingRect.height);
            this.area = Imgproc.contourArea(contour);
            this.moments = Imgproc.moments(contour);
            x = ((moments.m10 / moments.m00) * 2.0 / config.width) - 1.0;
            y = ((moments.m01 / moments.m00) * 2.0 / config.height) - 1.0;
        }

        public MatOfPoint getContour() {
            return contour;
        }
    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup tests = super.createManualTests();
        tests.addTest(new VisionTest());
        return tests;
    }

    private class VisionTest extends ManualTest {
        public VisionTest() {
            super("Vision");
        }

        @Override
        public void start() {
            logger.info("Press A to get last vision data");
        }

        @Override
        public void onButtonDown(LogitechF310.Button button) {
            switch (button) {
                case A:
                    logger.info("Detected object: {}", getLastDetectedObject());
                    logger.info("Distance: {}, yaw offset: {}", getLastDistance(), getLastYawOffset());
            }
        }
    }
}
