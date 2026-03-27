package io.github.mobdev

import android.os.Bundle
import kotlin.math.abs

class Calculator {

    private var currentInput: String = "0"
    private var firstOperand: Double? = null
    private var operator: String? = null
    private var expression: String = ""
    private var justCalculated: Boolean = false
    private var isError: Boolean = false

    fun inputDigit(digit: String) {
        if (isError) clear()
        if (justCalculated) {
            currentInput = digit
            expression = ""
            justCalculated = false
            return
        }
        currentInput = if (currentInput == "0") digit
        else currentInput + digit
    }

    fun inputDot() {
        if (isError) clear()
        if (justCalculated) {
            currentInput = "0."
            expression = ""
            justCalculated = false
            return
        }
        if (!currentInput.contains(".")) {
            currentInput += "."
        }
    }

    fun inputOperator(op: String) {
        if (isError) return
        justCalculated = false

        val value = currentInput.toDoubleOrNull() ?: 0.0

        if (firstOperand != null && operator != null) {
            val result = compute(firstOperand!!, value, operator!!)
            if (result == null) {
                setError()
                return
            }
            firstOperand = result
            expression = formatNumber(result) + " $op "
            currentInput = "0"
        } else {
            firstOperand = value
            expression = formatNumber(value) + " $op "
            currentInput = "0"
        }
        operator = op
    }

    fun calculate() {
        if (isError) return
        val op = operator ?: return
        val first = firstOperand ?: return
        val second = currentInput.toDoubleOrNull() ?: 0.0

        val fullExpression = expression + currentInput
        val result = compute(first, second, op)

        if (result == null) {
            expression = fullExpression + " ="
            setError()
            return
        }

        expression = fullExpression + " ="
        currentInput = formatNumber(result)
        firstOperand = null
        operator = null
        justCalculated = true
    }

    fun clear() {
        currentInput = "0"
        firstOperand = null
        operator = null
        expression = ""
        justCalculated = false
        isError = false
    }

    fun backspace() {
        if (isError || justCalculated) {
            clear()
            return
        }
        if (currentInput.length <= 1 || currentInput == "-0") {
            currentInput = "0"
        } else {
            currentInput = currentInput.dropLast(1)
            if (currentInput == "-") currentInput = "0"
        }
    }

    fun toggleSign() {
        if (isError) return
        val value = currentInput.toDoubleOrNull() ?: return
        currentInput = formatNumber(-value)
    }

    fun percent() {
        if (isError) return
        val value = currentInput.toDoubleOrNull() ?: return
        currentInput = formatNumber(value / 100.0)
    }

    fun getDisplay(): String = if (isError) "Ошибка" else currentInput

    fun getExpression(): String = expression

    private fun compute(a: Double, b: Double, op: String): Double? {
        return when (op) {
            "+" -> a + b
            "-" -> a - b
            "×" -> a * b
            "÷" -> if (b == 0.0) null else a / b
            else -> null
        }
    }

    private fun setError() {
        isError = true
        currentInput = "Ошибка"
    }

    private fun formatNumber(value: Double): String {
        return if (value == kotlin.math.floor(value) && abs(value) < 1e15) {
            value.toLong().toString()
        } else {
            val s = value.toBigDecimal().stripTrailingZeros().toPlainString()
            if (s.length > 12) "%.10g".format(value).trimEnd('0').trimEnd('.') else s
        }
    }

    fun saveState(bundle: Bundle) {
        bundle.putString("currentInput", currentInput)
        bundle.putString("operator", operator)
        bundle.putString("expression", expression)
        bundle.putBoolean("justCalculated", justCalculated)
        bundle.putBoolean("isError", isError)
        if (firstOperand != null) bundle.putDouble("firstOperand", firstOperand!!)
    }

    fun restoreState(bundle: Bundle) {
        currentInput = bundle.getString("currentInput", "0")!!
        operator = bundle.getString("operator", null)
        expression = bundle.getString("expression", "")!!
        justCalculated = bundle.getBoolean("justCalculated", false)
        isError = bundle.getBoolean("isError", false)
        firstOperand = if (bundle.containsKey("firstOperand")) bundle.getDouble("firstOperand") else null
    }
}