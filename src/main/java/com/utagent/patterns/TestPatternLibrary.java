package com.utagent.patterns;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class TestPatternLibrary {

    private static final Logger logger = LoggerFactory.getLogger(TestPatternLibrary.class);

    private final Map<String, TestPattern> patterns;
    private final ObjectMapper objectMapper;

    public TestPatternLibrary() {
        this.patterns = new HashMap<>();
        this.objectMapper = new ObjectMapper();
        initializeDefaultPatterns();
    }

    private void initializeDefaultPatterns() {
        addPattern(TestPattern.builder()
            .name("Arrange-Act-Assert")
            .category(PatternCategory.STRUCTURE)
            .description("Classic AAA pattern for test organization")
            .template("""
                @Test
                @DisplayName("Should {{methodName}}")
                void should{{methodName}}() {
                    // Arrange
                    {{className}} {{instanceName}} = new {{className}}();
                    
                    // Act
                    var result = {{instanceName}}.{{methodName}}();
                    
                    // Assert
                    assertThat(result).isNotNull();
                }
                """)
            .tags(List.of("structure", "basic"))
            .applicableClassTypes(List.of("Service", "Util", "Helper"))
            .build());

        addPattern(TestPattern.builder()
            .name("Given-When-Then")
            .category(PatternCategory.STRUCTURE)
            .description("BDD style test pattern")
            .template("""
                @Test
                @DisplayName("Should {{methodName}}")
                void should{{methodName}}() {
                    // Given
                    {{className}} {{instanceName}} = new {{className}}();
                    
                    // When
                    var result = {{instanceName}}.{{methodName}}();
                    
                    // Then
                    assertThat(result).isNotNull();
                }
                """)
            .tags(List.of("structure", "bdd"))
            .applicableClassTypes(List.of("Service", "Controller"))
            .build());

        addPattern(TestPattern.builder()
            .name("Mock-Dependency")
            .category(PatternCategory.MOCKING)
            .description("Pattern for mocking dependencies")
            .template("""
                @Test
                @DisplayName("Should {{methodName}}")
                void should{{methodName}}() {
                    // Arrange
                    @Mock
                    Dependency dependency;
                    @InjectMocks
                    {{className}} {{instanceName}};
                    
                    // Act
                    var result = {{instanceName}}.{{methodName}}();
                    
                    // Assert
                    assertThat(result).isNotNull();
                }
                """)
            .tags(List.of("mocking", "mockito"))
            .applicableClassTypes(List.of("Service", "Controller"))
            .build());

        addPattern(TestPattern.builder()
            .name("Parameterized-Test")
            .category(PatternCategory.PARAMETERIZED)
            .description("Parameterized test pattern")
            .template("""
                @ParameterizedTest
                @CsvSource({
                    "input1, expected1",
                    "input2, expected2"
                })
                @DisplayName("Should {{methodName}} with various inputs")
                void should{{methodName}}WithVariousInputs(String input, String expected) {
                    {{className}} {{instanceName}} = new {{className}}();
                    var result = {{instanceName}}.{{methodName}}(input);
                    assertThat(result).isEqualTo(expected);
                }
                """)
            .tags(List.of("parameterized", "data-driven"))
            .applicableClassTypes(List.of("Service", "Util", "Calculator"))
            .build());

        addPattern(TestPattern.builder()
            .name("Exception-Test")
            .category(PatternCategory.ASSERTION)
            .description("Pattern for testing exceptions")
            .template("""
                @Test
                @DisplayName("Should throw exception when {{methodName}} with invalid input")
                void shouldThrowExceptionWhen{{methodName}}WithInvalidInput() {
                    {{className}} {{instanceName}} = new {{className}}();
                    assertThrows(IllegalArgumentException.class, () -> {
                        {{instanceName}}.{{methodName}}(null);
                    });
                }
                """)
            .tags(List.of("exception", "negative"))
            .applicableClassTypes(List.of("Service", "Validator"))
            .build());

        addPattern(TestPattern.builder()
            .name("Repository-Test")
            .category(PatternCategory.DATA)
            .description("Pattern for repository tests")
            .template("""
                @Test
                @DisplayName("Should {{methodName}}")
                void should{{methodName}}() {
                    // Arrange
                    {{className}} {{instanceName}} = new {{className}}();
                    
                    // Act
                    var result = {{instanceName}}.{{methodName}}();
                    
                    // Assert
                    assertThat(result).isNotNull();
                }
                """)
            .tags(List.of("repository", "data"))
            .applicableClassTypes(List.of("Repository", "Dao"))
            .build());

        addPattern(TestPattern.builder()
            .name("Controller-Test")
            .category(PatternCategory.STRUCTURE)
            .description("Pattern for controller tests")
            .template("""
                @Test
                @DisplayName("Should {{methodName}}")
                void should{{methodName}}() throws Exception {
                    // Arrange
                    @Mock
                    Service service;
                    {{className}} {{instanceName}} = new {{className}}(service);
                    
                    // Act & Assert
                    mockMvc.perform(get("/api/resource"))
                        .andExpect(status().isOk());
                }
                """)
            .tags(List.of("controller", "mvc"))
            .applicableClassTypes(List.of("Controller", "RestController"))
            .build());
    }

    public Optional<TestPattern> findByName(String name) {
        return Optional.ofNullable(patterns.get(name));
    }

    public List<TestPattern> findByCategory(PatternCategory category) {
        return patterns.values().stream()
            .filter(p -> p.getCategory() == category)
            .collect(Collectors.toList());
    }

    public List<TestPattern> findByTag(String tag) {
        return patterns.values().stream()
            .filter(p -> p.getTags().contains(tag))
            .collect(Collectors.toList());
    }

    public void addPattern(TestPattern pattern) {
        patterns.put(pattern.getName(), pattern);
    }

    public void removePattern(String name) {
        patterns.remove(name);
    }

    public void updatePattern(TestPattern pattern) {
        patterns.put(pattern.getName(), pattern);
    }

    public List<PatternCategory> getAllCategories() {
        return Arrays.asList(PatternCategory.values());
    }

    public int countByCategory(PatternCategory category) {
        return (int) patterns.values().stream()
            .filter(p -> p.getCategory() == category)
            .count();
    }

    public List<TestPattern> recommendForClassType(String classType) {
        return patterns.values().stream()
            .filter(p -> p.getApplicableClassTypes().stream()
                .anyMatch(type -> classType.contains(type)))
            .collect(Collectors.toList());
    }

    public String exportToJson() {
        try {
            List<Map<String, Object>> patternList = patterns.values().stream()
                .map(this::patternToMap)
                .collect(Collectors.toList());
            return objectMapper.writeValueAsString(patternList);
        } catch (JsonProcessingException e) {
            logger.error("Failed to export patterns to JSON", e);
            return "[]";
        }
    }

    public void importFromJson(String json) {
        try {
            List<Map<String, Object>> patternList = objectMapper.readValue(json, List.class);
            for (Map<String, Object> map : patternList) {
                TestPattern pattern = mapToPattern(map);
                if (pattern != null) {
                    patterns.put(pattern.getName(), pattern);
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to import patterns from JSON", e);
        }
    }

    private Map<String, Object> patternToMap(TestPattern pattern) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", pattern.getName());
        map.put("category", pattern.getCategory().name());
        map.put("description", pattern.getDescription());
        map.put("template", pattern.getTemplate());
        map.put("tags", pattern.getTags());
        map.put("applicableClassTypes", pattern.getApplicableClassTypes());
        return map;
    }

    @SuppressWarnings("unchecked")
    private TestPattern mapToPattern(Map<String, Object> map) {
        try {
            String categoryStr = (String) map.get("category");
            PatternCategory category = PatternCategory.valueOf(categoryStr);
            
            List<String> tags = new ArrayList<>();
            Object tagsObj = map.get("tags");
            if (tagsObj instanceof List) {
                tags = ((List<?>) tagsObj).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
            }
            
            List<String> applicableTypes = new ArrayList<>();
            Object typesObj = map.get("applicableClassTypes");
            if (typesObj instanceof List) {
                applicableTypes = ((List<?>) typesObj).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
            }
            
            return TestPattern.builder()
                .name((String) map.get("name"))
                .category(category)
                .description((String) map.get("description"))
                .template((String) map.get("template"))
                .tags(tags)
                .applicableClassTypes(applicableTypes)
                .build();
        } catch (Exception e) {
            logger.warn("Failed to parse pattern from map: {}", map, e);
            return null;
        }
    }
}
