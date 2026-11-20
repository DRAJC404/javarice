package com.bai.inventory.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class CalculatorEvaluator {

    public record EvaluationResult(BigDecimal val, Integer intValue, String error) {
        public boolean isSuccess() {
            return error == null && val != null;
        }

        public boolean isValidQuantity() {
            return isSuccess();
        }
    }

    public static EvaluationResult evaluate(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return new EvaluationResult(null, null, "Please enter an expression.");
        }
        String trimmed = expression.trim();
        trimmed = trimmed.replace('*', '×').replace('/', '÷');

        List<String> tokens = new ArrayList<>();
        StringBuilder currentNum = new StringBuilder();

        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (Character.isDigit(ch) || ch == '.') {
                currentNum.append(ch);
            } else if (ch == '+' || ch == '-' || ch == '×' || ch == '÷') {
                if (currentNum.length() > 0) {
                    tokens.add(currentNum.toString());
                    currentNum.setLength(0);
                }
                tokens.add(String.valueOf(ch));
            } else if (Character.isWhitespace(ch)) {
                if (currentNum.length() > 0) {
                    tokens.add(currentNum.toString());
                    currentNum.setLength(0);
                }
            } else {
                return new EvaluationResult(null, null, "Invalid character: " + ch);
            }
        }
        if (currentNum.length() > 0) {
            tokens.add(currentNum.toString());
        }

        if (tokens.isEmpty()) {
            return new EvaluationResult(null, null, "Please enter an expression.");
        }

        while (!tokens.isEmpty()) {
            String last = tokens.get(tokens.size() - 1);
            if (last.equals("+") || last.equals("-") || last.equals("×") || last.equals("÷")) {
                tokens.remove(tokens.size() - 1);
            } else {
                break;
            }
        }

        if (tokens.isEmpty()) {
            return new EvaluationResult(null, null, "Please enter an expression.");
        }

        List<BigDecimal> numbers = new ArrayList<>();
        List<String> ops = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            String t = tokens.get(i);
            if (i % 2 == 0) {
                try {
                    numbers.add(new BigDecimal(t));
                } catch (NumberFormatException e) {
                    return new EvaluationResult(null, null, "Invalid number: " + t);
                }
            } else {
                if (!t.equals("+") && !t.equals("-") && !t.equals("×") && !t.equals("÷")) {
                    return new EvaluationResult(null, null, "Expected operator.");
                }
                ops.add(t);
            }
        }

        if (numbers.size() != ops.size() + 1) {
            return new EvaluationResult(null, null, "Invalid expression structure.");
        }

        int i = 0;
        while (i < ops.size()) {
            String op = ops.get(i);
            if (op.equals("×") || op.equals("÷")) {
                BigDecimal left = numbers.get(i);
                BigDecimal right = numbers.get(i + 1);
                BigDecimal res;
                if (op.equals("×")) {
                    res = left.multiply(right);
                } else {
                    if (right.compareTo(BigDecimal.ZERO) == 0) {
                        return new EvaluationResult(null, null, "Cannot divide by zero.");
                    }
                    res = left.divide(right, 6, RoundingMode.HALF_UP);
                }
                numbers.set(i, res);
                numbers.remove(i + 1);
                ops.remove(i);
            } else {
                i++;
            }
        }

        i = 0;
        while (i < ops.size()) {
            String op = ops.get(i);
            BigDecimal left = numbers.get(i);
            BigDecimal right = numbers.get(i + 1);
            BigDecimal res;
            if (op.equals("+")) {
                res = left.add(right);
            } else {
                res = left.subtract(right);
            }
            numbers.set(i, res);
            numbers.remove(i + 1);
            ops.remove(i);
        }

        BigDecimal finalVal;
        if (numbers.get(0).compareTo(BigDecimal.ZERO) == 0) {
            finalVal = BigDecimal.ZERO;
        } else {
            finalVal = numbers.get(0).stripTrailingZeros();
            if (finalVal.scale() < 0) {
                finalVal = finalVal.setScale(0, RoundingMode.UNNECESSARY);
            }
            if (finalVal.scale() > 2) {
                finalVal = finalVal.setScale(2, RoundingMode.HALF_UP);
            }
        }

        Integer intVal = (finalVal.abs().compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) <= 0) ? finalVal.intValue() : null;
        return new EvaluationResult(finalVal, intVal, null);
    }
}
