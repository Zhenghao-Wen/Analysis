package com.imcys.bilibilias.database.entity.download

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Ignore
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.imcys.bilibilias.database.currentTimeMillis

// 注意：本文件内所有实体都**不再使用 `@TypeConverters`**。
// Room 3.0 内置 enum <-> String 转换（存 enum 的 name），与原先各 *Converter 的行为完全一致，
// 因此删除这些 converter 不影响已存数据，也无需迁移。
// 仅有 MediaContainer（sealed interface）与 NamingConventionInfo（sealed class）需要转换，
// 这两个已改为「列存 String + @Ignore 计算属性」，详见 DownloadSegment。

/**
 * 下载任务（顶层）
 */
@Entity(tableName = "download_task")
data class DownloadTask(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "task_id")
    val taskId: Long = 0,

    @ColumnInfo(name = "platform_id")
    val platformId: String,          // bvid 或其余平台 id

    @ColumnInfo(name = "download_platform")
    val downloadPlatform: DownloadPlatform,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "cover")
    val cover: String,

    @ColumnInfo(name = "type")
    val type: DownloadTaskType,


    @ColumnInfo(name = "created_time")
    val createTime: Long = currentTimeMillis(),

    @ColumnInfo(name = "update_time")
    val updateTime: Long = currentTimeMillis()
)

/**
 * 下载任务节点
 */
@Entity(
    tableName = "download_task_node",
    foreignKeys = [ForeignKey(
        entity = DownloadTask::class,
        parentColumns = ["task_id"],
        childColumns = ["task_id"],
        onDelete = ForeignKey.CASCADE
    ), ForeignKey(
        entity = DownloadTaskNode::class,
        parentColumns = ["node_id"],
        childColumns = ["parent_node_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("task_id"), Index("parent_node_id")]
)
data class DownloadTaskNode(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "node_id")
    val nodeId: Long = 0,

    @ColumnInfo(name = "task_id")
    val taskId: Long,                // 归属任务

    @ColumnInfo(name = "parent_node_id")
    val parentNodeId: Long? = null,         // 父节点

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "pic")
    val pic: String? = null,


    @ColumnInfo(name = "platform_id")
    val platformId: String,          // 平台节点唯一 id

    @ColumnInfo(name = "node_type")
    val nodeType: DownloadTaskNodeType,

    @ColumnInfo(name = "created_time")
    val createTime: Long = currentTimeMillis(),

    @ColumnInfo(name = "update_time")
    val updateTime: Long = currentTimeMillis()
)

/**
 * 最终下载片段
 */
@Entity(
    tableName = "download_segment",
    foreignKeys = [ForeignKey(
        entity = DownloadTaskNode::class,
        parentColumns = ["node_id"],
        childColumns = ["node_id"],
        onDelete = ForeignKey.CASCADE
    ), ForeignKey(
        entity = DownloadTask::class,
        parentColumns = ["task_id"],
        childColumns = ["task_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("node_id"), Index("task_id")]
)
data class DownloadSegment(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "segment_id")
    val segmentId: Long = 0,

    @ColumnInfo(name = "node_id")
    val nodeId: Long,

    @ColumnInfo(name = "task_id")
    val taskId: Long? = null, // 关联任务

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "cover")
    val cover: String?,

    @ColumnInfo(name = "segment_order")
    val segmentOrder: Long,

    @ColumnInfo(name = "platform_id")
    val platformId: String,

    // 平台唯一ID，如果是B站视频/番剧那就是CID，不同的平台唯一ID不同
    @ColumnInfo(name = "platform_unique_id", defaultValue = "")
    val platformUniqueId: String,

    @ColumnInfo(name = "platform_info")
    val platformInfo: String, // 平台JSON

    @ColumnInfo(name = "download_mode")
    val downloadMode: DownloadMode,

    // 直接用 extension 字符串（"m4a"/"mp3"/"mp4"/"mkv"）持久化，不再依赖 @TypeConverters。
    // 列名与取值都与改造前完全一致（原先的 MediaContainerConverter 就是存 extension），
    // 因此无需迁移；读取方仍可用下文的 @Ignore 属性 mediaContainer 拿到模型对象。
    @ColumnInfo(name = "media_container")
    val mediaContainerExtension: String,

    // 质量描述
    @ColumnInfo(name = "quality_description")
    val qualityDescription: String? = null,

    @ColumnInfo(name = "save_path")
    val savePath: String,

    @ColumnInfo(name = "file_size")
    val fileSize: Long,

    @ColumnInfo(name = "duration")
    val duration: Long?,

    @ColumnInfo(name = "download_state")
    val downloadState: DownloadState = DownloadState.WAITING,

    // 用原 converter 相同的 JSON 字符串持久化，不再依赖 @TypeConverters。
    // 列名与 JSON 结构都与改造前一致（见 NamingConventionCodec），因此无需迁移；
    // 读取方仍可用下文的 @Ignore 属性 namingConventionInfo 拿到模型对象。
    @ColumnInfo(name = "naming_convention_info")
    val namingConventionInfoJson: String? = null,

    @ColumnInfo(name = "created_time")
    val createTime: Long = currentTimeMillis(),

    @ColumnInfo(name = "update_time")
    val updateTime: Long = currentTimeMillis(),
) {
    // 仅仅用于临时存储视频时长等信息，不存数据库
    @Ignore
    var tempDuration: Long = 0

    /**
     * 容器类型（不参与持久化）。
     *
     * 原先是 `@TypeConverters(MediaContainerConverter::class)` 由 Room 自动转换；
     * 因 Room 3.0.x + KSP2 下 `@TypeConverters` 会导致 `[MissingType]`，
     * 改为「列存 extension + 计算属性」，对外 API 保持不变。
     *
     * 注意：这是**没有 backing field 的计算属性**，`@Ignore` 不能直接标在属性上
     * （会报 "annotation is not applicable to target 'member property without
     * backing field or delegate'"），必须用 `@get:` 指定到 getter。
     */
    @get:Ignore
    val mediaContainer: MediaContainer
        get() = MediaContainer.fromExtension(mediaContainerExtension)

    /**
     * 命名规则信息（不参与持久化），由 JSON 列解码得到。
     *
     * 同上：取代原先的 `NamingConventionConverter`，对外 API 保持不变；
     * 同样必须用 `@get:Ignore` 而非 `@Ignore`。
     */
    @get:Ignore
    val namingConventionInfo: NamingConventionInfo?
        get() = NamingConventionCodec.decode(namingConventionInfoJson)
}
