package com.utagent.testdata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BoundaryValueGenerator Tests")
class BoundaryValueGeneratorTest {

    private BoundaryValueGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new BoundaryValueGenerator();
    }

    @Nested
    @DisplayName("Integer Boundary Generation")
    class IntegerBoundaryGeneration {

        @Test
        @DisplayName("Should generate boundary values for Integer")
        void shouldGenerateBoundaryValuesForInteger() {
            List<Object> boundaries = generator.generate(Integer.class);
            
            assertThat(boundaries).isNotEmpty();
            assertThat(boundaries).contains(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, -1, 1);
        }

        @Test
        @DisplayName("Should generate boundary values for primitive int")
        void shouldGenerateBoundaryValuesForPrimitiveInt() {
            List<Object> boundaries = generator.generate(int.class);
            
            assertThat(boundaries).isNotEmpty();
            assertThat(boundaries).contains(Integer.MIN_VALUE, Integer.MAX_VALUE, 0);
        }
    }

    @Nested
    @DisplayName("Long Boundary Generation")
    class LongBoundaryGeneration {

        @Test
        @DisplayName("Should generate boundary values for Long")
        void shouldGenerateBoundaryValuesForLong() {
            List<Object> boundaries = generator.generate(Long.class);
            
            assertThat(boundaries).isNotEmpty();
            assertThat(boundaries).contains(Long.MIN_VALUE, Long.MAX_VALUE, 0L, -1L, 1L);
        }

        @Test
        @DisplayName("Should generate boundary values for primitive long")
        void shouldGenerateBoundaryValuesForPrimitiveLong() {
            List<Object> boundaries = generator.generate(long.class);
            
            assertThat(boundaries).isNotEmpty();
            assertThat(boundaries).contains(Long.MIN_VALUE, Long.MAX_VALUE, 0L);
        }
    }

    @Nested
    @DisplayName("Double Boundary Generation")
    class DoubleBoundaryGeneration {

        @Test
        @DisplayName("Should generate boundary values for Double")
        void shouldGenerateBoundaryValuesForDouble() {
            List<Object> boundaries = generator.generate(Double.class);
            
            assertThat(boundaries).isNotEmpty();
            assertThat(boundaries).contains(Double.MIN_VALUE, Double.MAX_VALUE, 0.0);
        }

        @Test
        @DisplayName("Should include special double values")
        void shouldIncludeSpecialDoubleValues() {
            List<Object> boundaries = generator.generate(Double.class);
            
            assertThat(boundaries).contains(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
        }
    }

    @Nested
    @DisplayName("Float Boundary Generation")
    class FloatBoundaryGeneration {

        @Test
        @DisplayName("Should generate boundary values for Float")
        void shouldGenerateBoundaryValuesForFloat() {
            List<Object> boundaries = generator.generate(Float.class);
            
            assertThat(boundaries).isNotEmpty();
            assertThat(boundaries).contains(Float.MIN_VALUE, Float.MAX_VALUE, 0.0f);
        }

        @Test
        @DisplayName("Should include special float values")
        void shouldIncludeSpecialFloatValues() {
            List<Object> boundaries = generator.generate(Float.class);
            
            assertThat(boundaries).contains(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY);
        }
    }

    @Nested
    @DisplayName("String Boundary Generation")
    class StringBoundaryGeneration {

        @Test
        @DisplayName("Should generate boundary values for String")
        void shouldGenerateBoundaryValuesForString() {
            List<Object> boundaries = generator.generate(String.class);
            
            assertThat(boundaries).isNotEmpty();
            assertThat(boundaries).contains("", " ");
            assertThat(boundaries).containsNull();
        }

        @Test
        @DisplayName("Should include whitespace variations")
        void shouldIncludeWhitespaceVariations() {
            List<Object> boundaries = generator.generate(String.class);
            
            assertThat(boundaries).contains(" ", "  ", "\t", "\n", "\r\n");
        }

        @Test
        @DisplayName("Should include special characters")
        void shouldIncludeSpecialCharacters() {
            List<Object> boundaries = generator.generate(String.class);
            
            assertThat(boundaries).anyMatch(s -> s instanceof String && ((String) s).contains("<"));
            assertThat(boundaries).anyMatch(s -> s instanceof String && ((String) s).contains(">"));
            assertThat(boundaries).anyMatch(s -> s instanceof String && ((String) s).contains("\""));
        }

        @Test
        @DisplayName("Should include long string")
        void shouldIncludeLongString() {
            List<Object> boundaries = generator.generate(String.class);
            
            assertThat(boundaries).anyMatch(s -> s instanceof String && ((String) s).length() > 100);
        }
    }

    @Nested
    @DisplayName("Boolean Boundary Generation")
    class BooleanBoundaryGeneration {

        @Test
        @DisplayName("Should generate boundary values for Boolean")
        void shouldGenerateBoundaryValuesForBoolean() {
            List<Object> boundaries = generator.generate(Boolean.class);
            
            assertThat(boundaries).containsExactlyInAnyOrder(true, false, null);
        }

        @Test
        @DisplayName("Should generate boundary values for primitive boolean")
        void shouldGenerateBoundaryValuesForPrimitiveBoolean() {
            List<Object> boundaries = generator.generate(boolean.class);
            
            assertThat(boundaries).containsExactlyInAnyOrder(true, false);
        }
    }

    @Nested
    @DisplayName("BigDecimal and BigInteger Generation")
    class BigDecimalAndBigIntegerGeneration {

        @Test
        @DisplayName("Should generate boundary values for BigDecimal")
        void shouldGenerateBoundaryValuesForBigDecimal() {
            List<Object> boundaries = generator.generate(BigDecimal.class);
            
            assertThat(boundaries).isNotEmpty();
            assertThat(boundaries).contains(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN);
            assertThat(boundaries).contains(BigDecimal.valueOf(-1));
        }

        @Test
        @DisplayName("Should generate boundary values for BigInteger")
        void shouldGenerateBoundaryValuesForBigInteger() {
            List<Object> boundaries = generator.generate(BigInteger.class);
            
            assertThat(boundaries).isNotEmpty();
            assertThat(boundaries).contains(BigInteger.ZERO, BigInteger.ONE, BigInteger.TEN);
            assertThat(boundaries).contains(BigInteger.valueOf(-1));
        }
    }

    @Nested
    @DisplayName("Byte and Short Generation")
    class ByteAndShortGeneration {

        @Test
        @DisplayName("Should generate boundary values for Byte")
        void shouldGenerateBoundaryValuesForByte() {
            List<Object> boundaries = generator.generate(Byte.class);
            
            assertThat(boundaries).contains(Byte.MIN_VALUE, Byte.MAX_VALUE, (byte) 0, (byte) -1, (byte) 1);
        }

        @Test
        @DisplayName("Should generate boundary values for Short")
        void shouldGenerateBoundaryValuesForShort() {
            List<Object> boundaries = generator.generate(Short.class);
            
            assertThat(boundaries).contains(Short.MIN_VALUE, Short.MAX_VALUE, (short) 0, (short) -1, (short) 1);
        }
    }

    @Nested
    @DisplayName("Character Generation")
    class CharacterGeneration {

        @Test
        @DisplayName("Should generate boundary values for Character")
        void shouldGenerateBoundaryValuesForCharacter() {
            List<Object> boundaries = generator.generate(Character.class);
            
            assertThat(boundaries).isNotEmpty();
            assertThat(boundaries).contains(Character.MIN_VALUE, Character.MAX_VALUE, 'a', 'A', '0');
        }

        @Test
        @DisplayName("Should include special characters")
        void shouldIncludeSpecialCharacters() {
            List<Object> boundaries = generator.generate(Character.class);
            
            assertThat(boundaries).contains(' ', '\t', '\n', '\0');
        }
    }

    @Nested
    @DisplayName("Custom Range Generation")
    class CustomRangeGeneration {

        @Test
        @DisplayName("Should generate boundaries within custom range")
        void shouldGenerateBoundariesWithinCustomRange() {
            List<Object> boundaries = generator.generateInRange(Integer.class, 0, 100);
            
            assertThat(boundaries).contains(0, 100, 1, 99, 50);
            assertThat(boundaries).doesNotContain(Integer.MIN_VALUE, Integer.MAX_VALUE);
        }

        @Test
        @DisplayName("Should handle negative range")
        void shouldHandleNegativeRange() {
            List<Object> boundaries = generator.generateInRange(Integer.class, -100, -50);
            
            assertThat(boundaries).contains(-100, -50, -99, -51, -75);
        }
    }

    @Nested
    @DisplayName("Unknown Type Handling")
    class UnknownTypeHandling {

        @Test
        @DisplayName("Should return empty list for unknown types")
        void shouldReturnEmptyListForUnknownTypes() {
            List<Object> boundaries = generator.generate(CustomType.class);
            
            assertThat(boundaries).isEmpty();
        }
    }

    static class CustomType {
    }
}
