package com.imcys.bilibilias.data.repository

import com.imcys.bilibilias.common.data.CommonBuildConfig
import com.imcys.bilibilias.data.model.github.GithubCodeVersionUpdateInfo
import com.imcys.bilibilias.network.mapData
import com.imcys.bilibilias.network.service.GithubAPIService
import kotlinx.coroutines.flow.map

class GithubInfoRepository(
    private val githubAPIService: GithubAPIService
) {
    suspend fun getLastCommitInfo(
        org: String,
        repository: String,
    ) =
        githubAPIService.getCommits(org, repository).map { result ->
            result.mapData { commits, _ ->
                if (commits.isNullOrEmpty()) {
                    return@mapData GithubCodeVersionUpdateInfo(
                        hasUpdate = false,
                        tipMsg = "无法获取版本信息",
                        newHashList = emptyList(),
                    )
                }

                // 构建哈希固定为 7 位（见 app / shared 的 build.gradle.kts：
                // git rev-parse --short=7 HEAD）。
                // 注意：这里必须用「前缀匹配」而不是「等长截取后比较」——
                // 之前用 take(8) 截取 7 位的构建哈希去比 8 位的仓库 SHA，
                // 永远不相等，导致一直显示「无法追踪该版本信息」。
                val currentShortHash = CommonBuildConfig.gitCommitHash.trim().lowercase()

                val currentCommitIndex = if (currentShortHash.isEmpty()) {
                    -1
                } else {
                    commits.indexOfFirst { it.sha.lowercase().startsWith(currentShortHash) }
                }

                // 当前版本不在提交历史中
                if (currentCommitIndex == -1) {
                    return@mapData GithubCodeVersionUpdateInfo(
                        hasUpdate = false,
                        tipMsg = if (currentShortHash.isEmpty()) {
                            "无法获取当前构建的提交信息"
                        } else {
                            "$currentShortHash 无法追踪该版本信息（可能使用了本地修改或未推送的提交）"
                        },
                        newHashList = emptyList(),
                    )
                }

                return@mapData if (currentCommitIndex == 0) {
                    // 当前版本就是最新提交
                    GithubCodeVersionUpdateInfo(
                        hasUpdate = false,
                        tipMsg = "当前已是最新版本",
                        newHashList = emptyList(),
                    )
                } else {
                    val newCommits = commits.take(currentCommitIndex)
                    GithubCodeVersionUpdateInfo(
                        hasUpdate = true,
                        tipMsg = "发现 ${newCommits.size} 个可用提交：${
                            newCommits.map {
                                it.sha.take(
                                    7
                                )
                            }
                        }",
                        newHashList = newCommits.map { it.sha.take(7) },
                    )
                }
            }
        }
}

