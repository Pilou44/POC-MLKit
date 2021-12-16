package com.wechantloup.facedetection.photoDisplay

import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.otaliastudios.zoom.ZoomEngine
import com.otaliastudios.zoom.ZoomLayout
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
    private var processTime: Long = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDisplayPhotoBinding.inflate(layoutInflater)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = (activity as GalleryActivity?)?.viewModel

        subscribeToUpdates()

        (binding?.zoomLayout as ZoomLayout).engine.addListener(
            object: ZoomEngine.Listener {
                override fun onIdle(engine: ZoomEngine) {
                    checkVisibleFaces()
                }

                override fun onUpdate(engine: ZoomEngine, matrix: Matrix) {
                    // Nothing to do
                }
            }
        )
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun checkVisibleFaces() {
        val binding = binding ?: return
        val engine = binding.zoomLayout.engine
        val faces = binding.imageContainer.children.toList().filterIsInstance<FaceAnalyzeOverlay>()

        var info = "Process duration: $processTime ms\n${faces.size} faces detected"

        val imageFactor = binding.ivPhoto.width.toFloat() / engine.computeHorizontalScrollRange().toFloat()

        var hStart = engine.computeHorizontalScrollOffset()
        if (hStart < 0) hStart = 0
        val hEnd = hStart + binding.zoomLayout.width
        var vStart = engine.computeVerticalScrollOffset()
        if (vStart < 0) vStart = 0
        val vEnd = vStart + binding.zoomLayout.height

        faces.forEachIndexed { index, face ->
            val faceTop = (face.top / imageFactor).toInt()
            val faceLeft = (face.left / imageFactor).toInt()
            val faceRight = (face.right / imageFactor).toInt()
            val faceBottom = (face.bottom / imageFactor).toInt()
            val isTopLeftVisible = faceTop in vStart until vEnd && faceLeft in hStart until hEnd
            val isBottomRightVisible = faceBottom in vStart until vEnd && faceRight in hStart until hEnd

            info += when {
                isTopLeftVisible && isBottomRightVisible -> "\nFace $index is totally visible"
                isTopLeftVisible || isBottomRightVisible -> "\nFace $index is partially visible"
                else -> "\nFace $index is not visible"
            }
        }

        binding.tvInfo.text = info
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
            val startTime = System.currentTimeMillis()
            val image = InputImage.fromFilePath(requireContext(), Uri.parse(photo.uri))
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
            val detector = FaceDetection.getClient(options)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    val endTime = System.currentTimeMillis()
                    processTime = endTime - startTime
                    Log.i("DisplayPhotoFragment", "${faces.size} faces detected")
                    faces.forEach { it.analyze(photo) }
                    binding?.imageContainer?.children?.last()?.post {
                        checkVisibleFaces()
                    }
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

            val view = FaceAnalyzeOverlay(requireContext())
            binding.imageContainer.addView(view)
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
