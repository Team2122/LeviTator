import com.fazecast.jSerialComm.SerialPort;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;

import java.nio.ByteBuffer;

public class SliderApp {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Finding slider...");

        SerialPort[] serialPorts = SerialPort.getCommPorts();
        SerialPort slider = null;

        for (int i = 0; i < serialPorts.length; i++) {
            System.out.println(serialPorts[i].getSystemPortName());
            //todo check input from serial port to verify that it is the slider
            if (serialPorts[i].isOpen() || true) {
                slider = serialPorts[i];
            }
        }

        if (slider == null) {
            System.err.println("No slider found!");
            System.exit(-1);
        }

        System.out.println("Slider found!");

        slider.openPort();
        System.out.println("Connecting to NetworkTables");

        NetworkTableInstance inst = NetworkTableInstance.getDefault();
        inst.startClientTeam(2122);
        NetworkTable smartDashboard = inst.getTable("SmartDashboard");

        double lastTargetHeight = 0.0;

        /*while (!inst.isConnected()) {
            //Thread.sleep(10);
        }*/

        System.out.println("Connected to NetworkTables!");

        while (true) {
            NetworkTableEntry pos = smartDashboard.getEntry("liftTarget");
            double num = pos.getNumber(-1).doubleValue();
            //System.out.println(num);
            if (num != -1 && lastTargetHeight != num) {
                System.out.printf("Target height changed, was %.3f now %.3f\n", lastTargetHeight, num);
                int result = slider.writeBytes(toBytes(num), 8);
                System.out.println(result);
                lastTargetHeight = num;
            }
        }
    }

    private static byte[] toBytes(double d) {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putDouble(d);
        return bytes;
    }
}
