package ua.com.radiokot.photoprism.features.gallery.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismMergedPhoto
import ua.com.radiokot.photoprism.features.gallery.logic.MediaFileDownloadUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.MediaWebUrlFactory
import ua.com.radiokot.photoprism.util.LocalDate
import java.util.concurrent.TimeUnit

/**
 * A merged gallery media entry.
 */
class GalleryMedia(
    /**
     * Type-specific media data.
     */
    val media: TypeData,
    /**
     * Unique ID of the merged entry.
     * But because of PhotoPrism pagination strategy, it may be duplicated on adjacent pages.
     */
    val uid: String,
    /**
     * Original graphics width.
     */
    val width: Int,
    /**
     * Original graphics height.
     */
    val height: Int,
    /**
     * **Local** time of taking (or at least indexing) the shot.
     * While the value is not actually in UTC (unless really taken there),
     * all the temporal operations (Calendar, DateFormat) must be done in UTC
     * to get proper result.
     *
     * *For example, if a photo is taken on July 4th at 23:18 in Montenegro,
     * its local time 2023-07-04T23:18:32Z must be displayed and treated as July 4th, 23:18
     * regardless of whether the gallery is viewed from Montenegro or USA.*
     */
    val takenAtLocal: LocalDate,
    /**
     * Human-friendly title (PhotoPrism "Title").
     */
    val title: String,
    /**
     * Direct URL to the small square static thumbnail.
     */
    val smallThumbnailUrl: String,
    /**
     * Direct URL to open this media in the web viewer.
     */
    val webViewUrl: String,
    files: List<File>,
) {
    /**
     * Files associated with this media.
     */
    var files: List<File> = files
        private set

    constructor(
        source: PhotoPrismMergedPhoto,
        previewUrlFactory: MediaPreviewUrlFactory,
        downloadUrlFactory: MediaFileDownloadUrlFactory,
        webUrlFactory: MediaWebUrlFactory,
    ) : this(
        media = TypeData.fromPhotoPrism(
            source = source,
            previewUrlFactory = previewUrlFactory
        ),
        uid = source.uid,
        width = source.width,
        height = source.height,
        takenAtLocal = LocalDate(localDate = parsePhotoPrismDate(source.takenAtLocal)!!),
        title = source.title,
        smallThumbnailUrl = previewUrlFactory.getSmallThumbnailUrl(source.hash),
        webViewUrl = webUrlFactory.getWebViewUrl(source.uid),
        files = source.files.map { photoPrismFile ->
            File(
                source = photoPrismFile,
                thumbnailUrlFactory = previewUrlFactory,
                downloadUrlFactory = downloadUrlFactory,
            )
        }.toMutableList()
    )

    /**
     * Merges current [files] with [moreFiles] overwriting the value
     */
    fun mergeFiles(moreFiles: Collection<File>) {
        files = (files + moreFiles).distinct()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GalleryMedia

        if (uid != other.uid) return false

        return true
    }

    override fun hashCode(): Int {
        return uid.hashCode()
    }

    override fun toString(): String {
        return "GalleryMedia(uid='$uid', media=$media)"
    }


    /**
    photoprism/pkg/media/types.go

    const (
    Unknown  Type = ""
    Image    Type = "image"
    Raw      Type = "raw"
    Animated Type = "animated"
    Live     Type = "live"
    Video    Type = "video"
    Vector   Type = "vector"
    Sidecar  Type = "sidecar"
    Text     Type = "text"
    Other    Type = "other"
    )
     */
    enum class TypeName(val value: String) {
        UNKNOWN(""),
        IMAGE("image"),
        RAW("raw"),
        ANIMATED("animated"),
        LIVE("live"),
        VIDEO("video"),
        VECTOR("vector"),
        SIDECAR("sidecar"),
        TEXT("text"),
        OTHER("other"),
        ;
    }

    sealed class TypeData(val typeName: TypeName) {
        interface ViewableAsImage {
            fun getPreviewUrl(maxViewSize: Int): String
        }

        private class ViewableAsImageWithUrlFactory(
            private val hash: String,
            private val mediaPreviewUrlFactory: MediaPreviewUrlFactory,
        ) : ViewableAsImage {
            override fun getPreviewUrl(maxViewSize: Int): String =
                when {
                    maxViewSize < 1000 ->
                        mediaPreviewUrlFactory.getPreview720Url(hash)

                    maxViewSize < 1500 ->
                        mediaPreviewUrlFactory.getPreview1280Url(hash)

                    maxViewSize < 2000 ->
                        mediaPreviewUrlFactory.getPreview1920Url(hash)

                    maxViewSize < 2500 ->
                        mediaPreviewUrlFactory.getPreview2048Url(hash)

                    maxViewSize < 4000 ->
                        mediaPreviewUrlFactory.getPreview3840Url(hash)

                    maxViewSize < 4500 ->
                        mediaPreviewUrlFactory.getPreview4096Url(hash)

                    else ->
                        mediaPreviewUrlFactory.getPreview7680Url(hash)
                }
        }

        interface ViewableAsVideo {
            val avcPreviewUrl: String
        }

        object Unknown : TypeData(TypeName.UNKNOWN)

        class Image(
            hash: String,
            mediaPreviewUrlFactory: MediaPreviewUrlFactory,
        ) : TypeData(TypeName.IMAGE),
            ViewableAsImage by ViewableAsImageWithUrlFactory(hash, mediaPreviewUrlFactory)

        class Raw(
            hash: String,
            mediaPreviewUrlFactory: MediaPreviewUrlFactory,
        ) : TypeData(TypeName.RAW),
            ViewableAsImage by ViewableAsImageWithUrlFactory(hash, mediaPreviewUrlFactory)

        class Animated(
            override val avcPreviewUrl: String,
        ) : TypeData(TypeName.ANIMATED), ViewableAsVideo

        class Live(
            override val avcPreviewUrl: String,
            /**
             * Non-zero duration of the full video in milliseconds,
             * or null if it couldn't be determined.
             */
            val fullDurationMs: Long?,
            val kind: Kind,
            hash: String,
            mediaPreviewUrlFactory: MediaPreviewUrlFactory,
        ) : TypeData(TypeName.LIVE), ViewableAsVideo,
            ViewableAsImage by ViewableAsImageWithUrlFactory(hash, mediaPreviewUrlFactory) {
            init {
                require((fullDurationMs ?: 0L) != 0L) {
                    "The full duration must be either null or positive"
                }
            }

            sealed class Kind {
                /**
                 * Just a short video treated by PhotoPrism as a live photo
                 * having the still image generated from the first frame.
                 */
                object ShortVideo : Kind()

                /**
                 * Real live photo with with a high quality still image
                 * taken at the end of the video.
                 * This kind has the true live photo magic ✨
                 *
                 * [Samsung: Motion Photos](https://www.samsung.com/au/support/mobile-devices/motion-photos/)
                 */
                object Samsung : Kind()

                /**
                 * Real live photo with a high quality still image
                 * taken at the middle of the 3 second the video.
                 * This kind has the true live photo magic ✨
                 *
                 * [Apple: Live Photos](https://support.apple.com/en-gb/HT207310)
                 */
                object Apple : Kind()

                /**
                 * Google motion still photo with a high quality still image
                 * taken, however, not during the video but before it.
                 * To get the magical effect, it must be played with a motion still
                 * stabilization, which the gallery is not capable of doing.
                 *
                 * [Google: Motion Stills](https://blog.research.google/2018/03/behind-motion-photos-technology-in.html)
                 */
                object Google : Kind()

                /**
                 * Unknown kind of live photo.
                 */
                object Other : Kind()
            }
        }

        class Video(
            override val avcPreviewUrl: String,
        ) : TypeData(TypeName.VIDEO), ViewableAsVideo

        class Vector(
            hash: String,
            mediaPreviewUrlFactory: MediaPreviewUrlFactory,
        ) : TypeData(TypeName.VECTOR),
            ViewableAsImage by ViewableAsImageWithUrlFactory(hash, mediaPreviewUrlFactory)

        object Sidecar : TypeData(TypeName.SIDECAR)
        object Text : TypeData(TypeName.TEXT)
        object Other : TypeData(TypeName.OTHER)

        companion object {
            fun fromPhotoPrism(
                source: PhotoPrismMergedPhoto,
                previewUrlFactory: MediaPreviewUrlFactory,
            ): TypeData =
                when (val type = source.type) {
                    TypeName.UNKNOWN.value -> Unknown
                    TypeName.IMAGE.value -> Image(
                        hash = source.hash,
                        mediaPreviewUrlFactory = previewUrlFactory,
                    )

                    TypeName.RAW.value -> Raw(
                        hash = source.hash,
                        mediaPreviewUrlFactory = previewUrlFactory,
                    )

                    TypeName.ANIMATED.value -> Animated(
                        avcPreviewUrl = previewUrlFactory.getMp4PreviewUrl(source.hash),
                    )

                    TypeName.LIVE.value -> Live(
                        avcPreviewUrl = previewUrlFactory.getMp4PreviewUrl(source.hash),
                        // Find the duration among video files.
                        fullDurationMs = source.files
                            .find { it.duration != null && it.duration > 0 }
                            ?.duration
                            ?.let(TimeUnit.NANOSECONDS::toMillis),
                        hash = source.hash,
                        kind = when {
                            source.files.let { files ->
                                val videoFile = files.find { it.video }
                                val primaryFile = files.find { it.primary && it != videoFile }

                                // Short videos have primary image file
                                // generated from the video file,
                                // while real live photos have the preview generated
                                // from the image file or don't have it at all.
                                primaryFile != null && videoFile != null
                                        && primaryFile.name.startsWith(videoFile.name)
                            } ->
                                Live.Kind.ShortVideo

                            source.cameraMake == "Samsung" ->
                                Live.Kind.Samsung

                            source.cameraMake == "Apple" ->
                                Live.Kind.Apple

                            source.cameraMake == "Google" ->
                                Live.Kind.Google

                            else ->
                                Live.Kind.Other
                        },
                        mediaPreviewUrlFactory = previewUrlFactory,
                    )

                    TypeName.VIDEO.value -> Video(
                        avcPreviewUrl = previewUrlFactory.getMp4PreviewUrl(source.hash),
                    )

                    TypeName.VECTOR.value -> Vector(
                        hash = source.hash,
                        mediaPreviewUrlFactory = previewUrlFactory,
                    )

                    TypeName.SIDECAR.value -> Sidecar
                    TypeName.TEXT.value -> Text
                    TypeName.OTHER.value -> Other
                    else -> error("Unsupported PhotoPrism media type '$type'")
                }
        }
    }

    @Parcelize
    class File(
        /**
         * Filename with the full path.
         */
        val name: String,
        /**
         * Unique ID of the file.
         */
        val uid: String,
        /**
         * Unique ID of the file's parent [GalleryMedia] (PhotoPrism "PhotoUID").
         */
        val mediaUid: String,
        val mimeType: String,
        val sizeBytes: Long,
        /**
         * Direct URL to the small square static thumbnail.
         */
        val smallThumbnailUrl: String,
        /**
         * Direct URL download this file.
         */
        val downloadUrl: String,
    ) : Parcelable {
        constructor(
            source: PhotoPrismMergedPhoto.File,
            thumbnailUrlFactory: MediaPreviewUrlFactory,
            downloadUrlFactory: MediaFileDownloadUrlFactory,
        ) : this(
            name = source.name,
            uid = source.uid,
            mediaUid = source.photoUid,
            mimeType = source.mime,
            sizeBytes = source.size,
            smallThumbnailUrl = thumbnailUrlFactory.getSmallThumbnailUrl(source.hash),
            downloadUrl = downloadUrlFactory.getDownloadUrl(source.hash),
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as File

            if (uid != other.uid) return false

            return true
        }

        override fun hashCode(): Int {
            return uid.hashCode()
        }

        override fun toString(): String {
            return "File(uid='$uid', name='$name')"
        }
    }
}
