package org.teamtators.common.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Loads in configuration files
 */
public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    private String configDir;
    private ObjectMapper objectMapper;

    public ConfigLoader(String configDir, ObjectMapper objectMapper) {
        this.configDir = configDir;
        this.objectMapper = objectMapper;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public JsonNode load(String fileName) {
        String filePath = configDir + File.separator + fileName;
        try (InputStream fileStream = new FileInputStream(filePath)) {
            logger.trace("Loading config from path {}", filePath);
            return objectMapper.reader().readTree(fileStream);
        } catch (IOException e) {
            throw new ConfigException(String.format("Error loading config %s", fileName), e);
        }
    }
}
