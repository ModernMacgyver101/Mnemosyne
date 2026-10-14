package com.hardlinecoding.checkthebox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalculatorDecoy(
    onUnlock: () -> Unit,
    correctPin: String?
) {
    var display by remember { mutableStateOf("0") }
    var expression by remember { mutableStateOf("") }
    var operand1 by remember { mutableStateOf<Double?>(null) }
    var operator by remember { mutableStateOf<String?>(null) }
    var shouldResetDisplay by remember { mutableStateOf(false) }

    val cOrange = Color(0xFFFF9F0A)
    val cDarkGrey = Color(0xFF333333)
    val cLightGrey = Color(0xFFA5A5A5)

    fun format(d: Double): String {
        return if (d % 1 == 0.0) d.toInt().toString() else d.toString()
    }

    fun calculate(o1: Double, o2: Double, op: String): Double {
        return when (op) {
            "+" -> o1 + o2
            "-" -> o1 - o2
            "×" -> o1 * o2
            "÷" -> if (o2 != 0.0) o1 / o2 else 0.0
            else -> o2
        }
    }

    fun onButtonClick(text: String) {
        when {
            text.all { it.isDigit() } || text == "." -> {
                if (shouldResetDisplay || display == "0") {
                    display = text
                    shouldResetDisplay = false
                } else {
                    display += text
                }
            }
            text == "AC" -> {
                display = "0"
                expression = ""
                operand1 = null
                operator = null
                shouldResetDisplay = false
            }
            text == "backspace" -> {
                if (display.length > 1) {
                    display = display.dropLast(1)
                } else {
                    display = "0"
                }
            }
            text == "+/-" -> {
                display = if (display.startsWith("-")) display.substring(1) else "-$display"
            }
            text == "%" -> {
                val value = display.toDoubleOrNull() ?: 0.0
                val result = value / 100.0
                display = format(result)
            }
            text == "=" -> {
                if (display == correctPin) {
                    onUnlock()
                    return
                }
                val o1 = operand1
                val op = operator
                if (o1 != null && op != null) {
                    val o2 = display.toDoubleOrNull() ?: 0.0
                    val result = calculate(o1, o2, op)
                    expression = "${format(o1)}$op${format(o2)}"
                    display = format(result)
                    operand1 = null
                    operator = null
                    shouldResetDisplay = true
                }
            }
            else -> { // Operators +, -, ×, ÷
                val currentVal = display.toDoubleOrNull() ?: 0.0
                if (operand1 == null) {
                    operand1 = currentVal
                } else if (operator != null) {
                    operand1 = calculate(operand1!!, currentVal, operator!!)
                }
                operator = text
                expression = "${format(operand1!!)}$text"
                shouldResetDisplay = true
                display = format(operand1!!)
            }
        }
    }

    val buttons = listOf(
        "backspace", "AC", "%", "÷",
        "7", "8", "9", "×",
        "4", "5", "6", "-",
        "1", "2", "3", "+",
        "+/-", "0", ".", "="
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = expression,
                fontSize = 24.sp,
                color = Color.Gray,
                textAlign = TextAlign.End,
                maxLines = 1
            )
            Text(
                text = display,
                fontSize = 80.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            items(buttons) { button ->
                val isOperator = button in listOf("÷", "×", "-", "+", "=")
                val isSpecial = button in listOf("backspace", "AC", "%", "+/-")
                
                Surface(
                    onClick = { onButtonClick(button) },
                    modifier = Modifier.aspectRatio(1f),
                    shape = CircleShape,
                    color = when {
                        isOperator -> cOrange
                        isSpecial -> cLightGrey
                        else -> cDarkGrey
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (button == "backspace") {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "backspace",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            Text(
                                text = button,
                                fontSize = if (button.length > 2) 20.sp else 32.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
