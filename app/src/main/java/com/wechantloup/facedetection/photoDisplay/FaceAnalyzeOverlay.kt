package com.wechantloup.facedetection.photoDisplay

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.google.mlkit.vision.face.Face
import com.wechantloup.facedetection.databinding.FaceAnalyzeOverlayLayoutBinding

class FaceAnalyzeOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = FaceAnalyzeOverlayLayoutBinding.inflate(LayoutInflater.from(context), this, true)

    fun bind(face: Face) {
        val leftEyeValue = "${((face.leftEyeOpenProbability ?: 0f) * 100).toInt()}%"
        val rightEyeValue = "${((face.rightEyeOpenProbability ?: 0f) * 100).toInt()}%"
        val smile = "${((face.smilingProbability ?: 0f) * 100).toInt()}%"
        binding.leftEye.text = leftEyeValue
        binding.rightEye.text = rightEyeValue
        binding.smile.text = smile
    }
}