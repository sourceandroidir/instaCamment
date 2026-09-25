package com.example.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface InstagramApiService {

    // Official Instagram / Meta Graph API Endpoints
    @GET("{media_id}/comments")
    suspend fun getMediaComments(
        @Path("media_id") mediaId: String,
        @Query("access_token") accessToken: String,
        @Query("fields") fields: String = "id,text,username,from,timestamp",
        @Query("after") afterCursor: String? = null,
        @Query("limit") limit: Int = 100
    ): Response<InstagramCommentsGraphResponse>

    @POST("v18.0/me/messages")
    suspend fun sendDirectMessage(
        @Query("access_token") accessToken: String,
        @Query("recipient") recipientJson: String,
        @Query("message") messageJson: String
    ): Response<InstagramSendMessageResponse>
}

data class InstagramCommentsGraphResponse(
    val data: List<GraphCommentData>?,
    val paging: GraphPaging?
)

data class GraphCommentData(
    val id: String,
    val text: String?,
    val username: String?,
    val from: GraphCommentUser?,
    val timestamp: String?
)

data class GraphCommentUser(
    val id: String?,
    val username: String?,
    val name: String?
)

data class GraphPaging(
    val cursors: GraphCursors?,
    val next: String?
)

data class GraphCursors(
    val before: String?,
    val after: String?
)

data class InstagramSendMessageResponse(
    val recipient_id: String?,
    val message_id: String?,
    val error: GraphApiError?
)

data class GraphApiError(
    val message: String,
    val type: String,
    val code: Int,
    val error_subcode: Int?
)
