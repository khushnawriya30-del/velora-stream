package com.cinevault.app.di

import android.content.Context
import android.util.Log
import com.cinevault.app.BuildConfig
import com.cinevault.app.data.local.SessionManager
import com.cinevault.app.data.remote.CineVaultApi
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
                // Don't retry if we already tried refreshing on this chain
                if (response.request.header("X-Token-Refreshed") != null) {
                    Log.w("CineVaultAuth", "Token refresh already attempted, giving up")
                    return null
                }

                // Don't try to refresh the refresh endpoint itself
                if (response.request.url.encodedPath.contains("auth/refresh")) {
                    return null
                }

                val storedRefreshToken = runBlocking {
                    sessionManager.refreshToken.firstOrNull()
                }

                if (storedRefreshToken.isNullOrEmpty()) {
                    Log.w("CineVaultAuth", "No refresh token available, cannot refresh")
                    return null
                }

                Log.d("CineVaultAuth", "Access token expired, attempting refresh...")

                return try {
                    // Use a plain OkHttpClient (no interceptors) to call refresh
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
                        val refreshData = gson.fromJson(bodyString, RefreshResponseData::class.java)

                        if (refreshData?.accessToken != null) {
                            // Save new tokens
                            runBlocking {
                                sessionManager.updateToken(refreshData.accessToken)
                                refreshData.refreshToken?.let { sessionManager.saveRefreshToken(it) }
                            }
                            Log.d("CineVaultAuth", "Token refreshed successfully")

                            // Retry the original request with the new token
                            response.request.newBuilder()
                                .header("Authorization", "Bearer ${refreshData.accessToken}")
                                .header("X-Token-Refreshed", "true")
                                .build()
                        } else {
                            Log.e("CineVaultAuth", "Refresh response missing accessToken")
                            null
                        }
                    } else {
                        Log.e("CineVaultAuth", "Token refresh failed: ${refreshResponse.code}")
                        // Refresh token is also invalid — clear session
                        if (refreshResponse.code == 401) {
                            runBlocking { sessionManager.clearSession() }
                        }
                        null
                    }
                } catch (e: Exception) {
                    Log.e("CineVaultAuth", "Token refresh error", e)
                    null
                }
            }
        }
    }

    /** Simple data class for parsing refresh response (can't use Retrofit models here) */
    private data class RefreshResponseData(
        val accessToken: String?,
        val refreshToken: String?,
    )

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor, tokenAuthenticator: Authenticator): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
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
