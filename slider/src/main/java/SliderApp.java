import com.fazecast.jSerialComm.SerialPort;
import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;

public class SliderApp {
    static double lastTargetHeight = 0.0;
    static SerialPort slider = null;
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Finding slider...");

        SerialPort[] serialPorts = SerialPort.getCommPorts();
        byte[] pingPacket = {SliderPacketType.PING};
        for (int i = 0; i < serialPorts.length; i++) {
            System.out.println(serialPorts[i].getSystemPortName());
            SerialPort p = serialPorts[i];

            boolean connected = p.openPort();

            if (!connected) {
                continue;
            }

            int written = p.writeBytes(pingPacket, pingPacket.length);

            if (written != pingPacket.length) {
                continue;
            }

            Thread.sleep(25);

            Scanner scanner = new Scanner(p.getInputStream());

            while (scanner.hasNextLine()) {
                String in = scanner.nextLine();
                System.out.println(in);
                if (in.startsWith("~2122~")) {
                    System.out.printf("Slider version %s\n", in.split("~")[1]);
                    slider = p;
                    break;
                }
            }
            if (slider != null) {
                break;
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
//        inst.startClientTeam(2122);
        inst.startClient("localhost");
        NetworkTable smartDashboard = inst.getTable("SmartDashboard");


        /*while (!inst.isConnected()) {
            //Thread.sleep(10);
        }*/

        System.out.println("Connected to NetworkTables!");

//        while (true) {
            NetworkTableEntry pos = smartDashboard.getEntry("liftTarget");
            pos.addListener(entryNotification -> {

                double num = pos.getNumber(-1).doubleValue();
                //System.out.println(num);
                if (num != -1 && lastTargetHeight != num) {
                    System.out.printf("Target height changed, was %.3f now %.3f\n", lastTargetHeight, num);
                    byte[] bytes = generateSliderUpdatePacket(num);
                    System.out.println(new String(bytes));
                    int result = slider.writeBytes(bytes, bytes.length);
                    System.out.printf("Sent bytes %s\n", result == bytes.length ? "sucessfully" : "unsucessfully");
                    lastTargetHeight = num;
                }
            }, EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);

//        }
        while (!Thread.interrupted()) {
            Thread.sleep(100);
        }
    }

    private static byte[] generateSliderUpdatePacket(double d) {
        String sb = "" + SliderPacketType.UPDATE_SLIDER;
        sb += d;
        sb += 'a';
        return sb.getBytes();
    }
}
