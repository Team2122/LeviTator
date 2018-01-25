package org.teamtators.common.datalogging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.teamtators.vision.MqttTopics;

import java.util.HashMap;

public class TatorDashboardAdapter implements Dashboard {
    private MqttAsyncClient client;
    private HashMap<String, String> values = new HashMap<>();

    public TatorDashboardAdapter(MqttAsyncClient client) {
        this.client = client;
    }

    @Override
    public void putBoolean(String name, boolean val) {
        put(name, String.valueOf(val));
    }

    @Override
    public void putNumber(String name, double val) {
        put(name, String.valueOf(val));
    }

    @Override
    public void putString(String name, String val) {
        put(name, val);
    }

    //just incase we need more put* methods
    private void put(String name, String val) {
        values.put(name, val);
    }

    void writeToMqtt() {
        try {
            byte[] payload = new ObjectMapper().writeValueAsBytes(values);
            client.publish(MqttTopics.ROBOT_DASHBOARD_DATA, payload, 1, true);
        } catch (Exception e) {
            //ignore the problem. MqttClient might not be connected yet.
        } finally {
            values.clear();
        }
    }
}
