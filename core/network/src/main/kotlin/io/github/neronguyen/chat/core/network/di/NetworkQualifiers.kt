package io.github.neronguyen.chat.core.network.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthOkHttp

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedOkHttp
