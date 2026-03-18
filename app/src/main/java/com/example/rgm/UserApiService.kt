package com.pushkar.RGM

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface UserApiService {
    @GET("api/status")
    suspend fun getServerStatus(): Response<Map<String, String>>

    @POST("api/register")
    suspend fun register(@Body user: User): Response<ResponseBody>

    @POST("api/login")
    suspend fun login(@Body user: User): Response<User>

    @GET("api/check-username/{username}")
    suspend fun checkUsername(@Path("username") username: String): Response<Map<String, Boolean>>

    @PUT("api/user/{username}")
    suspend fun updateProfile(@Path("username") username: String, @Body user: User): Response<User>
    
    @GET("api/users")
    suspend fun getAllUsers(): Response<List<User>>

    @GET("api/chats/{username}")
    suspend fun getChattedUsers(@Path("username") username: String): Response<List<String>>

    @POST("api/chats/{username}/{otherUser}")
    suspend fun addChattedUser(@Path("username") username: String, @Path("otherUser") otherUser: String): Response<ResponseBody>

    @GET("api/posts")
    suspend fun getPosts(): Response<List<Post>>

    @POST("api/posts")
    suspend fun createPost(@Body post: Post): Response<Post>

    @GET("api/reels")
    suspend fun getReels(): Response<List<Post>>
}
