package org.teamtators.common.hw;

import edu.wpi.first.wpilibj.PIDSource;
import edu.wpi.first.wpilibj.PIDSourceType;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.SensorBase;
import edu.wpi.first.wpilibj.hal.SPIJNI;
import edu.wpi.first.wpilibj.smartdashboard.SendableBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.common.util.ShortCircularBuffer;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A sensor class for using an ADXRS453 gyroscope.
 * Measures angle change on the yaw axis.
 */
@SuppressWarnings({"PointlessBitwiseExpression", "unused"})
public class ADXRS453 extends SensorBase implements PIDSource, Gyro {
    private static final double UPDATE_PERIOD = 1.0 / 120.0;
    private static final double SAMPLE_PERIOD = 1.0 / 2000.0;
    private static final int ACCUMULATOR_DEPTH = 2048;
    private static final int STARTUP_DELAY_MS = 50;
    private static final int DATA_SIZE = 4;
    private static final int SPI_CLOCK_RATE = 3000000;
    private static final double DEGREES_PER_SECOND_PER_LSB = 1 / 80.0;

    private static final Logger logger = LoggerFactory.getLogger(ADXRS453.class);
    private static final int kSensorData = 1 << 29;
    @SuppressWarnings("NumericOverflow")
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
    private static final int kInvalidData = 0b00 << 26;
    private static final int kValidData = 0b01 << 26;
    private static final int kTestData = 0b10 << 26;
    private static final int kReadWrite = 0b11 << 26;
    private static final int kStatusBits = 0b11 << 26;

    private final SPI spi;
    private final SPI.Port port;
    private final AtomicBoolean hasStarted = new AtomicBoolean(false);
    private final Thread startup;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();
    private final ByteBuffer accumulatorBuffer = ByteBuffer.allocateDirect(DATA_SIZE * ACCUMULATOR_DEPTH);

    private int sampleCount;
    private long rawRateSum;
    private double rate;
    private double angle;

    private boolean isCalibrating;
    private double calibrationOffset;
    private final ShortCircularBuffer calibrationValues = new ShortCircularBuffer(0);
    private double calibrationPeriod;

    private PIDSourceType pidSource = PIDSourceType.kDisplacement;
    private int lastFaults;
    private int lastStatus;

    /**
     * Creates a new ADXRS453
     *
     * @param port The SPI port to attach to
     */
    public ADXRS453(SPI.Port port) {
        super();
        setName("ADXRS453");
        this.spi = new SPI(port);
        this.port = port;
        spi.setClockRate(SPI_CLOCK_RATE);
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
            int capacity = (int) (calibrationPeriod / SAMPLE_PERIOD);
            calibrationValues.setCapacity(capacity);
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
            lastFaults = 0;
            lastStatus = 0;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void startCalibration() {
        logger.trace("Starting gyro calibration");
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
                    .mapToDouble(rawRate -> rawRate * DEGREES_PER_SECOND_PER_LSB)
                    .average()
                    .orElse(0.0);
            if (!Double.isFinite(calibrationOffset)) {
                calibrationOffset = 0.0;
            }
            angle = 0;
            isCalibrating = false;
            logger.debug("Finished calibrating gyro. Offset is {}", calibrationOffset);
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
    private int readRegister(Register reg) {
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
    @SuppressWarnings("unused")
    private void writeRegister(Register reg, int value) {
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
    private double getTemperature() {
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
    private int getPartID() {
        return readRegister(Register.PID);
    }

    /**
     * Gets the serial number of the currently connected gyro
     *
     * @return The 32 bit serial number
     */
    private int getSerialNumber() {
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
    public void free() {
        hasStarted.set(false);
        spi.free();
        startup.interrupt();
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
        int faults = data & kFaultBits;
        boolean hasFaults = faults != 0;
        StringBuilder logMessage;
        if (hasFaults) {
            if (faults == lastFaults) {
                return false;
            }
            lastFaults = faults;
            logMessage = new StringBuilder("Faults detected:");
        } else {
            lastFaults = faults;
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
        int status = data & kStatusBits;
        boolean isSame = status == lastStatus;
        lastStatus = status;
        switch (status) {
            case kInvalidData:
                if (!isSame) {
                    logger.warn("Invalid data received");
                    checkFaults(data);
                }
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
            logger.debug(String.format(
                    "Part ID of gyro is correct (%#04x). Temperature: %f C. Serial: (%#08x)",
                    pid, temperature, serial));
            return true;
        } else {
            logger.error(String.format("Bad gyro found. Part id: %#04x", pid));
            return false;
        }
    }

    private ByteBuffer dataToBytes(int data) {
        ByteBuffer bytes = ByteBuffer.allocate(DATA_SIZE);
        bytes.putInt(data);
        return bytes;
    }

    private void write(int data) {
        ByteBuffer send = dataToBytes(data);
        spi.write(send, send.capacity()); // send it
    }

    private int read() {
        ByteBuffer recv = ByteBuffer.allocate(DATA_SIZE);
        spi.read(false, recv, recv.capacity()); // read into the buffer
        return recv.getInt();
    }

    private int transfer(int data) {
        ByteBuffer send = dataToBytes(data);
        ByteBuffer recv = ByteBuffer.allocate(DATA_SIZE);
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
        logger.debug("Starting up gyro");
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
        write(send);
        Thread.sleep(STARTUP_DELAY_MS);
        int cmd = fixParity(kSensorData);
        spi.initAuto(DATA_SIZE * ACCUMULATOR_DEPTH);
        ByteBuffer cmdBytes = dataToBytes(cmd);
        spi.setAutoTransmitData(cmdBytes.array(), 0);
        spi.startAutoRate(SAMPLE_PERIOD);

        hasStarted.set(true);
    }

    @Override
    public void update(double delta) {
        if (!hasStarted.get()) {
            return;
        }
        writeLock.lock();
        try {
            boolean hasData = true;
            sampleCount = 0;
            rawRateSum = 0;
            while (hasData) {
                int availableBytes = SPIJNI.spiReadAutoReceivedData(port.value, accumulatorBuffer, 0, 0);

                availableBytes -= availableBytes % DATA_SIZE;
                if (availableBytes > DATA_SIZE * ACCUMULATOR_DEPTH) {
                    availableBytes = DATA_SIZE * ACCUMULATOR_DEPTH;
                    hasData = true;
                } else if (availableBytes == 0) {
                    break;
                } else {
                    hasData = false;
                }

                SPIJNI.spiReadAutoReceivedData(port.value, accumulatorBuffer, availableBytes, 0);

                for (int i = 0; i < availableBytes; i += DATA_SIZE) {
                    int data = accumulatorBuffer.getInt(i);
                    processSensorData(data);
                }
            }

            if (sampleCount > 0) {
                // apply calibration offset
                double rateSum = rawRateSum * DEGREES_PER_SECOND_PER_LSB - calibrationOffset * sampleCount;
                rate = rateSum / sampleCount;
                angle += rateSum * SAMPLE_PERIOD;
            } else {
                rate = 0.0;
            }
        } finally {
            writeLock.unlock();
        }
    }

    private void processSensorData(int data) {
        if (!checkResponse(data))
            return;
        short rawRate = (short) (data >>> 10);
        if (isCalibrating) {
            calibrationValues.push(rawRate);
        } else {
            rawRateSum += rawRate;
        }
        sampleCount++;
    }

    @Override
    public void initSendable(SendableBuilder builder) {
        builder.setSmartDashboardType("Gyro");
        builder.addDoubleProperty("Value", this::getAngle, this::setAngle);
        builder.addDoubleProperty("Rate", this::getRate, null);
    }

    @SuppressWarnings("unused")
    private enum Register {
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
