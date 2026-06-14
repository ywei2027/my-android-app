package com.example.app.feature.register.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * 3-4-4 手机号格式化：13812345678 -> 138 1234 5678
 */
class PhoneVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter { it.isDigit() }
        val formatted = formatPhone(raw)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 3) return offset
                if (offset <= 7) return offset + 1
                return (offset + 2).coerceAtMost(formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 3) return offset
                if (offset <= 8) return offset - 1
                return (offset - 2).coerceAtMost(raw.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }

    private fun formatPhone(raw: String): String {
        return when {
            raw.length <= 3 -> raw
            raw.length <= 7 -> "${raw.substring(0, 3)} ${raw.substring(3)}"
            else -> "${raw.substring(0, 3)} ${raw.substring(3, 7)} ${raw.substring(7)}"
        }
    }
}
