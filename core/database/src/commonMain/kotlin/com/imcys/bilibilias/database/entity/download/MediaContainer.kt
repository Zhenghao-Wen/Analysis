package com.imcys.bilibilias.database.entity.download

enum class ContainerAudience {
    VIDEO,
    AUDIO,
    VIDEO_AUDIO,
}


sealed interface MediaContainer {
    val extension: String
    val mimeType: String
    val audience: ContainerAudience

    fun canEmbedCover(): Boolean = false
    fun canEmbedSubtitle(): Boolean = false

    companion object {
        val entries: List<MediaContainer> by lazy {
            listOf(M4A, MP3, MP4, MKV)
        }

        /**
         * 由持久化的 extension 字符串还原容器类型。
         *
         * 该逻辑原先在 `MediaContainerConverter`（`@TypeConverter`）中；为了移除对
         * `@TypeConverters` 的依赖（Room 3.0.x + KSP2 下该注解会导致 `[MissingType]`），
         * 改为普通函数，由实体上的 `@Ignore` 计算属性调用。
         * 存储取值仍是 extension（"m4a"/"mp3"/"mp4"/"mkv"），与改造前完全一致。
         */
        fun fromExtension(extension: String): MediaContainer =
            entries.first { it.extension == extension }
    }

    object M4A : MediaContainer {
        override val extension = "m4a"
        override val mimeType = "audio/mp4"
        override val audience = ContainerAudience.AUDIO
        override fun canEmbedCover() = true
    }

    object MP3 : MediaContainer {
        override val extension = "mp3"
        override val mimeType = "audio/mpeg"
        override val audience = ContainerAudience.AUDIO
    }

    object MP4 : MediaContainer {
        override val extension = "mp4"
        override val mimeType = "video/mp4"
        override val audience = ContainerAudience.VIDEO
        override fun canEmbedCover() = true
        override fun canEmbedSubtitle() = true
    }

    object MKV : MediaContainer {
        override val extension = "mkv"
        override val mimeType = "video/x-matroska"
        override val audience = ContainerAudience.VIDEO
        override fun canEmbedCover() = true
        override fun canEmbedSubtitle() = true
    }
}


val audioContainer by lazy {
    MediaContainer.entries.filter { it.audience != ContainerAudience.VIDEO }
}
val videoContainer by lazy {
    MediaContainer.entries.filter { it.audience != ContainerAudience.AUDIO }
}