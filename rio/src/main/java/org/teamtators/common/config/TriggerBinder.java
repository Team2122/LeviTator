package org.teamtators.common.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wpi.first.wpilibj.Joystick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.common.hw.LogitechF310;
import org.teamtators.common.scheduler.*;

import java.util.Map;
import java.util.Set;

public class TriggerBinder {
    private static final Logger logger = LoggerFactory.getLogger(TriggerBinder.class);
    private Scheduler scheduler;
    private CommandStore commandStore;
    private ObjectMapper objectMapper;
    private LogitechF310 driverJoystick;
    private LogitechF310 gunnerJoystick;

    public TriggerBinder(Scheduler scheduler, CommandStore commandStore,
                         ObjectMapper objectMapper) {
        this.scheduler = scheduler;
        this.commandStore = commandStore;
        this.objectMapper = objectMapper;
    }

    public LogitechF310 getDriverJoystick() {
        return driverJoystick;
    }

    public void setDriverJoystick(LogitechF310 driverJoystick) {
        this.driverJoystick = driverJoystick;
    }

    public LogitechF310 getGunnerJoystick() {
        return gunnerJoystick;
    }

    public void setGunnerJoystick(LogitechF310 gunnerJoystick) {
        this.gunnerJoystick = gunnerJoystick;
    }

    /**
     * Binds triggers from the config
     *
     * @param triggersConfig Triggers configuration object
     */
    public void bindTriggers(TriggersConfig triggersConfig) {
        bindButtonsToLogitechF310(triggersConfig.driver, driverJoystick);
        bindButtonsToLogitechF310(triggersConfig.gunner, gunnerJoystick);
        registerDefaults(triggersConfig.defaults);
    }

    private void registerDefaults(Set<String> defaults) {
        for (String defaultCommand : defaults) {
            Command command = getCommandForBinding(defaultCommand);
            scheduler.registerDefaultCommand(command);
        }
    }

    /**
     * Binds triggers from the config
     *
     * @param config Root config node of triggers
     */
    public void bindTriggers(JsonNode config) {
        TriggersConfig triggersConfig;
        try {
            triggersConfig = objectMapper.treeToValue(config, TriggersConfig.class);
        } catch (JsonProcessingException e) {
            throw new ConfigException("Failed to load triggers config", e);
        }
        bindTriggers(triggersConfig);
    }

    /**
     * Bind buttons to a joystick using a TriggersConfig
     *
     * @param buttonsMap Map of buttons and their bindings
     * @param joystick   Joystick to bind buttons to
     */
    public void bindButtonsToLogitechF310(Map<LogitechF310.Button, JsonNode> buttonsMap, LogitechF310 joystick) {
        if (buttonsMap == null) return;
        for (Map.Entry<LogitechF310.Button, JsonNode> entry : buttonsMap.entrySet()) {
            TriggerSource triggerSource = joystick.getTriggerSource(entry.getKey());
            TriggerAdder triggerAdder = scheduler.onTrigger(triggerSource);
            JsonNode specifier = entry.getValue();
            if (specifier.isTextual()) {
                bindTriggerWithSpecifier(triggerAdder, specifier.asText());
            } else if (specifier.isArray()) {
                for (JsonNode arrayElem : specifier) {
                    if (!arrayElem.isTextual()) {
                        throw new ConfigException("Trigger specifiers must be textual, not \"" + arrayElem + '"');
                    }
                    bindTriggerWithSpecifier(triggerAdder, arrayElem.asText());
                }
            } else {
                throw new ConfigException("Trigger specifier must be textual or array, not \"" + specifier + '"');
            }
        }
    }

    private void bindTriggerWithSpecifier(TriggerAdder triggerAdder, String bindingSpecifier) {
        String[] binding = bindingSpecifier.split(" ");
        invalidBinding:
        if (binding.length == 2) {
            Command command = getCommandForBinding(binding[1]);
            switch (binding[0]) {
                case "whilePressed":
                    triggerAdder.whilePressed(command);
                    return;
            }
        } else if (binding.length == 3) {
            Command command = getCommandForBinding(binding[2]);
            TriggerAdder.TriggerBinder binder;
            switch (binding[0]) {
                case "start":
                    binder = triggerAdder.start(command);
                    break;
                case "toggle":
                    binder = triggerAdder.toggle(command);
                    break;
                case "cancel":
                    binder = triggerAdder.cancel(command);
                    break;
                default:
                    break invalidBinding;
            }
            switch (binding[1]) {
                case "whenPressed":
                    binder.whenPressed();
                    return;
                case "whenReleased":
                    binder.whenReleased();
                    return;
            }
        }
        throw new ConfigException("Invalid binding specifier: " + bindingSpecifier);
    }

    private Command getCommandForBinding(String commandName) {
        Command command;
        try {
            command = commandStore.getCommand(commandName);
        } catch (IllegalArgumentException e) {
            throw new ConfigException("Command " + commandName + " in binding does not exist");
        }
        return command;
    }

    public void bindButtonsToJoystick(Joystick joystick) {

    }
}
