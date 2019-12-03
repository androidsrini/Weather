package com.product.weatherapplication.net;

import com.google.gson.JsonElement;

import java.util.HashMap;

import io.reactivex.Observable;

public class RequestHandler {

    public static final String APPID_PARAM = "appid";
    public static final String LAT_PARAM = "lat";
    public static final String LON_PARAM = "lon";

    private ApiCallInterface apiCallInterface;
    private static final RequestHandler requestHandler = new RequestHandler();

    public static RequestHandler GetInstance() {
        return requestHandler;
    }

    private RequestHandler() {
        this.apiCallInterface = NetworkModule.GetInstance().getApiCallInterface();
    }

    private HashMap<String, String> getWatherParam(double lat, double lon) {
        HashMap<String, String> param = new HashMap<>();
        param.put(APPID_PARAM, WebserviceUrls.APP_ID);
        param.put(LAT_PARAM, String.valueOf(lat));
        param.put(LON_PARAM, String.valueOf(lon));
        return param;
    }

    public Observable<JsonElement> fetchWeatherReport(double lat, double lon) {
        return apiCallInterface.fetchWeatherReport(getWatherParam(lat, lon));
    }
}
