package com.pushkar.RGM

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface UserApiService {
    @POST("api/register")
    suspend fun register(@Body user: User): Response<ResponseBody>

    @POST("api/login")
    suspend fun login(@Body user: User): Response<User>

    @PUT("api/user/{username}")
    suspend fun updateProfile(@Path("username") username: String, @Body user: User): Response<User>
    
    @GET("api/users")
    suspend fun getAllUsers(): Response<List<User>>

    @GET("api/chats/{username}")
    suspend fun getChattedUsers(@Path("username") username: String): Response<List<String>>

    @GET("api/posts")
    suspend fun getPosts(): Response<List<Post>>

    @POST("api/posts")
    suspend fun createPost(@Body post: Post): Response<Post>

    @GET("api/reels")
    suspend fun getReels(): Response<List<Post>>
}
