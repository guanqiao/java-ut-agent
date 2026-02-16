package com.utagent.team;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TeamConfigSync {

    private static final Logger logger = LoggerFactory.getLogger(TeamConfigSync.class);
    private final ObjectMapper objectMapper;

    public TeamConfigSync() {
        this.objectMapper = new ObjectMapper();
    }

    public void exportConfig(TeamConfig config, Path path) throws IOException {
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(toMap(config));
        Files.writeString(path, json);
    }

    public TeamConfig importConfig(Path path) {
        try {
            String json = Files.readString(path);
            return parseConfig(json);
        } catch (IOException e) {
            logger.error("Failed to import config from {}", path, e);
            return null;
        }
    }

    public TeamConfig mergeConfigs(TeamConfig base, TeamConfig override) {
        TeamConfig.Builder builder = TeamConfig.builder();
        
        builder.teamName(override.getTeamName() != null ? override.getTeamName() : base.getTeamName());
        builder.targetCoverage(override.getTargetCoverage() > 0 ? override.getTargetCoverage() : base.getTargetCoverage());
        builder.namingConvention(override.getNamingConvention() != null ? override.getNamingConvention() : base.getNamingConvention());
        builder.version(override.getVersion() != null ? override.getVersion() : base.getVersion());
        
        Set<String> mergedPatterns = new HashSet<>();
        mergedPatterns.addAll(base.getTestPatterns());
        mergedPatterns.addAll(override.getTestPatterns());
        builder.testPatterns(new ArrayList<>(mergedPatterns));
        
        Map<String, Object> mergedSettings = new HashMap<>();
        if (base.getCustomSettings() != null) {
            mergedSettings.putAll(base.getCustomSettings());
        }
        if (override.getCustomSettings() != null) {
            mergedSettings.putAll(override.getCustomSettings());
        }
        builder.customSettings(mergedSettings);
        
        return builder.build();
    }

    public ValidationResult validateConfig(TeamConfig config) {
        List<String> errors = new ArrayList<>();
        
        if (config.getTeamName() == null || config.getTeamName().isEmpty()) {
            errors.add("teamName is required");
        }
        
        if (config.getTargetCoverage() < 0 || config.getTargetCoverage() > 1) {
            errors.add("targetCoverage must be between 0 and 1, got: " + config.getTargetCoverage());
        }
        
        if (config.getNamingConvention() != null && config.getNamingConvention().isEmpty()) {
            errors.add("namingConvention cannot be empty string");
        }
        
        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
    }

    public List<ConfigChange> generateDiff(TeamConfig oldConfig, TeamConfig newConfig) {
        List<ConfigChange> changes = new ArrayList<>();
        
        if (oldConfig.getTargetCoverage() != newConfig.getTargetCoverage()) {
            changes.add(new ConfigChange("targetCoverage", 
                oldConfig.getTargetCoverage(), newConfig.getTargetCoverage()));
        }
        
        if (!java.util.Objects.equals(oldConfig.getNamingConvention(), newConfig.getNamingConvention())) {
            changes.add(new ConfigChange("namingConvention", 
                oldConfig.getNamingConvention(), newConfig.getNamingConvention()));
        }
        
        if (!oldConfig.getTestPatterns().equals(newConfig.getTestPatterns())) {
            changes.add(new ConfigChange("testPatterns", 
                oldConfig.getTestPatterns(), newConfig.getTestPatterns()));
        }
        
        return changes;
    }

    public String generateShareableLink(TeamConfig config) {
        try {
            String json = objectMapper.writeValueAsString(toMap(config));
            return Base64.getUrlEncoder().encodeToString(json.getBytes());
        } catch (JsonProcessingException e) {
            logger.error("Failed to generate shareable link", e);
            return "";
        }
    }

    public TeamConfig parseShareableLink(String link) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(link);
            String json = new String(bytes);
            return parseConfig(json);
        } catch (Exception e) {
            logger.error("Failed to parse shareable link", e);
            return null;
        }
    }

    private Map<String, Object> toMap(TeamConfig config) {
        Map<String, Object> map = new HashMap<>();
        map.put("teamName", config.getTeamName());
        map.put("targetCoverage", config.getTargetCoverage());
        map.put("namingConvention", config.getNamingConvention());
        map.put("testPatterns", config.getTestPatterns());
        map.put("version", config.getVersion());
        map.put("customSettings", config.getCustomSettings());
        return map;
    }

    @SuppressWarnings("unchecked")
    private TeamConfig parseConfig(String json) throws JsonProcessingException {
        Map<String, Object> map = objectMapper.readValue(json, Map.class);
        
        List<String> patterns = new ArrayList<>();
        Object patternsObj = map.get("testPatterns");
        if (patternsObj instanceof List) {
            patterns = ((List<?>) patternsObj).stream()
                .map(Object::toString)
                .collect(Collectors.toList());
        }
        
        Map<String, Object> customSettings = new HashMap<>();
        Object settingsObj = map.get("customSettings");
        if (settingsObj instanceof Map) {
            ((Map<?, ?>) settingsObj).forEach((k, v) -> customSettings.put(k.toString(), v));
        }
        
        return TeamConfig.builder()
            .teamName((String) map.get("teamName"))
            .targetCoverage(map.get("targetCoverage") != null ? 
                ((Number) map.get("targetCoverage")).doubleValue() : 0.8)
            .namingConvention((String) map.get("namingConvention"))
            .testPatterns(patterns)
            .version((String) map.get("version"))
            .customSettings(customSettings)
            .build();
    }
}
