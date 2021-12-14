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
        binding.leftEye.text = "${face.leftEyeOpenProbability * 100}%"
    }
}