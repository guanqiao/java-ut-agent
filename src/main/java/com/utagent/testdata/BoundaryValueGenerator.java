package com.utagent.testdata;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class BoundaryValueGenerator {

    public List<Object> generate(Class<?> type) {
        List<Object> boundaries = new ArrayList<>();
        
        if (type == Integer.class || type == int.class) {
            addIntegerBoundaries(boundaries, type.isPrimitive());
        } else if (type == Long.class || type == long.class) {
            addLongBoundaries(boundaries, type.isPrimitive());
        } else if (type == Double.class || type == double.class) {
            addDoubleBoundaries(boundaries, type.isPrimitive());
        } else if (type == Float.class || type == float.class) {
            addFloatBoundaries(boundaries, type.isPrimitive());
        } else if (type == String.class) {
            addStringBoundaries(boundaries);
        } else if (type == Boolean.class || type == boolean.class) {
            addBooleanBoundaries(boundaries, type.isPrimitive());
        } else if (type == Byte.class || type == byte.class) {
            addByteBoundaries(boundaries);
        } else if (type == Short.class || type == short.class) {
            addShortBoundaries(boundaries);
        } else if (type == Character.class || type == char.class) {
            addCharacterBoundaries(boundaries);
        } else if (type == BigDecimal.class) {
            addBigDecimalBoundaries(boundaries);
        } else if (type == BigInteger.class) {
            addBigIntegerBoundaries(boundaries);
        }
        
        return boundaries;
    }

    public List<Object> generateInRange(Class<?> type, Number min, Number max) {
        List<Object> boundaries = new ArrayList<>();
        
        if (type == Integer.class || type == int.class) {
            int minVal = min.intValue();
            int maxVal = max.intValue();
            boundaries.add(minVal);
            boundaries.add(maxVal);
            boundaries.add(minVal + 1);
            boundaries.add(maxVal - 1);
            boundaries.add(minVal + (maxVal - minVal) / 2);
        } else if (type == Long.class || type == long.class) {
            long minVal = min.longValue();
            long maxVal = max.longValue();
            boundaries.add(minVal);
            boundaries.add(maxVal);
            boundaries.add(minVal + 1);
            boundaries.add(maxVal - 1);
            boundaries.add(minVal + (maxVal - minVal) / 2);
        }
        
        return boundaries;
    }

    private void addIntegerBoundaries(List<Object> boundaries, boolean isPrimitive) {
        boundaries.add(Integer.MIN_VALUE);
        boundaries.add(Integer.MAX_VALUE);
        boundaries.add(0);
        boundaries.add(-1);
        boundaries.add(1);
        boundaries.add(Integer.MIN_VALUE + 1);
        boundaries.add(Integer.MAX_VALUE - 1);
        if (!isPrimitive) {
            boundaries.add(null);
        }
    }

    private void addLongBoundaries(List<Object> boundaries, boolean isPrimitive) {
        boundaries.add(Long.MIN_VALUE);
        boundaries.add(Long.MAX_VALUE);
        boundaries.add(0L);
        boundaries.add(-1L);
        boundaries.add(1L);
        boundaries.add(Long.MIN_VALUE + 1);
        boundaries.add(Long.MAX_VALUE - 1);
        if (!isPrimitive) {
            boundaries.add(null);
        }
    }

    private void addDoubleBoundaries(List<Object> boundaries, boolean isPrimitive) {
        boundaries.add(Double.MIN_VALUE);
        boundaries.add(Double.MAX_VALUE);
        boundaries.add(0.0);
        boundaries.add(-1.0);
        boundaries.add(1.0);
        boundaries.add(Double.NaN);
        boundaries.add(Double.POSITIVE_INFINITY);
        boundaries.add(Double.NEGATIVE_INFINITY);
        boundaries.add(Double.MIN_NORMAL);
        if (!isPrimitive) {
            boundaries.add(null);
        }
    }

    private void addFloatBoundaries(List<Object> boundaries, boolean isPrimitive) {
        boundaries.add(Float.MIN_VALUE);
        boundaries.add(Float.MAX_VALUE);
        boundaries.add(0.0f);
        boundaries.add(-1.0f);
        boundaries.add(1.0f);
        boundaries.add(Float.NaN);
        boundaries.add(Float.POSITIVE_INFINITY);
        boundaries.add(Float.NEGATIVE_INFINITY);
        boundaries.add(Float.MIN_NORMAL);
        if (!isPrimitive) {
            boundaries.add(null);
        }
    }

    private void addStringBoundaries(List<Object> boundaries) {
        boundaries.add(null);
        boundaries.add("");
        boundaries.add(" ");
        boundaries.add("  ");
        boundaries.add("\t");
        boundaries.add("\n");
        boundaries.add("\r\n");
        boundaries.add("a");
        boundaries.add("A");
        boundaries.add("0");
        boundaries.add("<script>");
        boundaries.add("</script>");
        boundaries.add("<html>");
        boundaries.add("\"test\"");
        boundaries.add("'test'");
        boundaries.add("test\0null");
        boundaries.add("日本語");
        boundaries.add("🎉");
        boundaries.add("a".repeat(1000));
        boundaries.add("a".repeat(10000));
    }

    private void addBooleanBoundaries(List<Object> boundaries, boolean isPrimitive) {
        boundaries.add(true);
        boundaries.add(false);
        if (!isPrimitive) {
            boundaries.add(null);
        }
    }

    private void addByteBoundaries(List<Object> boundaries) {
        boundaries.add(Byte.MIN_VALUE);
        boundaries.add(Byte.MAX_VALUE);
        boundaries.add((byte) 0);
        boundaries.add((byte) -1);
        boundaries.add((byte) 1);
        boundaries.add((byte) (Byte.MIN_VALUE + 1));
        boundaries.add((byte) (Byte.MAX_VALUE - 1));
        boundaries.add(null);
    }

    private void addShortBoundaries(List<Object> boundaries) {
        boundaries.add(Short.MIN_VALUE);
        boundaries.add(Short.MAX_VALUE);
        boundaries.add((short) 0);
        boundaries.add((short) -1);
        boundaries.add((short) 1);
        boundaries.add((short) (Short.MIN_VALUE + 1));
        boundaries.add((short) (Short.MAX_VALUE - 1));
        boundaries.add(null);
    }

    private void addCharacterBoundaries(List<Object> boundaries) {
        boundaries.add(Character.MIN_VALUE);
        boundaries.add(Character.MAX_VALUE);
        boundaries.add('a');
        boundaries.add('A');
        boundaries.add('0');
        boundaries.add(' ');
        boundaries.add('\t');
        boundaries.add('\n');
        boundaries.add('\r');
        boundaries.add('\0');
        boundaries.add('_');
        boundaries.add('-');
        boundaries.add('.');
        boundaries.add('/');
        boundaries.add('\\');
        boundaries.add('@');
        boundaries.add('#');
        boundaries.add('$');
        boundaries.add('%');
        boundaries.add('^');
        boundaries.add('&');
        boundaries.add('*');
        boundaries.add('(');
        boundaries.add(')');
        boundaries.add('中');
    }

    private void addBigDecimalBoundaries(List<Object> boundaries) {
        boundaries.add(BigDecimal.ZERO);
        boundaries.add(BigDecimal.ONE);
        boundaries.add(BigDecimal.TEN);
        boundaries.add(BigDecimal.valueOf(-1));
        boundaries.add(BigDecimal.valueOf(Long.MAX_VALUE));
        boundaries.add(BigDecimal.valueOf(Long.MIN_VALUE));
        boundaries.add(new BigDecimal("0.000000001"));
        boundaries.add(new BigDecimal("999999999999999999999999999999.999999999999999999999999999999"));
        boundaries.add(null);
    }

    private void addBigIntegerBoundaries(List<Object> boundaries) {
        boundaries.add(BigInteger.ZERO);
        boundaries.add(BigInteger.ONE);
        boundaries.add(BigInteger.TEN);
        boundaries.add(BigInteger.valueOf(-1));
        boundaries.add(BigInteger.valueOf(Long.MAX_VALUE));
        boundaries.add(BigInteger.valueOf(Long.MIN_VALUE));
        boundaries.add(new BigInteger("99999999999999999999999999999999999999999999999999"));
        boundaries.add(null);
    }
}
