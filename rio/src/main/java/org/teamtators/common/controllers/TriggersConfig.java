package org.teamtators.common.controllers;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TriggersConfig {
    @JsonIgnore
    public Map<String, Map<String, JsonNode>> bindings = new HashMap<>();

    public Set<String> defaults;

    @JsonAnyGetter
    public Map<String, Map<String, JsonNode>> getBindings() {
        return bindings;
    }

    @JsonAnySetter
    public void setBinding(String name, Map<String, JsonNode> binding) {
        bindings.put(name, binding);
    }
}
