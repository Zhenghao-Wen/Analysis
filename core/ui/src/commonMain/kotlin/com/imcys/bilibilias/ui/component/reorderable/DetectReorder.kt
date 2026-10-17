@file:Suppress("DEPRECATION")

package com.imcys.bilibilias.ui.component.reorderable

/*
 * Copyright 2022 André Claßen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import androidx.compose.foundation.gestures.awaitFirstDown
// forEachGesture 已被官方标记 @Deprecated（建议改用 awaitEachGesture）。
// 本文件两个手势函数依赖 PointerInputScope 接收者（awaitLongPressOrCancellation、
// awaitPointerEventScope 均在此基础上），改成 awaitEachGesture 会引入
// awaitPointerEventScope 嵌套，属运行时行为变更，需要在真机上验证拖动/长按。
// 因此这里保留原实现，仅用文件级 @Suppress("DEPRECATION") 消除编译警告。
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.detectReorder(state: ReorderableState<*>) =
    this.then(
        Modifier.pointerInput(Unit) {
            forEachGesture {
                awaitPointerEventScope {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var drag: PointerInputChange?
                    var overSlop = Offset.Zero
                    do {
                        drag = awaitPointerSlopOrCancellation(down.id, down.type) { change, over ->
                            change.consume()
                            overSlop = over
                        }
                    } while (drag != null && !drag.isConsumed)
                    if (drag != null) {
                        state.interactions.trySend(StartDrag(down.id, overSlop))
                    }
                }
            }
        }
    )


fun Modifier.detectReorderAfterLongPress(state: ReorderableState<*>) =
    this.then(
        Modifier.pointerInput(Unit) {
            forEachGesture {
                val down = awaitPointerEventScope {
                    awaitFirstDown(requireUnconsumed = false)
                }
                awaitLongPressOrCancellation(down)?.also {
                    state.interactions.trySend(StartDrag(down.id))
                }
            }
        }
    )