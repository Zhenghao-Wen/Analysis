package com.imcys.bilibilias.database.dao


import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.imcys.bilibilias.database.entity.BILIUserCookiesEntity

@Dao
interface BILIUserCookiesDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBILIUserCookie(biliUserCookiesEntity: BILIUserCookiesEntity): Long

    @Query("select * from bili_user_cookies where userId = :userId")
    suspend fun getBILIUserCookiesByUid(userId: Long): List<BILIUserCookiesEntity>

    @Query("delete from bili_user_cookies WHERE userId = :userId")
    suspend fun deleteBILICookiesByUid(userId: Long): Int

    /**
     * 某个用户名下**原始记录**的条数（不解密）。
     *
     * 用于区分「库里根本没有 Cookie」（如 TV/accessKey 登录）与
     * 「库里有记录但全部无法解密」（旧版明文数据，需要清理会话）。
     */
    @Query("select count(*) from bili_user_cookies where userId = :userId")
    suspend fun countBILIUserCookiesByUid(userId: Long): Int
}