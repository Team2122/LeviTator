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
        server = cs.addServer("CameraServer2122");
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
            usbCamera = cs.startAutomaticCapture("USBWebCam_Pick", usbCameras[0].path);
            cvSink = cs.getVideo();
            outputStream = cs.putVideo("Vision_Pick", config.width, config.height);
        } else {
            logger.debug("Starting vision thread with existing source");
        }

        usbCamera.setResolution(config.width, config.height);
        usbCamera.setExposureManual(config.exposure);

        for (VideoProperty property : usbCamera.enumerateProperties()) {
            if (property.getName().equalsIgnoreCase("saturation")) {
                property.set(config.saturation);
            }
        }

        Mat source = Mat.zeros(config.width, config.height, CvType.CV_8UC3);
        Mat hsv = Mat.zeros(config.width, config.height, CvType.CV_8UC3);
        Mat threshold = Mat.zeros(config.width, config.height, CvType.CV_8UC1);
        Mat hierarchy = new Mat();
        Mat output = new Mat();
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        List<ContourInfo> filteredContours;
        while (!Thread.interrupted()) {
            contours.clear();
            long ret = cvSink.grabFrame(source);
            if (ret == 0) {
                logger.warn("frame not grabbed: " + cvSink.getError());
                continue;
            }
            DisplayMode displayMode = displayModeChooser.getSelected();
            if (displayMode == DisplayMode.Source || displayMode == DisplayMode.AllContours
                    || displayMode == DisplayMode.FilteredContours) {
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
            Imgproc.findContours(threshold, contours, hierarchy, Imgproc.RETR_LIST,
                    Imgproc.CHAIN_APPROX_TC89_L1);
            if (displayMode == DisplayMode.AllContours) {
                threshold.copyTo(output);
                Imgproc.drawContours(output, contours, -1, REJECTED_CONTOUR_COLOR);
            }
            filteredContours = contours.stream()
                    .map(ContourInfo::new)
                    .filter(contour -> {
                double width = contour.size.width;
                double height = contour.size.height;
                return (width >= config.minWidth && width <= config.maxWidth)
                        && (height >= config.minHeight && height <= config.maxHeight);
            }).sorted(Comparator.comparingDouble(a -> a.size.area()))
                    .collect(Collectors.toList());
            if (displayMode == DisplayMode.FilteredContours || displayMode == DisplayMode.AllContours) {
                threshold.copyTo(output);
                List<MatOfPoint> filteredRawContours = filteredContours.stream().map(ContourInfo::getContour).collect(Collectors.toList());
                Imgproc.drawContours(output, filteredRawContours, -1, FILTERED_CONTOUR_COLOR);
            }
            if (filteredContours.size() == 0) {
                lastOutput.set(new DetectedObject()); //null output, essentially
            } else {
                ContourInfo info = filteredContours.get(0);
                if (displayMode == DisplayMode.FilteredContours || displayMode == DisplayMode.AllContours) {
                    Imgproc.drawContours(output, Collections.singletonList(info.contour),
                            -1, FILTERED_CONTOUR_COLOR, 2);
                }
                double x = info.moments.m10 / info.moments.m00;
                double y = info.moments.m01 / info.moments.m00;
                double width = info.boundingRect.width;
                double height = info.boundingRect.height;
                double area = info.area;
                lastOutput.set(new DetectedObject(x, y, area, width, height));
            }
            outputStream.putFrame(output);
        }
//        cvSink.free();
//        outputStream.free();
//        server.free();
        source.release();
        hsv.release();
        threshold.release();
        hierarchy.release();
        output.release();
//        usbCamera.free();
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
        //todo
        public boolean deferConfigToDashboard;

        public int width;
        public int height;

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

    private class ContourInfo {
        public MatOfPoint contour;
        public Rect boundingRect;
        public Size size;
        public double area;
        public Moments moments;
        public ContourInfo(MatOfPoint contour) {
            this.contour = contour;
            this.boundingRect = Imgproc.boundingRect(contour);
            this.size = new Size(this.boundingRect.width, this.boundingRect.height);
            this.area = Imgproc.contourArea(contour);
            this.moments = Imgproc.moments(contour);
        }

        public MatOfPoint getContour() {
            return contour;
        }
    }
}
