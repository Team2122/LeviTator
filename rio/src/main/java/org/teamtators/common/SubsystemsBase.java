package org.teamtators.common;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.teamtators.common.config.ConfigLoader;
import org.teamtators.common.control.Updatable;
import org.teamtators.common.scheduler.Subsystem;

import java.util.List;

/**
 * Created by TatorsDriverStation on 10/1/2017.
 */
public abstract class SubsystemsBase {
    public abstract List<Subsystem> getSubsystemList();

    public abstract void configure(ConfigLoader configLoader);

    public abstract List<Updatable> getControllers();

    public abstract MqttAsyncClient getMqttClient();
}
