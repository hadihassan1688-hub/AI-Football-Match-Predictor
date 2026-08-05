package com.example

import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class MatchResponse(val matches: List<Match>)

@JsonClass(generateAdapter = true)
data class Match(
    val id: Int,
    val homeTeam: Team,
    val awayTeam: Team,
    val utcDate: String
)

@JsonClass(generateAdapter = true)
data class Team(val name: String)

interface FootballDataApiService {
    @GET("v4/competitions/PL/matches")
    suspend fun getMatches(
        @Header("X-Auth-Token") apiKey: String
    ): MatchResponse
}

object FootballDataClient {
    private const val BASE_URL = "https://api.football-data.org/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val service: FootballDataApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FootballDataApiService::class.java)
    }
}

suspend fun fetchMatches(): Result<List<FootballMatch>> {
    val apiKey = BuildConfig.FootballDataAPI
    if (apiKey.isEmpty()) return Result.failure(Exception("API Key is missing"))

    return try {
        val response = FootballDataClient.service.getMatches(apiKey)
        Result.success(response.matches.map { 
            FootballMatch(it.id, it.homeTeam.name, it.awayTeam.name, it.utcDate.take(10), "Premier League") 
        })
    } catch (e: Exception) {
        Result.failure(e)
    }
}
