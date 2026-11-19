package com.xingheyuzhuan.shiguangschedule.xatu.campus

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions

object XatuCalendarOcr {
    fun read(bitmap: Bitmap): String {
        val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        return try {
            val text = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0))).text
            val spans = XatuCalendarParser.semesters(text)
            if (spans.size >= 2) text else {
                val crop = Bitmap.createBitmap(bitmap, 0, bitmap.height * 3 / 5, bitmap.width, bitmap.height - bitmap.height * 3 / 5)
                val bottom = try {
                    Tasks.await(recognizer.process(InputImage.fromBitmap(crop, 0))).text
                } finally {
                    if (crop !== bitmap) crop.recycle()
                }
                if (XatuCalendarParser.semesters(bottom).size > spans.size) "$text\n$bottom" else text
            }
        } finally {
            recognizer.close()
        }
    }
}
