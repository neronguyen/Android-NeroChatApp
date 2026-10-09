package io.github.neronguyen.chat.core.common.network

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Dispatcher(val chatDispatcher: ChatDispatchers)

enum class ChatDispatchers {
    Default,
    IO,
}
