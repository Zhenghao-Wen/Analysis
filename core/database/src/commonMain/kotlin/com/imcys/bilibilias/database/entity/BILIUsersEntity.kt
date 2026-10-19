package com.imcys.bilibilias.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.imcys.bilibilias.database.currentTimeMillis

@Entity(tableName = "bili_users")
data class BILIUsersEntity(
    @PrimaryKey(true)
    var id: Long = 0,
    @ColumnInfo(name = "login_platform")
    val loginPlatform: LoginPlatform,
    @ColumnInfo(name = "mid")
    val mid: Long,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "face")
    val face: String,
    @ColumnInfo("level")
    val level: Int,
    @ColumnInfo("vip_state")
    val vipState: Int,
    @ColumnInfo("refresh_token")
    val refreshToken: String?,
    @ColumnInfo("access_token")
    val accessToken: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = currentTimeMillis(),
) {
    fun isVip(): Boolean {
        return vipState > 0
    }
}
