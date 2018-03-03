package org.teamtators.levitator.subsystems;

import edu.wpi.cscore.*;
import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.Deconfigurable;
import org.teamtators.common.math.Polynomial3;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.levitator.TatorRobot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Vision extends Subsystem implements Configurable<Vision.Config>, Deconfigurable {
    public static final Scalar REJECTED_CONTOUR_COLOR = new Scalar(255, 0, 0);
    public static final Scalar FILTERED_CONTOUR_COLOR = new Scalar(0, 255, 0);
    private Drive drive;
    private TatorRobot robot;
    private Config config;
    private SendableChooser<DisplayMode> displayModeChooser;
    private AtomicReference<DetectedObject> lastOutput = new AtomicReference<>(new DetectedObject());
    private Thread processing;

    public Vision(Drive drive) {
        super("Vision");
        this.drive = drive;
        displayModeChooser = new SendableChooser<>();
        displayModeChooser.addDefault("Source", DisplayMode.Source);
        displayModeChooser.addObject("HSV", DisplayMode.HSV);
        displayModeChooser.addObject("Threshold", DisplayMode.Threshold);
        displayModeChooser.addObject("AllContours", DisplayMode.AllContours);
        displayModeChooser.addObject("FilteredContours", DisplayMode.FilteredContours);
        setupCameraThread();
    }

    public void setupCameraThread() {
        killThread();
        processing = new Thread(this::processingThread, "TatorVision2Processing");
        processing.start();
    }

    private void processingThread() {
        UsbCameraInfo[] usbCameras = getCameras();
        if (usbCameras.length <= 1) {
            logger.warn("No USB webcam!");
            return;
        }
        logger.info("Using camera source: " + usbCameras[0].name);
        UsbCamera usbCamera = CameraServer.getInstance().startAutomaticCapture("USBWebCam_Pick", usbCameras[0].dev);
        MjpegServer server = CameraServer.getInstance().addServer("CameraServer2122");
        usbCamera.setResolution(320, 240);
        usbCamera.setExposureManual(config.exposure);

        for (VideoProperty property : usbCamera.enumerateProperties()) {
            if (property.getName().equalsIgnoreCase("saturation")) {
                property.set(config.saturation);
            }
        }

        CvSink cvSink = CameraServer.getInstance().getVideo();
        CvSource outputStream = CameraServer.getInstance().putVideo("Debug", 320, 240);

        Mat source = new Mat();
        Mat hsv = new Mat();
        Mat threshold = new Mat();
        Mat hierarchy = new Mat();
        Mat output = new Mat();
        ArrayList<MatOfPoint> points = new ArrayList<>();
        List<MatOfPoint> filteredPoints;
        while (!Thread.interrupted()) {
            points.clear();
            cvSink.grabFrame(source);
            DisplayMode displayMode = displayModeChooser.getSelected();
            if (displayMode == DisplayMode.Source) {
                source.copyTo(output);
            }
            Imgproc.cvtColor(source, hsv, Imgproc.COLOR_BGR2HSV);
            if (displayMode == DisplayMode.HSV) {
                hsv.copyTo(output);
            }
            Scalar lowerThreshold = new Scalar(config.lowerThreshold);
            Scalar upperThreshold = new Scalar(config.upperThreshold);
            Core.inRange(hsv, lowerThreshold, upperThreshold, threshold);
            if (displayMode == DisplayMode.Threshold) {
                threshold.copyTo(output);
            }
            Imgproc.findContours(threshold, points, hierarchy, Imgproc.RETR_LIST,
                    Imgproc.CHAIN_APPROX_TC89_L1);
            if (displayMode == DisplayMode.AllContours) {
                threshold.copyTo(output);
                Imgproc.drawContours(output, points, -1, REJECTED_CONTOUR_COLOR);
            }
            filteredPoints = points.stream().filter(contour -> {
                Size size = getSize(contour);
                double width = size.width;
                double height = size.height;
                return (width >= config.minWidth && width <= config.maxWidth)
                        && (height >= config.minHeight && height <= config.maxHeight);
            }).sorted(Comparator.comparingDouble(a -> getSize(a).area()))
                    .collect(Collectors.toList());
            if (displayMode == DisplayMode.FilteredContours || displayMode == DisplayMode.AllContours) {
                threshold.copyTo(output);
                Imgproc.drawContours(output, filteredPoints, -1, FILTERED_CONTOUR_COLOR);
            }
            if (filteredPoints.size() == 0) {
                lastOutput.set(new DetectedObject()); //null output, essentially
            } else {
                MatOfPoint selectedContour = filteredPoints.get(0);
                if (displayMode == DisplayMode.FilteredContours || displayMode == DisplayMode.AllContours) {
                    Imgproc.drawContours(output, Collections.singletonList(selectedContour),
                            -1, FILTERED_CONTOUR_COLOR, 2);
                }
                Rect rect = getRect(selectedContour);
                Moments moments = getMoments(selectedContour);
                double x = moments.m10 / moments.m00;
                double y = moments.m01 / moments.m00;
                double width = rect.width;
                double height = rect.height;
                double area = Imgproc.contourArea(selectedContour);
                lastOutput.set(new DetectedObject(x, y, area, width, height));
            }
            outputStream.putFrame(output);
        }
        cvSink.free();
        outputStream.free();
        server.free();
        source.release();
        hsv.release();
        threshold.release();
        hierarchy.release();
        output.release();
        usbCamera.free();
    }

    private UsbCameraInfo[] getCameras() {
        UsbCameraInfo[] usbCameras = UsbCamera.enumerateUsbCameras();
        logger.info("CameraServer reports {} usb cameras", usbCameras.length);
        return usbCameras;
    }

    private Size getSize(MatOfPoint contour) {
        return getSize(getRect(contour));
    }

    private Size getSize(Rect rect) {
        return new Size(rect.width, rect.height);
    }

    private Rect getRect(MatOfPoint contour) {
//        MatOfPoint2f contour2f = new MatOfPoint2f();
//        contour.convertTo(contour2f, CvType.CV_32FC2);
//        double epsilon = Imgproc.arcLength(contour2f, true)
//                * config.arcLengthPercentage;
//        Imgproc.approxPolyDP(contour2f, contour2f, epsilon, true);
        return Imgproc.boundingRect(contour);
    }

    private Moments getMoments(MatOfPoint contour) {
        return Imgproc.moments(contour);
    }

    public void configure(Config config) {
        this.config = config;
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
        if (detectedObject.x == null) return null;
        return (.5 * detectedObject.x * config.fovX) + config.yawOffset;
    }

    public Double getLastYawOffset() {
        return getYawOffset(getLastOutput());
    }

    public DetectedObject getLastOutput() {
        return lastOutput.get();
    }

    public Double getDistance(DetectedObject output) {
        if (output.y == null) {
            return null;
        }
        //noinspection SuspiciousNameCombination
        return config.distanceCalculator.calculate(output.y);
    }

    public void killThread() {
        if (processing != null)
            processing.interrupt();
    }

    public enum DisplayMode {
        Source, HSV, Threshold, AllContours, FilteredContours
    }

    public static class Config {
        //todo
        public boolean deferConfigToDashboard;

        public double[] lowerThreshold;
        public double[] upperThreshold;

        public double minWidth;
        public double maxWidth;

        public double minHeight;
        public double maxHeight;

        public double arcLengthPercentage = 0.01;

        public int exposure;
        public int saturation;

        public double fovX;

        public double yawOffset;

        public Polynomial3 distanceCalculator;
    }
}
