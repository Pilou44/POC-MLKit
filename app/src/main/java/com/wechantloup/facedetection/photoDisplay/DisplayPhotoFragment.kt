package com.wechantloup.facedetection.photoDisplay

import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.wechantloup.facedetection.GalleryActivity
import com.wechantloup.facedetection.GalleryViewModel
import com.wechantloup.facedetection.databinding.FragmentDisplayPhotoBinding
import com.wechantloup.facedetection.provider.Photo
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.IOException

class DisplayPhotoFragment : Fragment() {

    private var binding: FragmentDisplayPhotoBinding? = null

    private var viewModel: GalleryViewModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDisplayPhotoBinding.inflate(layoutInflater)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = (activity as GalleryActivity?)?.viewModel

        subscribeToUpdates()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun subscribeToUpdates() {
        val ivPhoto = binding?.ivPhoto?: return
        viewModel?.stateFlow
            ?.flowWithLifecycle(lifecycle)
            ?.onEach { item ->
                val photo = item.selectedPhoto ?: return@onEach
                Glide.with(requireContext())
                    .load(photo.uri)
                    .into(ivPhoto)
                detectFaces(photo)
            }
            ?.launchIn(lifecycleScope)
    }

    private fun detectFaces(photo: Photo) {
        try {
            val image = InputImage.fromFilePath(requireContext(), Uri.parse(photo.uri))
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
            val detector = FaceDetection.getClient(options)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    Log.i("DisplayPhotoFragment", "${faces.size} faces detected")
                    faces.forEach { it.analyze(photo) }
                }
                .addOnFailureListener { e ->
                    Log.e("DisplayPhotoFragment", "Error on detecting faces", e)
                }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun Face.analyze(photo: Photo) {
        val binding = binding ?: return
        val bounds = boundingBox
        val face = this

        binding.ivPhoto.post {
            val factor = binding.ivPhoto.getFactor(photo)
            val imageVerticalOffset = binding.ivPhoto.getVerticalOffset(photo)
            val imageHorizontalOffset = binding.ivPhoto.getHorizontalOffset(photo)

            val updatedBounds = RectF(
                bounds.left * factor,
                bounds.top * factor,
                bounds.right * factor,
                bounds.bottom * factor,
            )
            binding.root.apply {
                val view = FaceAnalyzeOverlay(requireContext())
                addView(view)
                view.bind(face)
                val lp = view.layoutParams as FrameLayout.LayoutParams
                lp.width = updatedBounds.width().toInt()
                lp.height = updatedBounds.height().toInt()
                lp.setMargins(
                    updatedBounds.left.toInt() + imageHorizontalOffset,
                    updatedBounds.top.toInt() + imageVerticalOffset,
                    0,
                    0,
                )
                view.layoutParams = lp
            }
        }

//        Log.i("DisplayPhotoFragment", "Position: $bounds")
//        Log.i("DisplayPhotoFragment", "Smiling probability: $smilingProbability")
//        Log.i("DisplayPhotoFragment", "Left eye opened: $leftEyeOpenProbability")
//        Log.i("DisplayPhotoFragment", "Right eye opened: $rightEyeOpenProbability")
    }

    private fun AppCompatImageView.getFactor(photo: Photo): Float {
        return width.toFloat() / photo.widthPx.toFloat()
    }

    private fun AppCompatImageView.getHorizontalOffset(photo: Photo): Int {
        val photoRatio = photo.widthPx.toFloat() / photo.heightPx.toFloat()
        val ivRatio = width.toFloat() / height.toFloat()

        if (ivRatio < photoRatio) return 0

        val displayedWidth = height * photo.widthPx / photo.heightPx

        return (width - displayedWidth) / 2
    }

    private fun AppCompatImageView.getVerticalOffset(photo: Photo): Int {
        val photoRatio = photo.widthPx.toFloat() / photo.heightPx.toFloat()
        val ivRatio = width.toFloat() / height.toFloat()

        if (ivRatio > photoRatio) return 0

        val displayedHeight = width * photo.heightPx / photo.widthPx

        return (height - displayedHeight) / 2
    }
}
