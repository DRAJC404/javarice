package com.bai.inventory.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorEvaluatorTest {

    @Test
    @DisplayName("Evaluate simple addition")
    void testSimpleAddition() {
        CalculatorEvaluator.EvaluationResult res = CalculatorEvaluator.evaluate("10 + 25");
        assertTrue(res.isSuccess());
        assertTrue(res.isValidQuantity());
        assertEquals(new BigDecimal("35"), res.val());
        assertEquals(35, res.intValue());
    }

    @Test
    @DisplayName("Evaluate operator precedence: multiplication before addition")
    void testOperatorPrecedenceMultiplyFirst() {
        CalculatorEvaluator.EvaluationResult res = CalculatorEvaluator.evaluate("3 + 2 × 4");
        assertTrue(res.isSuccess());
        assertTrue(res.isValidQuantity());
        assertEquals(new BigDecimal("11"), res.val());
        assertEquals(11, res.intValue());
    }

    @Test
    @DisplayName("Evaluate operator precedence: division before subtraction")
    void testOperatorPrecedenceDivideFirst() {
        CalculatorEvaluator.EvaluationResult res = CalculatorEvaluator.evaluate("20 - 12 ÷ 3");
        assertTrue(res.isSuccess());
        assertTrue(res.isValidQuantity());
        assertEquals(new BigDecimal("16"), res.val());
        assertEquals(16, res.intValue());
    }

    @Test
    @DisplayName("Evaluate trailing operator automatically stripped")
    void testTrailingOperatorStripped() {
        CalculatorEvaluator.EvaluationResult res = CalculatorEvaluator.evaluate("5 + 3 ×");
        assertTrue(res.isSuccess());
        assertTrue(res.isValidQuantity());
        assertEquals(new BigDecimal("8"), res.val());
        assertEquals(8, res.intValue());
    }

    @Test
    @DisplayName("Division by zero returns error")
    void testDivisionByZero() {
        CalculatorEvaluator.EvaluationResult res = CalculatorEvaluator.evaluate("10 ÷ 0");
        assertFalse(res.isSuccess());
        assertEquals("Cannot divide by zero.", res.error());
    }

    @Test
    @DisplayName("Decimal result evaluates successfully")
    void testDecimalResult() {
        CalculatorEvaluator.EvaluationResult res = CalculatorEvaluator.evaluate("5 ÷ 2");
        assertTrue(res.isSuccess());
        assertEquals(new BigDecimal("2.5"), res.val());
    }

    @Test
    @DisplayName("Zero or negative result evaluates successfully")
    void testZeroOrNegativeResult() {
        CalculatorEvaluator.EvaluationResult zeroRes = CalculatorEvaluator.evaluate("5 - 5");
        assertTrue(zeroRes.isSuccess());
        assertEquals(BigDecimal.ZERO, zeroRes.val());

        CalculatorEvaluator.EvaluationResult negRes = CalculatorEvaluator.evaluate("3 - 8");
        assertTrue(negRes.isSuccess());
        assertEquals(new BigDecimal("-5"), negRes.val());
    }
}
