package de.devfest.hamburg.twozerommo.service

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST

interface IGameEndpoint {
    @POST("turnStart/")
    fun postTurnStart(): Observable<ResponseBody>

    @POST("move/")
    fun postMove(@Body move: Move): Observable<ResponseBody>

    @POST("gameOver/")
    fun postGameOver(@Body move: Move): Observable<ResponseBody>
}