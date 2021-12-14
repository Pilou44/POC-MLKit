package com.wechantloup.facedetection.photoDisplay

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
                val photoUri = item.selectedPhoto?.uri ?: return@onEach
                Glide.with(requireContext())
                    .load(photoUri)
                    .into(ivPhoto)
                detectFaces(photoUri)
            }
            ?.launchIn(lifecycleScope)
    }

    private fun detectFaces(photoUri: String) {
        try {
            val image = InputImage.fromFilePath(requireContext(), Uri.parse(photoUri))
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
            val detector = FaceDetection.getClient(options)
            val result = detector.process(image)
                .addOnSuccessListener { faces ->
                    Log.i("DisplayPhotoFragment", "${faces.size} faces detected")
                    faces.forEach { it.analyze() }
                }
                .addOnFailureListener { e ->
                    Log.e("DisplayPhotoFragment", "Error on detecting faces", e)
                }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun Face.analyze() {
        Log.i("DisplayPhotoFragment", "New face detected")
        val bounds = boundingBox
        Log.i("DisplayPhotoFragment", "Position: $bounds")
        Log.i("DisplayPhotoFragment", "Smiling probability: $smilingProbability")
        Log.i("DisplayPhotoFragment", "Left eye opened: $leftEyeOpenProbability")
        Log.i("DisplayPhotoFragment", "Right eye opened: $rightEyeOpenProbability")
    }
}
