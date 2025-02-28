package ua.com.radiokot.photoprism.features.viewer.view

import androidx.lifecycle.Lifecycle
import com.google.android.exoplayer2.ui.PlayerView
import ua.com.radiokot.photoprism.databinding.LayoutVideoPlayerControlsBinding
import ua.com.radiokot.photoprism.features.viewer.logic.VideoPlayerCache
import ua.com.radiokot.photoprism.features.viewer.view.model.MediaViewerPage

interface VideoPlayerViewHolder {
    val playerView: PlayerView
    val playerControlsLayout: LayoutVideoPlayerControlsBinding?
    var playerCache: VideoPlayerCache

    fun bindPlayerToLifecycle(lifecycle: Lifecycle)
    fun setOnFatalPlaybackErrorListener(listener: (item: MediaViewerPage) -> Unit)
    fun enableFatalPlaybackErrorListener(item: MediaViewerPage)
}
