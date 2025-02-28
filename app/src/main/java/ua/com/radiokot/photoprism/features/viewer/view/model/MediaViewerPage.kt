package ua.com.radiokot.photoprism.features.viewer.view.model

import android.util.Size
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.mikepenz.fastadapter.items.AbstractItem
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaTypeResources
import kotlin.math.max

sealed class MediaViewerPage(
    val thumbnailUrl: String,
    val source: GalleryMedia?,
) : AbstractItem<ViewHolder>() {

    override var identifier: Long
        get() = (thumbnailUrl + type).hashCode().toLong()
        set(_) = error("Don't override my value")

    companion object {
        private val FADE_END_LIVE_PHOTO_KINDS = setOf(
            GalleryMedia.TypeData.Live.Kind.Samsung,
            GalleryMedia.TypeData.Live.Kind.Apple,
            GalleryMedia.TypeData.Live.Kind.Google,
        )
        private const val FADE_END_PLAYBACK_DURATION_MS_SHORT =
            400L + FadeEndLivePhotoViewerPage.FADE_DURATION_MS
        private const val FADE_END_PLAYBACK_DURATION_MS_LONG =
            1000L + FadeEndLivePhotoViewerPage.FADE_DURATION_MS

        fun fromGalleryMedia(
            source: GalleryMedia,
            imageViewSize: Size,
        ): MediaViewerPage {
            return when {
                source.media is GalleryMedia.TypeData.Live
                        && source.media.kind in FADE_END_LIVE_PHOTO_KINDS
                        && source.media.fullDurationMs != null -> {

                    val videoPreviewStartMs: Long? =
                        when (source.media.kind) {
                            GalleryMedia.TypeData.Live.Kind.Samsung ->
                                (source.media.fullDurationMs - FADE_END_PLAYBACK_DURATION_MS_SHORT)
                                    .coerceAtLeast(0)

                            GalleryMedia.TypeData.Live.Kind.Apple ->
                                (source.media.fullDurationMs / 2 - FADE_END_PLAYBACK_DURATION_MS_SHORT)
                                    .coerceAtLeast(0)

                            GalleryMedia.TypeData.Live.Kind.Google ->
                                (source.media.fullDurationMs - FADE_END_PLAYBACK_DURATION_MS_LONG)
                                    .coerceAtLeast(0)

                            else ->
                                null
                        }

                    val videoPreviewEndMs: Long? =
                        when (source.media.kind) {
                            GalleryMedia.TypeData.Live.Kind.Apple ->
                                (source.media.fullDurationMs / 2)
                                    .coerceAtLeast(0)

                            else ->
                                null
                        }

                    FadeEndLivePhotoViewerPage(
                        photoPreviewUrl = source.media.getPreviewUrl(
                            max(
                                imageViewSize.width,
                                imageViewSize.height
                            )
                        ),
                        videoPreviewUrl = source.media.avcPreviewUrl,
                        videoPreviewStartMs = videoPreviewStartMs,
                        videoPreviewEndMs = videoPreviewEndMs,
                        imageViewSize = imageViewSize,
                        thumbnailUrl = source.smallThumbnailUrl,
                        source = source,
                    )
                }

                source.media is GalleryMedia.TypeData.ViewableAsVideo ->
                    VideoViewerPage(
                        previewUrl = source.media.avcPreviewUrl,
                        isLooped = source.media is GalleryMedia.TypeData.Live
                                || source.media is GalleryMedia.TypeData.Animated,
                        needsVideoControls = source.media is GalleryMedia.TypeData.Video,
                        thumbnailUrl = source.smallThumbnailUrl,
                        source = source,
                    )

                source.media is GalleryMedia.TypeData.ViewableAsImage ->
                    ImageViewerPage(
                        previewUrl = source.media.getPreviewUrl(
                            max(
                                imageViewSize.width,
                                imageViewSize.height
                            )
                        ),
                        imageViewSize = imageViewSize,
                        thumbnailUrl = source.smallThumbnailUrl,
                        source = source,
                    )

                else ->
                    unsupported(source)
            }
        }

        fun unsupported(source: GalleryMedia) = UnsupportedNoticePage(
            mediaTypeIcon = GalleryMediaTypeResources.getIcon(source.media.typeName),
            mediaTypeName = GalleryMediaTypeResources.getName(source.media.typeName),
            thumbnailUrl = source.smallThumbnailUrl,
            source = source,
        )
    }
}
