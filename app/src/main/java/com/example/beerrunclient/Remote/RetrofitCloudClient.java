package com.example.beerrunclient.Remote;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitCloudClient {
    private static Retrofit instance;
    public static Retrofit getInstance(String paymentUrl ){

        if (instance==null)
            instance =new Retrofit.Builder()
                    .baseUrl(paymentUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build();
        return instance;
    }
}
