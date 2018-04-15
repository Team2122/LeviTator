package org.teamtators.common.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.scheduler.CommandStore;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

public class ConfigCommandStore extends CommandStore {
    private Map<String, Supplier<Command>> commandSuppliers = new HashMap<String, Supplier<Command>>();
    private Map<String, JsonNode> defaultConfigs = new HashMap<>();

    public static ObjectNode applyDefaults(ObjectNode object, ObjectNode defaults) {
        ObjectNode result = defaults.deepCopy();
        Iterator<Map.Entry<String, JsonNode>> it = object.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> field = it.next();
            result.set(field.getKey(), field.getValue());
        }
        return result;
    }

    private static <T extends Command> Constructor<T> getConstructor(Class<T> commandClass) {
        try {
            return commandClass.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(commandClass.toString() +
                    " does not have a no argument constructor", e);
        }
    }

    public void registerCommandSuppliers(Map<String, Supplier<Command>> commandSuppliers) {
        this.commandSuppliers.putAll(commandSuppliers);
    }

    public Map<String, Supplier<Command>> getCommandSuppliers() {
        return commandSuppliers;
    }

    public void registerCommand(String name, Supplier<Command> constructor) {
        commandSuppliers.put(name, constructor);
    }

    public Supplier<Command> getCommandSupplier(String className) throws ConfigException {
        Supplier<Command> supplier = commandSuppliers.get(className);
        if (supplier == null) {
            throw new ConfigException(String.format("Missing Command supplier \"%s\"", className));
        }
        return supplier;
    }

    public void clearRegistrations() {
        commandSuppliers.clear();
    }

    public <T extends Command> void registerClass(String name, Class<T> commandClass) {
        final Constructor<T> constructor = getConstructor(commandClass);
        Supplier<Command> commandSupplier = () -> {
            try {
                return constructor.newInstance();
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                throw new ConfigException("Error constructing command", e);
            }
        };
        registerCommand(name, commandSupplier);
    }

    public <T extends Command> void registerClass(Class<T> commandClass) {
        registerClass(commandClass.getSimpleName(), commandClass);
    }

    public void createCommandsFromConfig(ObjectNode json) throws ConfigException {
        Iterator<Map.Entry<String, JsonNode>> it = json.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> field = it.next();
            String commandName = field.getKey();
            char prefix = commandName.charAt(0);
            if (prefix == '$') { // Sequential command config
//                logger.trace("Creating CommandGroup '" + commandName + "'");
                putCommand(commandName, new ConfigSequentialCommand(this));
            } else if (prefix == '^') {
                String defaultFor = commandName.substring(1);
//                logger.trace("Adding default config for command '" + defaultFor + "'");
                defaultConfigs.put(defaultFor, field.getValue());
            } else {
//                logger.trace("Creating command '" + commandName + "'");
                createCommandFromConfig(commandName, field.getValue());
            }
        }
        HashMap<String, Command> commandsMapCopy = new HashMap<>(getCommands());
        for (Map.Entry<String, Command> commandEntry : commandsMapCopy.entrySet()) {
            Command command = commandEntry.getValue();
//            logger.trace("Configuring command '" + command.getName() + "'");
            JsonNode config = json.get(command.getName());
            configureCommand(command, config);
        }
        for (int i = 0; i < 10; i++) {
            for (Map.Entry<String, Command> commandEntry : commandsMapCopy.entrySet()) {
                Command command = commandEntry.getValue();
                command.updateRequirements();
            }
        }
    }

    public void configureCommand(Command command, JsonNode config) throws ConfigException {
        if (config == null) {
//            logger.trace("Missing config for command '" + command.getName() + "'");
            return;
        }
        if (config.isObject() && config.has("class")) {
            ObjectNode objectConfig = (ObjectNode) config;
            String className = objectConfig.remove("class").asText();
            JsonNode defaultConfig = defaultConfigs.get(className);
            if (defaultConfig != null && defaultConfig.isObject()) {
                config = applyDefaults(objectConfig, (ObjectNode) defaultConfig);
            }
        }
        Configurables.configureObject(command, config);
    }

    public Command createCommandFromConfig(String commandName, JsonNode config) throws ConfigException {
        String className;
        JsonNode classNode = config.get("class");
        if (classNode != null) {
            className = classNode.asText();
        } else {
            className = commandName;
        }
        return constructCommandClass(commandName, className);
    }

    public Command constructCommandClass(String commandName, String className) throws ConfigException {
        Supplier<Command> constructor = getCommandSupplier(className);
        Command command;
        try {
            command = constructor.get();
        } catch (Exception e) {
            throw new ConfigException("Exception thrown while constructing Command " + commandName, e);
        }
        putCommand(commandName, command);
        return command;
    }
}
