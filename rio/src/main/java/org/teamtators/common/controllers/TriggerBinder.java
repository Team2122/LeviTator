package org.teamtators.common.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.common.TatorRobotBase;
import org.teamtators.common.config.ConfigException;
import org.teamtators.common.scheduler.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TriggerBinder {
    private static final Logger logger = LoggerFactory.getLogger(TriggerBinder.class);
    private static final ObjectMapper objectMapper = TatorRobotBase.configMapper;

    private final Scheduler scheduler;
    private final CommandStore commandStore;
    private final Map<String, Controller<?, ?>> controllers = new HashMap<>();

    public TriggerBinder(Scheduler scheduler, CommandStore commandStore) {
        this.scheduler = scheduler;
        this.commandStore = commandStore;
    }

    public void putController(Controller<?, ?> controller) {
        controllers.put(controller.getName(), controller);
    }

    public void putControllers(Collection<Controller<?, ?>> controllers) {
        controllers.forEach(this::putController);
    }

    public Map<String, Controller<?, ?>> getControllers() {
        return this.controllers;
    }

    public void clearControllers() {
        controllers.clear();
    }

    /**
     * Binds triggers from the config
     *
     * @param triggersConfig Triggers configuration object
     */
    public void bindTriggers(TriggersConfig triggersConfig) {
        Map<String, Map<String, JsonNode>> bindings = triggersConfig.getBindings();
        for (Map.Entry<String, Controller<?, ?>> entry : controllers.entrySet()) {
            Controller<?, ?> controller = entry.getValue();
            Map<String, JsonNode> binding = bindings.get(controller.getName());
            bindButtonsToController(controller, binding);
        }
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
        try {
            TriggersConfig triggersConfig = objectMapper.treeToValue(config, TriggersConfig.class);
            bindTriggers(triggersConfig);
        } catch (JsonProcessingException e) {
            throw new ConfigException("Failed to load triggers config", e);
        }
    }

    private <TButton> void bindButtonsToController(Controller<TButton, ?> controller,
                                                   Map<String, JsonNode> bindings) {
        if (bindings == null) return;
        Class<TButton> buttonClass = controller.getButtonClass();
        for (Map.Entry<String, JsonNode> binding : bindings.entrySet()) {
            TButton button;
            try {
                JsonNode buttonTree = objectMapper.readTree(binding.getKey());
                button = objectMapper.treeToValue(buttonTree, buttonClass);
            } catch (Exception e) {
                throw new ConfigException("Invalid button name " + binding.getKey() +
                        " for controller " + controller.getName());
            }
            TriggerSource triggerSource = controller.getTriggerSource(button);
            TriggerAdder triggerAdder = scheduler.onTrigger(triggerSource);
            JsonNode specifier = binding.getValue();
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
                case "afterHeld":
                    binder.afterHeld();
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
}
