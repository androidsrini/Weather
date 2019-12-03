package com.product.weatherapplication.service;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class WeatherWork extends Worker {

    private static final String TAG = "Weather";
    /*private static final String LAT_PARAM = "lat";
    private static final String LON_PARAM = "lon";
    private static final String WORK_RESULT = "weather";*/

    public WeatherWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        return Result.retry();

    }
}
