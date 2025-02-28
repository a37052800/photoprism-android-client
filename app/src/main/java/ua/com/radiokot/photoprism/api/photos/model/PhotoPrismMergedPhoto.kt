package ua.com.radiokot.photoprism.api.photos.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class PhotoPrismMergedPhoto
@JsonCreator
constructor(
    @JsonProperty("UID")
    val uid: String,
    @JsonProperty("Hash")
    val hash: String,
    @JsonProperty("Width")
    val width: Int,
    @JsonProperty("Height")
    val height: Int,
    @JsonProperty("TakenAtLocal")
    val takenAtLocal: String,
    @JsonProperty("Type")
    val type: String,
    @JsonProperty("Title")
    val title: String,
    @JsonProperty("Files")
    val files: List<File>,
    @JsonProperty("CameraMake")
    val cameraMake: String?,
) {
    class File(
        @JsonProperty("Hash")
        val hash: String,
        @JsonProperty("UID")
        val uid: String,
        @JsonProperty("PhotoUID")
        val photoUid: String,
        @JsonProperty("Name")
        val name: String,
        @JsonProperty("Mime")
        val mime: String,
        @JsonProperty("Size")
        val size: Long,
        @JsonProperty("Duration")
        val duration: Long?,
        @JsonProperty("Primary")
        val primary: Boolean,
        @JsonProperty("Root")
        val root: String,
        @JsonProperty("Video")
        val video: Boolean,
    )
}
