package de.devfest.hamburg.twozerommo.service

import android.util.Log
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

object GameFunctionsService {
    val FIREBASE_FUNCTIONS_URL = ""

    private var retrofit: Retrofit? = null
    private var gameFunctionsEndpoint: IGameEndpoint? = null

    fun buildEndpoint(token: String) {
        val httpClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val original = chain.request()

                    val request = original.newBuilder()
                            .header("User-Agent", "Your-App-Name")
                            .header("Accept", "application/vnd.yourapi.v1.full+json")
                            .method(original.method(), original.body())
                            .build()

                    chain.proceed(request)
                }
                .build()

        retrofit = Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(FIREBASE_FUNCTIONS_URL)
                .client(httpClient)
                .build()


        gameFunctionsEndpoint = retrofit?.create(IGameEndpoint::class.java)
    }

    fun setup() {
        val mUser = FirebaseAuth.getInstance().getCurrentUser()
        mUser.getToken(true)
                .addOnCompleteListener(object : OnCompleteListener<GetTokenResult>() {
                    fun onComplete(task: Task<GetTokenResult>) {
                        if (task.isSuccessful()) {
                            val idToken = task.getResult().getToken()
                            buildEndpoint(idToken)
                        } else {
                            Log.e("GAME", "Firebase Cloud service setup failed ${task.getException()}")
                        }
                    }
                })
    }

    fun startTurn(): Observable<ResponseBody>? {
        return gameFunctionsEndpoint?.postTurnStart()
                ?.subscribeOn(Schedulers.newThread())
                ?.observeOn(AndroidSchedulers.mainThread())
    }

    fun move(move: Move): Observable<ResponseBody>? {
        return gameFunctionsEndpoint?.postMove(move)
            ?.subscribeOn(Schedulers.newThread())
            ?.observeOn(AndroidSchedulers.mainThread())
    }

    
}