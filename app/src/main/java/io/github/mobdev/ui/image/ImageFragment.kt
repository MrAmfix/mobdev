package io.github.mobdev.ui.image

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import coil.load
import io.github.mobdev.R
import io.github.mobdev.databinding.FragmentImageBinding
import io.github.mobdev.util.Urls

class ImageFragment : Fragment(R.layout.fragment_image) {

    interface ImageListener {
        fun onImageClosed()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = FragmentImageBinding.bind(view)
        val link = requireArguments().getString(ARG_LINK)
            ?: error("Image link missing")

        b.imageProgress.visibility = View.VISIBLE
        b.fullImage.load(Urls.image(link)) {
            crossfade(true)
            listener(
                onSuccess = { _, _ -> b.imageProgress.visibility = View.GONE },
                onError = { _, _ -> b.imageProgress.visibility = View.GONE }
            )
        }

        b.closeButton.setOnClickListener {
            (activity as? ImageListener)?.onImageClosed()
        }
    }

    companion object {
        private const val ARG_LINK = "link"
        fun newInstance(link: String): ImageFragment = ImageFragment().apply {
            arguments = Bundle().apply { putString(ARG_LINK, link) }
        }
    }
}
