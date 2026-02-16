package com.utagent.testdata;

import com.utagent.model.ClassInfo;
import com.utagent.model.FieldInfo;
import com.utagent.model.MethodInfo;
import com.utagent.model.ParameterInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class TestDataFactory {

    private static final Logger logger = LoggerFactory.getLogger(TestDataFactory.class);

    private final Random random;
    private final Map<Class<?>, Supplier<TestDataValue>> generators;
    private double nullProbability = 0.0;

    public TestDataFactory() {
        this(System.currentTimeMillis());
    }

    public TestDataFactory(long seed) {
        this.random = new Random(seed);
        this.generators = new ConcurrentHashMap<>();
        registerDefaultGenerators();
    }

    private void registerDefaultGenerators() {
        generators.put(String.class, this::generateString);
        generators.put(Integer.class, this::generateInteger);
        generators.put(int.class, this::generateInteger);
        generators.put(Long.class, this::generateLong);
        generators.put(long.class, this::generateLong);
        generators.put(Double.class, this::generateDouble);
        generators.put(double.class, this::generateDouble);
        generators.put(Float.class, this::generateFloat);
        generators.put(float.class, this::generateFloat);
        generators.put(Boolean.class, this::generateBoolean);
        generators.put(boolean.class, this::generateBoolean);
        generators.put(Byte.class, this::generateByte);
        generators.put(byte.class, this::generateByte);
        generators.put(Short.class, this::generateShort);
        generators.put(short.class, this::generateShort);
        generators.put(Character.class, this::generateCharacter);
        generators.put(char.class, this::generateCharacter);
        generators.put(BigDecimal.class, this::generateBigDecimal);
        generators.put(BigInteger.class, this::generateBigInteger);
        generators.put(LocalDate.class, this::generateLocalDate);
        generators.put(LocalDateTime.class, this::generateLocalDateTime);
        generators.put(LocalTime.class, this::generateLocalTime);
        generators.put(UUID.class, this::generateUUID);
        generators.put(List.class, this::generateList);
        generators.put(Map.class, this::generateMap);
    }

    public TestDataValue generate(Class<?> type) {
        if (random.nextDouble() < nullProbability && !type.isPrimitive()) {
            return TestDataValue.nullValue();
        }

        Supplier<TestDataValue> generator = generators.get(type);
        if (generator != null) {
            return generator.get();
        }

        return generateUnknownType(type);
    }

    public List<TestDataValue> generateMultiple(Class<?> type, int count) {
        List<TestDataValue> values = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            values.add(generate(type));
        }
        return values;
    }

    public List<TestDataValue> generateScenarios(Class<?> type) {
        List<TestDataValue> scenarios = new ArrayList<>();
        
        scenarios.add(generate(type));
        
        if (!type.isPrimitive()) {
            scenarios.add(TestDataValue.nullValue());
        }
        
        if (type == String.class) {
            scenarios.add(new TestDataValue("", "Empty string", TestDataScenario.EMPTY));
        } else if (type == Integer.class || type == int.class) {
            scenarios.add(new TestDataValue(0, "Zero value", TestDataScenario.BOUNDARY));
            scenarios.add(new TestDataValue(Integer.MAX_VALUE, "Max value", TestDataScenario.BOUNDARY));
            scenarios.add(new TestDataValue(Integer.MIN_VALUE, "Min value", TestDataScenario.BOUNDARY));
            scenarios.add(new TestDataValue(-1, "Negative value", TestDataScenario.NEGATIVE));
        } else if (type == Long.class || type == long.class) {
            scenarios.add(new TestDataValue(0L, "Zero value", TestDataScenario.BOUNDARY));
            scenarios.add(new TestDataValue(Long.MAX_VALUE, "Max value", TestDataScenario.BOUNDARY));
            scenarios.add(new TestDataValue(Long.MIN_VALUE, "Min value", TestDataScenario.BOUNDARY));
        }
        
        return scenarios;
    }

    public TestDataValue generateEmpty(Class<?> type) {
        if (type == List.class) {
            return new TestDataValue(new ArrayList<>(), "Empty list", TestDataScenario.EMPTY);
        } else if (type == Map.class) {
            return new TestDataValue(new HashMap<>(), "Empty map", TestDataScenario.EMPTY);
        } else if (type == String.class) {
            return new TestDataValue("", "Empty string", TestDataScenario.EMPTY);
        }
        return TestDataValue.empty("Empty " + type.getSimpleName());
    }

    public TestDataValue generateNull(Class<?> type) {
        return TestDataValue.nullValue();
    }

    public List<TestDataValue> generateBoundaries(Class<?> type) {
        List<TestDataValue> boundaries = new ArrayList<>();
        
        if (type == Integer.class || type == int.class) {
            boundaries.add(new TestDataValue(Integer.MIN_VALUE, "Min int", TestDataScenario.BOUNDARY));
            boundaries.add(new TestDataValue(Integer.MAX_VALUE, "Max int", TestDataScenario.BOUNDARY));
            boundaries.add(new TestDataValue(0, "Zero", TestDataScenario.BOUNDARY));
            boundaries.add(new TestDataValue(-1, "Negative one", TestDataScenario.BOUNDARY));
            boundaries.add(new TestDataValue(1, "Positive one", TestDataScenario.BOUNDARY));
        } else if (type == Long.class || type == long.class) {
            boundaries.add(new TestDataValue(Long.MIN_VALUE, "Min long", TestDataScenario.BOUNDARY));
            boundaries.add(new TestDataValue(Long.MAX_VALUE, "Max long", TestDataScenario.BOUNDARY));
            boundaries.add(new TestDataValue(0L, "Zero", TestDataScenario.BOUNDARY));
        } else if (type == Double.class || type == double.class) {
            boundaries.add(new TestDataValue(Double.MIN_VALUE, "Min double", TestDataScenario.BOUNDARY));
            boundaries.add(new TestDataValue(Double.MAX_VALUE, "Max double", TestDataScenario.BOUNDARY));
            boundaries.add(new TestDataValue(0.0, "Zero", TestDataScenario.BOUNDARY));
            boundaries.add(new TestDataValue(Double.NaN, "NaN", TestDataScenario.BOUNDARY));
            boundaries.add(new TestDataValue(Double.POSITIVE_INFINITY, "Positive infinity", TestDataScenario.BOUNDARY));
            boundaries.add(new TestDataValue(Double.NEGATIVE_INFINITY, "Negative infinity", TestDataScenario.BOUNDARY));
        } else if (type == String.class) {
            boundaries.add(new TestDataValue("", "Empty string", TestDataScenario.BOUNDARY));
            boundaries.add(new TestDataValue(" ", "Whitespace", TestDataScenario.BOUNDARY));
            boundaries.add(new TestDataValue("a".repeat(1000), "Long string", TestDataScenario.BOUNDARY));
        }
        
        return boundaries;
    }

    public TestDataValue generateForClass(ClassInfo classInfo) {
        try {
            Class<?> clazz = Class.forName(classInfo.fullyQualifiedName());
            Object instance = createInstance(clazz, classInfo);
            return new TestDataValue(instance, "Generated " + classInfo.className(), TestDataScenario.NORMAL);
        } catch (Exception e) {
            logger.warn("Failed to generate instance for {}: {}", classInfo.fullyQualifiedName(), e.getMessage());
            return TestDataValue.empty("Could not generate " + classInfo.className());
        }
    }

    private Object createInstance(Class<?> clazz, ClassInfo classInfo) throws Exception {
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();
            
            for (FieldInfo field : classInfo.fields()) {
                try {
                    java.lang.reflect.Field declaredField = clazz.getDeclaredField(field.name());
                    declaredField.setAccessible(true);
                    Class<?> fieldType = declaredField.getType();
                    TestDataValue fieldValue = generate(fieldType);
                    declaredField.set(instance, fieldValue.value());
                } catch (NoSuchFieldException ignored) {
                }
            }
            
            return instance;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public List<TestDataValue> generateForParameters(MethodInfo method) {
        List<TestDataValue> params = new ArrayList<>();
        for (ParameterInfo param : method.parameters()) {
            Class<?> paramType = resolveType(param.type());
            params.add(generate(paramType));
        }
        return params;
    }

    public List<List<TestDataValue>> generateParameterCombinations(MethodInfo method, int count) {
        List<List<TestDataValue>> combinations = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            List<TestDataValue> params = generateForParameters(method);
            combinations.add(params);
        }
        
        return combinations;
    }

    public void registerGenerator(Class<?> type, Supplier<TestDataValue> generator) {
        generators.put(type, generator);
    }

    public void setNullProbability(double probability) {
        this.nullProbability = Math.max(0.0, Math.min(1.0, probability));
    }

    private Class<?> resolveType(String typeName) {
        return switch (typeName) {
            case "int" -> int.class;
            case "long" -> long.class;
            case "double" -> double.class;
            case "float" -> float.class;
            case "boolean" -> boolean.class;
            case "byte" -> byte.class;
            case "short" -> short.class;
            case "char" -> char.class;
            case "java.lang.String" -> String.class;
            case "java.lang.Integer" -> Integer.class;
            case "java.lang.Long" -> Long.class;
            case "java.lang.Double" -> Double.class;
            case "java.lang.Boolean" -> Boolean.class;
            case "java.util.List" -> List.class;
            case "java.util.Map" -> Map.class;
            default -> {
                try {
                    yield Class.forName(typeName);
                } catch (ClassNotFoundException e) {
                    yield Object.class;
                }
            }
        };
    }

    private TestDataValue generateUnknownType(Class<?> type) {
        try {
            Object instance = type.getDeclaredConstructor().newInstance();
            return new TestDataValue(instance, "Generated " + type.getSimpleName(), TestDataScenario.NORMAL);
        } catch (Exception e) {
            return TestDataValue.nullValue();
        }
    }

    private TestDataValue generateString() {
        int length = random.nextInt(20) + 1;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((char) (random.nextInt(26) + 'a'));
        }
        return new TestDataValue(sb.toString(), "Random string", TestDataScenario.NORMAL);
    }

    private TestDataValue generateInteger() {
        int value = random.nextInt();
        return new TestDataValue(value, "Random integer", TestDataScenario.NORMAL);
    }

    private TestDataValue generateLong() {
        long value = random.nextLong();
        return new TestDataValue(value, "Random long", TestDataScenario.NORMAL);
    }

    private TestDataValue generateDouble() {
        double value = random.nextDouble();
        return new TestDataValue(value, "Random double", TestDataScenario.NORMAL);
    }

    private TestDataValue generateFloat() {
        float value = random.nextFloat();
        return new TestDataValue(value, "Random float", TestDataScenario.NORMAL);
    }

    private TestDataValue generateBoolean() {
        boolean value = random.nextBoolean();
        return new TestDataValue(value, "Random boolean", TestDataScenario.NORMAL);
    }

    private TestDataValue generateByte() {
        byte value = (byte) random.nextInt();
        return new TestDataValue(value, "Random byte", TestDataScenario.NORMAL);
    }

    private TestDataValue generateShort() {
        short value = (short) random.nextInt();
        return new TestDataValue(value, "Random short", TestDataScenario.NORMAL);
    }

    private TestDataValue generateCharacter() {
        char value = (char) (random.nextInt(26) + 'a');
        return new TestDataValue(value, "Random character", TestDataScenario.NORMAL);
    }

    private TestDataValue generateBigDecimal() {
        BigDecimal value = BigDecimal.valueOf(random.nextDouble());
        return new TestDataValue(value, "Random BigDecimal", TestDataScenario.NORMAL);
    }

    private TestDataValue generateBigInteger() {
        BigInteger value = BigInteger.valueOf(random.nextLong());
        return new TestDataValue(value, "Random BigInteger", TestDataScenario.NORMAL);
    }

    private TestDataValue generateLocalDate() {
        long minDay = LocalDate.of(2000, 1, 1).toEpochDay();
        long maxDay = LocalDate.of(2030, 12, 31).toEpochDay();
        long randomDay = minDay + random.nextLong(maxDay - minDay);
        LocalDate value = LocalDate.ofEpochDay(randomDay);
        return new TestDataValue(value, "Random LocalDate", TestDataScenario.NORMAL);
    }

    private TestDataValue generateLocalDateTime() {
        LocalDate date = (LocalDate) generateLocalDate().value();
        LocalTime time = (LocalTime) generateLocalTime().value();
        LocalDateTime value = LocalDateTime.of(date, time);
        return new TestDataValue(value, "Random LocalDateTime", TestDataScenario.NORMAL);
    }

    private TestDataValue generateLocalTime() {
        int hour = random.nextInt(24);
        int minute = random.nextInt(60);
        int second = random.nextInt(60);
        LocalTime value = LocalTime.of(hour, minute, second);
        return new TestDataValue(value, "Random LocalTime", TestDataScenario.NORMAL);
    }

    private TestDataValue generateUUID() {
        UUID value = UUID.randomUUID();
        return new TestDataValue(value, "Random UUID", TestDataScenario.NORMAL);
    }

    private TestDataValue generateList() {
        List<Object> value = new ArrayList<>();
        int size = random.nextInt(5) + 1;
        for (int i = 0; i < size; i++) {
            value.add(generateString().value());
        }
        return new TestDataValue(value, "Random list", TestDataScenario.NORMAL);
    }

    private TestDataValue generateMap() {
        Map<Object, Object> value = new HashMap<>();
        int size = random.nextInt(5) + 1;
        for (int i = 0; i < size; i++) {
            value.put(generateString().value(), generateString().value());
        }
        return new TestDataValue(value, "Random map", TestDataScenario.NORMAL);
    }
}
