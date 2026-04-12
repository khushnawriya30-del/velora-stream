package com.cinevault.tv.di

import android.content.Context
import android.util.Log
import com.cinevault.tv.BuildConfig
import com.cinevault.tv.data.local.SessionManager
import com.cinevault.tv.data.remote.CineVaultApi
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthInterceptor(sessionManager: SessionManager): Interceptor {
        return Interceptor { chain ->
            val token = runBlocking { sessionManager.accessToken.firstOrNull() }
            val request = chain.request().newBuilder().apply {
                token?.let { addHeader("Authorization", "Bearer $it") }
                addHeader("Content-Type", "application/json")
            }.build()
            chain.proceed(request)
        }
    }

    @Provides
    @Singleton
    fun provideTokenAuthenticator(sessionManager: SessionManager): Authenticator {
        return object : Authenticator {
            private val gson = Gson()

            override fun authenticate(route: Route?, response: Response): Request? {
                if (response.request.header("X-Token-Refreshed") != null) return null
                if (response.request.url.encodedPath.contains("auth/refresh")) return null

                val storedRefreshToken = runBlocking { sessionManager.refreshToken.firstOrNull() }
                if (storedRefreshToken.isNullOrEmpty()) return null

                return try {
                    val refreshClient = OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .build()

                    val jsonBody = gson.toJson(mapOf("refreshToken" to storedRefreshToken))
                    val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
                    val refreshRequest = Request.Builder()
                        .url(BuildConfig.BASE_URL + "auth/refresh")
                        .post(requestBody)
                        .addHeader("Content-Type", "application/json")
                        .build()

                    val refreshResponse = refreshClient.newCall(refreshRequest).execute()
                    if (refreshResponse.isSuccessful) {
                        val bodyString = refreshResponse.body?.string()
                        val data = gson.fromJson(bodyString, RefreshData::class.java)
                        if (data?.accessToken != null) {
                            runBlocking {
                                sessionManager.updateToken(data.accessToken)
                                data.refreshToken?.let { sessionManager.saveRefreshToken(it) }
                            }
                            response.request.newBuilder()
                                .header("Authorization", "Bearer ${data.accessToken}")
                                .header("X-Token-Refreshed", "true")
                                .build()
                        } else null
                    } else {
                        if (refreshResponse.code == 401) {
                            runBlocking { sessionManager.clearSession() }
                        }
                        null
                    }
                } catch (e: Exception) {
                    Log.e("TvAuth", "Token refresh error", e)
                    null
                }
            }
        }
    }

    private data class RefreshData(val accessToken: String?, val refreshToken: String?)

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor, authenticator: Authenticator): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(authenticator)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                }
            }
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideCineVaultApi(retrofit: Retrofit): CineVaultApi {
        return retrofit.create(CineVaultApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager {
        return SessionManager(context)
    }
}
