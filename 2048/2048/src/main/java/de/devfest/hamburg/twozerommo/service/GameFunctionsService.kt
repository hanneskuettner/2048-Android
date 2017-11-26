package de.devfest.hamburg.twozerommo.service

import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GetTokenResult
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.logging.HttpLoggingInterceptor



object GameFunctionsService {
    val FIREBASE_FUNCTIONS_URL = "https://us-central1-appfest-hamburg-2017.cloudfunctions.net/"

    private var retrofit: Retrofit? = null
    private var gameFunctionsEndpoint: IGameEndpoint? = null

    fun buildEndpoint(token: String) {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY

        val httpClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val original = chain.request()

                    val request = original.newBuilder()
                            .header("Authorization", token)
                            .method(original.method(), original.body())
                            .build()

                    chain.proceed(request)
                }
                .addInterceptor(interceptor)
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
        val user = FirebaseAuth.getInstance().currentUser
        user?.getToken(true)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful()) {
                    val idToken = task.result.token
                    if (idToken != null)
                        buildEndpoint(idToken)
                } else {
                    Log.e("GAME", "Firebase Cloud service setup failed ${task.getException()}")
                }
            }
    }

    fun startTurn(user: GameUser): Observable<ResponseBody>? {
        return gameFunctionsEndpoint?.postTurnStart(user.uid)
                ?.subscribeOn(Schedulers.newThread())
                ?.observeOn(AndroidSchedulers.mainThread())
    }

    fun move(move: Move, user: GameUser): Observable<ResponseBody>? {
        return gameFunctionsEndpoint?.postMove(move, user.uid)
            ?.subscribeOn(Schedulers.newThread())
            ?.observeOn(AndroidSchedulers.mainThread())
    }


}