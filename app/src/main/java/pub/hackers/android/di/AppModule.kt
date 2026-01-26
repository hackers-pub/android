package pub.hackers.android.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo.network.okHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import pub.hackers.android.data.local.SessionManager
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hackerspub_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideSessionManager(dataStore: DataStore<Preferences>): SessionManager {
        return SessionManager(dataStore)
    }

    @Provides
    @Singleton
    fun provideApolloClient(
        @ApplicationContext context: Context,
        sessionManager: SessionManager
    ): ApolloClient {
        val sqlNormalizedCacheFactory = SqlNormalizedCacheFactory(context, "apollo_cache.db")

        val authInterceptor = Interceptor { chain ->
            val token = runBlocking { sessionManager.sessionToken.first() }
            val request = if (token != null) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()

        return ApolloClient.Builder()
            .serverUrl("https://hackers.pub/graphql")
            .okHttpClient(okHttpClient)
            .normalizedCache(sqlNormalizedCacheFactory)
            .build()
    }
}
