package com.google.firebase

/**
 * Firebase 的无操作替代实现。
 *
 * 原实现来自闭源 Firebase SDK。这里只提供业务代码实际引用到的 `Firebase.app`
 * 与 `isDataCollectionDefaultEnabled`，使 MainActivity 中的隐私开关逻辑
 * 可以原样保留而编译、运行均不再需要 Firebase。
 */
class FirebaseApp {
    var isDataCollectionDefaultEnabled: Boolean = false
}

object Firebase {
    val app: FirebaseApp = FirebaseApp()
}

val app: FirebaseApp
    get() = Firebase.app
