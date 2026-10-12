package io.github.neronguyen.chat.core.network.retrofit

import arrow.core.Either
import arrow.core.raise.either
import io.github.neronguyen.chat.core.model.error.DataError
import io.github.neronguyen.chat.core.network.BuildConfig
import io.github.neronguyen.chat.core.network.ChatNetworkDataSource
import io.github.neronguyen.chat.core.network.di.AuthenticatedOkHttp
import io.github.neronguyen.chat.core.network.model.AddChatParticipantRequest
import io.github.neronguyen.chat.core.network.model.ChatMessageDto
import io.github.neronguyen.chat.core.network.model.ChatParticipantDto
import io.github.neronguyen.chat.core.network.model.ChatRoomDto
import io.github.neronguyen.chat.core.network.model.CreateDirectChatRoomRequest
import io.github.neronguyen.chat.core.network.model.CreateGroupChatRoomRequest
import io.github.neronguyen.chat.core.network.util.safeCall
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

private interface RetrofitChatNetworkApi {

    @GET("api/chat/rooms")
    suspend fun getChatRoomsForUser(): Response<List<ChatRoomDto>>

    @GET("api/chat/rooms/{chatRoomId}")
    suspend fun getChatRoom(
        @Path("chatRoomId") chatRoomId: String
    ): Response<ChatRoomDto>

    @GET("api/chat/rooms/{chatRoomId}/messages")
    suspend fun getMessagesForChatRoom(
        @Path("chatRoomId") chatRoomId: String,
        @Query("before") before: String? = null
    ): Response<List<ChatMessageDto>>

    @GET("api/chat/participants")
    suspend fun getParticipantByEmail(
        @Query("email") email: String? = null
    ): Response<ChatParticipantDto>

    @POST("api/chat/rooms/direct")
    suspend fun createDirectChatRoom(
        @Body request: CreateDirectChatRoomRequest
    ): Response<ChatRoomDto>

    @POST("api/chat/rooms/group")
    suspend fun createGroupChatRoom(
        @Body request: CreateGroupChatRoomRequest
    ): Response<ChatRoomDto>

    @POST("api/chat/rooms/{chatRoomId}/add")
    suspend fun addParticipantsToGroup(
        @Path("chatRoomId") chatRoomId: String,
        @Body request: AddChatParticipantRequest
    ): Response<ChatRoomDto>

    @DELETE("api/chat/rooms/{chatRoomId}/leave")
    suspend fun leaveChatRoom(
        @Path("chatRoomId") chatRoomId: String
    ): Response<Unit>

    @DELETE("api/chat/messages/{messageId}")
    suspend fun deleteMessage(
        @Path("messageId") messageId: String
    ): Response<Unit>
}

@Singleton
internal class RetrofitChatNetwork @Inject constructor(
    networkJson: Json,
    @AuthenticatedOkHttp okHttpClient: dagger.Lazy<OkHttpClient>
) : ChatNetworkDataSource {

    private val networkApi: RetrofitChatNetworkApi by lazy {
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .callFactory { okHttpClient.get().newCall(it) }
            .addConverterFactory(networkJson.asConverterFactory(contentType))
            .build()
            .create(RetrofitChatNetworkApi::class.java)
    }

    override suspend fun getChatRoomsForUser(): Either<DataError.Network, List<ChatRoomDto>> =
        either { safeCall { networkApi.getChatRoomsForUser() } }

    override suspend fun getChatRoom(chatRoomId: String): Either<DataError.Network, ChatRoomDto> =
        either { safeCall { networkApi.getChatRoom(chatRoomId) } }

    override suspend fun getMessagesForChatRoom(
        chatRoomId: String,
        before: String?
    ): Either<DataError.Network, List<ChatMessageDto>> =
        either { safeCall { networkApi.getMessagesForChatRoom(chatRoomId, before) } }

    override suspend fun getParticipantByEmail(email: String): Either<DataError.Network, ChatParticipantDto> =
        either { safeCall { networkApi.getParticipantByEmail(email) } }

    override suspend fun createDirectChatRoom(
        targetUserId: String,
        message: String
    ): Either<DataError.Network, ChatRoomDto> =
        either {
            safeCall {
                networkApi.createDirectChatRoom(
                    CreateDirectChatRoomRequest(targetUserId = targetUserId, message = message)
                )
            }
        }

    override suspend fun createGroupChatRoom(
        participantIds: List<String>,
        name: String?
    ): Either<DataError.Network, ChatRoomDto> =
        either {
            safeCall {
                networkApi.createGroupChatRoom(
                    CreateGroupChatRoomRequest(name = name ?: "", participantIds = participantIds)
                )
            }
        }

    override suspend fun addParticipantsToGroup(
        chatRoomId: String,
        userIds: List<String>
    ): Either<DataError.Network, ChatRoomDto> =
        either {
            safeCall {
                networkApi.addParticipantsToGroup(
                    chatRoomId = chatRoomId,
                    request = AddChatParticipantRequest(userIds = userIds)
                )
            }
        }

    override suspend fun leaveChatRoom(chatRoomId: String): Either<DataError.Network, Unit> =
        either { safeCall { networkApi.leaveChatRoom(chatRoomId) } }

    override suspend fun deleteMessage(messageId: String): Either<DataError.Network, Unit> =
        either { safeCall { networkApi.deleteMessage(messageId) } }
}
