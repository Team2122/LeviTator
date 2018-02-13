package org.teamtators.common.hw;

import com.google.common.collect.EvictingQueue;
import edu.wpi.first.wpilibj.PIDSource;
import edu.wpi.first.wpilibj.PIDSourceType;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.SensorBase;
import edu.wpi.first.wpilibj.smartdashboard.SendableBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A sensor class for using an ADXRS453 gyroscope.
 * Measures angle change on the yaw axis.
 */
public class ADXRS453 extends SensorBase implements PIDSource, Gyro {
    public static final double UPDATE_PERIOD = 1.0 / 120.0;
    public static final int STARTUP_DELAY_MS = 50;
    protected static final Logger logger = LoggerFactory.getLogger(ADXRS453.class);
    private static final int kSensorData = 1 << 29;
    private static final int kRead = 1 << 31;
    private static final int kWrite = 1 << 30;
    private static final int kP = 1 << 0;
    private static final int kChk = 1 << 1;
    private static final int kCst = 1 << 2;
    private static final int kPwr = 1 << 3;
    private static final int kPor = 1 << 4;
    private static final int kNvm = 1 << 5;
    private static final int kQ = 1 << 6;
    private static final int kPll = 1 << 7;
    private static final int kFaultBits = kChk | kCst | kPwr | kPor | kNvm | kQ | kPll;
    private static final int kDu = 1 << 16;
    private static final int kRe = 1 << 17;
    private static final int kSpi = 1 << 18;
    private static final int kP0 = 1 << 28;
    private static final int kWriteBit = 1 << 29;
    private static final int kReadBit = 1 << 30;
    private static final int kInvalidData = 0x0;
    private static final int kValidData = 0x1;
    private static final int kTestData = 0x2;
    private static final int kReadWrite = 0x3;
    private static final int kSPIClockRate = 3000000;
    private static final double kDegreesPerSecondPerLSB = 80.0;
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Lock readLock = lock.readLock();
    private Lock writeLock = lock.writeLock();
    private AtomicBoolean hasStarted = new AtomicBoolean(false);
    private Thread startup;
    private SPI spi;
    private int lastDataSent;
    private double rate;
    private double angle;
    private boolean isCalibrating;
    private double calibrationOffset;
    private EvictingQueue<Double> calibrationValues;
    private double calibrationPeriod;
    private PIDSourceType pidSource = PIDSourceType.kDisplacement;

    /**
     * Creates a new ADXRS453
     *
     * @param port The SPI port to attach to
     */
    public ADXRS453(SPI.Port port) {
        this(new SPI(port));
    }

    /**
     * Creates a new ADXRS453
     *
     * @param spi The spi to use
     */
    public ADXRS453(SPI spi) {
        super();
        setName("ADXRS453");
        this.spi = spi;
        spi.setClockRate(kSPIClockRate);
        spi.setMSBFirst();
        spi.setSampleDataOnRising();
        spi.setClockActiveHigh();
        spi.setChipSelectActiveLow();
        setCalibrationPeriod(5.0);
        startup = new Thread(this::startup, "ADXRS453.Startup");
        fullReset();
    }

    private static int fixParity(int data) {
        data &= ~kP;
        return data | (calcParity(data) ? 0 : kP);
    }

    /**
     * Find the parity (even/odd) of an int
     *
     * @param data Data to find parity of
     * @return Whether or not the number of ones is odd
     */
    private static boolean calcParity(int data) {
        int parity = 0;
        while (data != 0) {
            parity += (data & 1);
            data >>>= 1;
        }
        return (parity % 2) == 1;
    }

    /**
     * Starts the startup process in a separate thread
     */
    public void start() {
        if (!hasStarted.get())
            startup.start();
    }

    @Override
    public double getCalibrationPeriod() {
        readLock.lock();
        try {
            return calibrationPeriod;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void setCalibrationPeriod(double calibrationPeriod) {
        writeLock.lock();
        try {
            this.calibrationPeriod = calibrationPeriod;
            calibrationValues = EvictingQueue.create((int) (calibrationPeriod / UPDATE_PERIOD));
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void fullReset() {
        writeLock.lock();
        try {
            rate = 0;
            angle = 0;
            calibrationOffset = 0;
            isCalibrating = false;
            calibrationValues.clear();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void startCalibration() {
        logger.info("Starting gyro calibration");
        writeLock.lock();
        try {
            calibrationOffset = 0;
            calibrationValues.clear();
            isCalibrating = true;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void finishCalibration() {
        writeLock.lock();
        try {
            if (!isCalibrating) {
                return;
            }
            calibrationOffset = calibrationValues.stream()
                    .reduce(0.0, (a, b) -> a + b) / calibrationValues.size();
            if (Double.isNaN(calibrationOffset) || Double.isInfinite(calibrationOffset)) {
                calibrationOffset = 0.0;
            }
            angle = 0;
            isCalibrating = false;
            logger.info("Finished calibrating gyro. Offset is {}", calibrationOffset);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public double getCalibrationOffset() {
        readLock.lock();
        try {
            return calibrationOffset;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isCalibrating() {
        return isCalibrating;
    }

    @Override
    public double getRate() {
        readLock.lock();
        try {
            return rate;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public double getAngle() {
        readLock.lock();
        try {
            return angle;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Sets the angle of the gyro
     *
     * @param angle The angle in degrees
     */
    public void setAngle(double angle) {
        writeLock.lock();
        try {
            this.angle = angle;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void resetAngle() {
        setAngle(0.0);
    }

    /**
     * Reads a register on the gyro
     *
     * @param reg The register to read
     * @return The value in the register
     */
    public int readRegister(Register reg) {
        int send, recv;
        send = kRead | (reg.address << 17);
        send = fixParity(send);
        write(send);
        recv = read();
        if (!checkResponse(recv)) {
            return 0x00;
        }
        return (recv >>> 5) & 0xFFFF;
    }

    /**
     * Writes to a register on the gyro
     *
     * @param reg   The register to write
     * @param value The value to write to it
     */
    public void writeRegister(Register reg, int value) {
        int send;
        send = kRead | (reg.address << 17) | (value << 1);
        send = fixParity(send);
        write(send);
        checkResponse(read());
    }

    /**
     * Gets the temperature from the internal register on the gyro
     *
     * @return The temperature in celcius
     */
    public double getTemperature() {
        int rawTemp = readRegister(Register.TEM);
        int tempLSB = rawTemp >>> 6;
        if ((tempLSB >>> 9) == 1) {
            tempLSB = -((~(tempLSB - 1)) & 0x03FF);
        }
        return (tempLSB / 5.0) + 45.0;
    }

    /**
     * Gets the part ID of the currently connected gyro
     *
     * @return The part ID
     */
    public int getPartID() {
        return readRegister(Register.PID);
    }

    /**
     * Gets the serial number of the currently connected gyro
     *
     * @return The 32 bit serial number
     */
    public int getSerialNumber() {
        int serial = 0;
        serial |= readRegister(Register.SN_H) << 16;
        serial |= readRegister(Register.SN_L);
        return serial;
    }

    public PIDSourceType getPIDSourceType() {
        return pidSource;
    }

    public void setPIDSourceType(PIDSourceType pidSource) {
        this.pidSource = pidSource;
    }

    public double pidGet() {
        switch (pidSource) {
            case kDisplacement:
                return getAngle();
            case kRate:
                return getRate();
            default:
                return 0;
        }
    }

    @Override
    public void finalize() throws Throwable {
        spi.free();
    }

    private boolean checkParity(int data) {
        boolean p1 = calcParity(data);
        boolean p0 = calcParity(data >>> 16);
        if (!p1 || !p0) {
            logger.error(String.format("Parity check failed on response: %#x", data));
            return false;
        }
        return true;
    }

    private boolean checkFaults(int data) {
        boolean hasFaults = (data & kFaultBits) != 0;
        StringBuilder logMessage;
        if (hasFaults) {
            logMessage = new StringBuilder("Faults detected:");
        } else {
            return true;
        }
        if ((data & kChk) != 0) {
            logMessage.append("\n * Self test enabled");
            logger.warn(logMessage.toString());
            return false;
        }
        if ((data & kCst) != 0)
            logMessage.append("\n * Continuous self test fault");
        if ((data & kPwr) != 0)
            logMessage.append("\n * Power fault");
        if ((data & kPor) != 0)
            logMessage.append("\n * Non-volatile programming fault");
        if ((data & kNvm) != 0)
            logMessage.append("\n * Non-volatile checksum fault");
        if ((data & kQ) != 0)
            logMessage.append("\n * Quadrature calculation fault");
        if ((data & kPll) != 0)
            logMessage.append("\n * Phase locked loop fault");
        logger.warn(logMessage.toString());

        return false;
    }

    private boolean checkResponse(int data) {
        if (!checkParity(data))
            return false;
        int status = (data >>> 26) & 0b11;
        switch (status) {
            case kInvalidData:
                logger.warn("Invalid data received");
                checkFaults(data);
                return false;
            case kValidData:
            case kTestData:
                return checkFaults(data);
            case kReadWrite:
                if ((data & (kReadBit | kWriteBit)) != 0)
                    return true;
                StringBuilder logMessage = new StringBuilder("Read/Write error: ");
                if ((data & kSpi) != 0)
                    logMessage.append("\n * SPI error");
                if ((data & kRe) != 0)
                    logMessage.append("\n * Request error");
                if ((data & kDu) != 0)
                    logMessage.append("\n * Data unavailable");
                logger.error(logMessage.toString());
                checkFaults(data);
                return false;
        }
        return false;
    }

    private boolean checkPartID() {
        int pid = getPartID();
        if ((pid & 0xff00) == 0x5200) {
            double temperature = getTemperature();
            int serial = getSerialNumber();
            logger.info(String.format(
                    "Part ID of gyro is correct (%#04x). Temperature: %f C. Serial: (%#08x)",
                    pid, temperature, serial));
            return true;
        } else {
            logger.error(String.format("Bad gyro found. Part id: %#04x", pid));
            return false;
        }
    }

    private void write(int data) {
        lastDataSent = data;
        ByteBuffer send = ByteBuffer.allocateDirect(4);
        send.putInt(data);
        spi.write(send, send.capacity()); // send it
    }

    private int read() {
        ByteBuffer recv = ByteBuffer.allocateDirect(4);
        spi.read(false, recv, recv.capacity()); // read into the buffer
        return recv.getInt();
    }

    private int transfer(int data) {
        lastDataSent = data;
        ByteBuffer send = ByteBuffer.allocateDirect(4);
        ByteBuffer recv = ByteBuffer.allocateDirect(4);
        send.putInt(data);
        spi.transaction(send, recv, send.capacity());
        return recv.getInt();
    }

    private void startup() {
        try {
            doStartup();
        } catch (InterruptedException ignored) {
        }
    }

    private void doStartup() throws InterruptedException {
        logger.info("Starting up gyro");
        int send, recv;
        send = fixParity(kSensorData | kChk);
        write(send);
        Thread.sleep(STARTUP_DELAY_MS); // in the spec
        send = fixParity(kSensorData);
        write(send);
        Thread.sleep(STARTUP_DELAY_MS);
        recv = transfer(send);
        if ((recv & kFaultBits) != kFaultBits) { // assert that all faults are set
            logger.error(String.format("Startup self test failed: %#x", recv));
            return;
        }
        Thread.sleep(STARTUP_DELAY_MS);
        write(send);
        Thread.sleep(STARTUP_DELAY_MS);
        if (!checkPartID()) {
            return;
        }
        hasStarted.set(true);
    }

    @Override
    public void update(double delta) {
        if (!hasStarted.get()) {
            return;
        }
        double elapsed = delta;
        writeLock.lock();
        try {
            int send, recv;
            send = fixParity(kSensorData);
            if (lastDataSent != send) // optimization, because of data latching
                write(send);
            recv = transfer(send);
            if (!checkResponse(recv))
                return;
            short rawRate = (short) (recv >>> 10); // bits 10-25
            rate = rawRate / kDegreesPerSecondPerLSB;
            if (elapsed > .5) // don't intergrate if more than half a second
                elapsed = 0;
            if (isCalibrating) {
                calibrationValues.add(rate);
            } else {
                rate -= calibrationOffset; // apply calibration offset
                angle += rate * elapsed; // intergrate it into the angle
            }
//            logger.trace("ADXRS453 recv=" + recv + ", isCalibrating=" + isCalibrating + ", delta=" + delta +
//                    ", rate=" + rate + ", angle=" + angle);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void initSendable(SendableBuilder builder) {
        builder.setSmartDashboardType("Gyro");
        builder.addDoubleProperty("Value", this::getAngle, this::setAngle);
        builder.addDoubleProperty("Rate", this::getRate, null);
    }

    public enum Register {
        RATE(0x00),
        TEM(0x02),
        LO_CST(0x04),
        HI_CST(0x06),
        QUAD(0x08),
        FAULT(0x0A),
        PID(0x0C),
        SN_H(0x0E),
        SN_L(0x10);

        public int address;

        Register(int address) {
            this.address = address;
        }
    }
}
