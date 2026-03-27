package io.github.mobdev

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.mobdev.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val calculator = Calculator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) {
            calculator.restoreState(savedInstanceState)
        }

        updateDisplay()
        setupButtons()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        calculator.saveState(outState)
    }

    private fun setupButtons() {
        binding.btn0.setOnClickListener { calculator.inputDigit("0"); updateDisplay() }
        binding.btn1.setOnClickListener { calculator.inputDigit("1"); updateDisplay() }
        binding.btn2.setOnClickListener { calculator.inputDigit("2"); updateDisplay() }
        binding.btn3.setOnClickListener { calculator.inputDigit("3"); updateDisplay() }
        binding.btn4.setOnClickListener { calculator.inputDigit("4"); updateDisplay() }
        binding.btn5.setOnClickListener { calculator.inputDigit("5"); updateDisplay() }
        binding.btn6.setOnClickListener { calculator.inputDigit("6"); updateDisplay() }
        binding.btn7.setOnClickListener { calculator.inputDigit("7"); updateDisplay() }
        binding.btn8.setOnClickListener { calculator.inputDigit("8"); updateDisplay() }
        binding.btn9.setOnClickListener { calculator.inputDigit("9"); updateDisplay() }
        binding.btnDot.setOnClickListener { calculator.inputDot(); updateDisplay() }

        binding.btnPlus.setOnClickListener { calculator.inputOperator("+"); updateDisplay() }
        binding.btnMinus.setOnClickListener { calculator.inputOperator("-"); updateDisplay() }
        binding.btnMultiply.setOnClickListener { calculator.inputOperator("×"); updateDisplay() }
        binding.btnDivide.setOnClickListener { calculator.inputOperator("÷"); updateDisplay() }

        binding.btnEquals.setOnClickListener { calculator.calculate(); updateDisplay() }
        binding.btnClear.setOnClickListener { calculator.clear(); updateDisplay() }
        binding.btnBackspace.setOnClickListener { calculator.backspace(); updateDisplay() }
        binding.btnPlusMinus.setOnClickListener { calculator.toggleSign(); updateDisplay() }
        binding.btnPercent.setOnClickListener { calculator.percent(); updateDisplay() }
    }

    private fun updateDisplay() {
        binding.tvExpression.text = calculator.getExpression()
        binding.tvResult.text = calculator.getDisplay()
    }
}