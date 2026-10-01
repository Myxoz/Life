package com.myxoz.life

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class MoneyBasedVisualTransformation: VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val anotatedText = toTransformed(text.text)
        return TransformedText(
            AnnotatedString(anotatedText),
            MoneyBasedOffsetMapping(anotatedText, text.text)
        )
    }
    class MoneyBasedOffsetMapping(val transformed: String, val original: String): OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            // 1005
            // to 10,05 €
            val dist = original.length - offset
            val res = transformed.length - when(dist){ // 0  is at the end
                0 -> 2
                1 -> 3
                else -> dist + 3
            }
            return res
        }

        override fun transformedToOriginal(offset: Int): Int {
            // 10,05 €
            // to 1005
            val dist = transformed.length - offset
            val res = (original.length - when(dist){ // 0  is at the end
                0 -> 0 // 10,05 €| -> 1005|
                1 -> 0 // 10,05 |€ -> 1005|
                2 -> 0 // 10,05| € -> 1005|
                3 -> 1 // 10,0|5 € -> 100|5
                4 -> 2 // 10,|05 € -> 10|05
                else -> dist - 3
            }).coerceIn(0, original.length) // Bad practice-ish
            return res
        }
    }
    companion object {
        fun toTransformed(text: String): String{
            return if(text.length > 2) "${text.take(text.length-2)},${text.takeLast(2)} €" else "0,${text.takeLast(2).padStart(2,'0')} €"
        }
        fun MoneyBasedOffsetMapping.testTransformed(res: Int, offset: Int){
            println("${transformed.take(offset)}|${transformed.substring(offset)} -> ${original.take(res)}|${original.substring(res)}")
        }
        fun MoneyBasedOffsetMapping.testOriginal(res: Int, offset: Int){
            println("${original.take(offset)}|${original.substring(offset)} -> ${transformed.take(res)}|${transformed.substring(res)}")
        }
    }
}
