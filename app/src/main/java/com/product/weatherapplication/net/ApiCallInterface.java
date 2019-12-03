package com.product.weatherapplication.net;

import com.google.gson.JsonElement;

import java.util.HashMap;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;

public interface ApiCallInterface {

    @GET(WebserviceUrls.WEATHER)
    Observable<JsonElement> fetchWeatherReport(@QueryMap HashMap<String, String> param);
}
