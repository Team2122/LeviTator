package org.teamtators.levitator.subsystems;

import edu.wpi.cscore.*;
import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.Deconfigurable;
import org.teamtators.common.math.Polynomial3;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.levitator.TatorRobot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Vision extends Subsystem implements Configurable<Vision.Config>, Deconfigurable {
    private final Drive drive;
    private Config config;
    private SendableChooser<String> displayModeChooser;
    private AtomicReference<VisionOutput> lastOutput = new AtomicReference<>(new VisionOutput());

    public Vision(TatorRobot robot) {
        super("Vision");
        this.drive = robot.getSubsystems().getDrive();
        displayModeChooser = new SendableChooser<>();
        displayModeChooser.addDefault("Source", "source");
        displayModeChooser.addObject("HSV", "hsv");
        displayModeChooser.addObject("Threshold", "threshold");
        displayModeChooser.addObject("RawContours", "rawcontours");
        displayModeChooser.addObject("FilteredContours", "filteredcontours");
        setupCameraThread();
    }

    public void setupCameraThread() {
        killThreads();
        new Thread(() -> {
            UsbCameraInfo[] usbCameras = UsbCamera.enumerateUsbCameras();
            logger.info("CameraServer reports {} usb cameras", usbCameras.length);
            if (usbCameras.length > 0) {
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
                    if (displayModeChooser.getSelected().equals("source")) {
                        source.copyTo(output);
                    }
                    Imgproc.cvtColor(source, hsv, Imgproc.COLOR_BGR2HSV);
                    if (displayModeChooser.getSelected().equals("hsv")) {
                        hsv.copyTo(output);
                    }
                    Core.inRange(hsv, config.lowerThreshold, config.upperThreshold, threshold);
                    if (displayModeChooser.getSelected().equals("threshold")) {
                        threshold.copyTo(output);
                    }
                    Imgproc.findContours(threshold, points, hierarchy, Imgproc.RETR_LIST,
                            Imgproc.CHAIN_APPROX_TC89_L1);
                    if (displayModeChooser.getSelected().equals("rawcontours")) {
                        threshold.copyTo(output);
                        Imgproc.drawContours(output, points, -1, Scalar.all(255));
                    }
                    filteredPoints = points.stream().filter(contour -> {
                        Size size = getSize(contour);
                        double width = size.width;
                        double height = size.height;
                        return (width >= config.minWidth && width <= config.maxWidth)
                                && (height >= config.minHeight && height <= config.maxHeight);
                    }).sorted((a, b) -> Double.compare(getSize(a).area(), getSize(b).area()))
                            .collect(Collectors.toList());
                    if (displayModeChooser.getSelected().equals("filteredcontours")) {
                        threshold.copyTo(output);
                        Imgproc.drawContours(output, filteredPoints, -1, Scalar.all(255));
                    }
                    if (filteredPoints.size() == 0) {
                        lastOutput.set(new VisionOutput()); //null output, essentially
                    } else {
                        RotatedRect rect = getRotatedRect(filteredPoints.get(0));
                        double x = rect.center.x;
                        double y = rect.center.y;
                        double width = rect.size.width;
                        double height = rect.size.height;
                        double area = rect.size.area();
                        lastOutput.set(new VisionOutput(x, y, area, width, height));
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
            } else {
                logger.warn("No USB webcam!");
                return;
            }
        }, "TatorVision2Processing-" + (int) (Math.random() * 1000)).start();
    }

    private Size getSize(MatOfPoint contour) {
        return getSize(getRotatedRect(contour));
    }

    private Size getSize(RotatedRect rect) {
        return new Size(rect.size.width, rect.size.height);
    }

    private RotatedRect getRotatedRect(MatOfPoint contour) {
        MatOfPoint2f contour2f = new MatOfPoint2f();
        contour.convertTo(contour2f, CvType.CV_32FC2);
//        double epsilon = Imgproc.arcLength(contour2f, true)
//                * config.arcLengthPercentage;
//        Imgproc.approxPolyDP(contour2f, contour2f, epsilon, true);
        return Imgproc.minAreaRect(contour2f);
    }

    public void configure(Config config) {
        this.config = config;
    }

    public void deconfigure() {
        killThreads();
    }

    public static class Config {
        //todo
        public boolean deferConfigToDashboard;

        public Scalar lowerThreshold;
        public Scalar upperThreshold;

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

    public Double getNewRobotAngle(VisionOutput output) {
        Double yawOffset = getYawOffset(output);
        if (yawOffset == null) {
            return null;
        }
        return yawOffset + drive.getYawAngle();
    }

    public Double getYawOffset(VisionOutput visionOutput) {
        if (visionOutput.x == null) return null;
        return (.5 * visionOutput.x * config.fovX) + config.yawOffset;
    }

    public Double getLastYawOffset() {
        return getYawOffset(getLastOutput());
    }

    public VisionOutput getLastOutput() {
        return lastOutput.get();
    }

    public void killThreads() {
        Thread.getAllStackTraces().forEach(((thread, stackTraceElements) -> {
            if (thread.getName().startsWith("TatorVision2Processing-")) {
                thread.interrupt();
            }
        }));
    }
}
