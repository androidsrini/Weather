package com.product.weatherapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.product.weatherapplication.helper.Utility;
import com.product.weatherapplication.net.ApiResponse;
import com.product.weatherapplication.net.RequestHandler;
import com.product.weatherapplication.pojo.WeatherItem;
import com.product.weatherapplication.pojo.WeatherResponse;
import com.product.weatherapplication.service.WeatherWork;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final double CELSIUS_DEFAULT = 273.15;
    private Location location;
    private TextView locationTv;
    private TextView description;
    private TextView celsius;
    private GoogleApiClient googleApiClient;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private LocationRequest locationRequest;
    private static final long UPDATE_INTERVAL = 5000, FASTEST_INTERVAL = 5000;
    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private String[] permission = new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION};
    // integer for permissions results request
    private static final int ALL_PERMISSIONS_RESULT = 1011;
    private boolean isLocationNameAvailable = false;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private WorkManager mWorkManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationTv = findViewById(R.id.location);
        description = findViewById(R.id.description);
        celsius = findViewById(R.id.celsius);

        permissionsToRequest = permissionsToRequest(permission);
        //To show user locatio permission dialog
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest.size() > 0) {
                requestPermissions(permissionsToRequest.toArray(
                        new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
            }
        }
        googleApiClient = new GoogleApiClient.Builder(this).
                addApi(LocationServices.API).
                addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).build();
    }

    private void startWorkManager(){
        mWorkManager = WorkManager.getInstance();
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(WeatherWork.class, 2, TimeUnit.HOURS).build();
        mWorkManager.getWorkInfoByIdLiveData(workRequest.getId()).observe(this, new Observer<WorkInfo>() {
            @Override
            public void onChanged(@Nullable WorkInfo workInfo) {
                if (workInfo != null) {
                    fetchWeatherRequest(location.getLatitude(), location.getLongitude());
                }
            }
        });
        mWorkManager.enqueue(workRequest);
    }

    private void updateUI(WeatherResponse weatherResponse) {
        String descrption = "";
        for (WeatherItem weatherItem: weatherResponse.getWeather()) {
            descrption = weatherItem.getDescription() + " ";
        }
        isLocationNameAvailable = !TextUtils.isEmpty(weatherResponse.getName());
        locationTv.setText(weatherResponse.getName());
        description.setText(descrption);
        double wCelsius = 0.0;
        if (null != weatherResponse.getMain()) {
            wCelsius = weatherResponse.getMain().getTemp() - CELSIUS_DEFAULT;
        }
        celsius.setText(getString(R.string.celsuis_place_holder, String.valueOf(wCelsius)));
    }

    private void apiResponseHandler(ApiResponse apiResponse) {
        switch (apiResponse.status) {
            case LOADING:
                Utility.getInstance().showProgressDialog(this);
                break;
            case SUCCESS:
                Utility.getInstance().dismissDialog();
                WeatherResponse weatherResponse = new Gson().fromJson(apiResponse.data, WeatherResponse.class);
                if (null != weatherResponse) {
                    updateUI(weatherResponse);
                }
                break;
            case ERROR:
                Utility.getInstance().dismissDialog();
                break;
        }
    }

    private void fetchWeatherRequest(double lat, double lon) {
        compositeDisposable.add(RequestHandler.GetInstance().fetchWeatherReport(lat, lon)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(d->apiResponseHandler(ApiResponse.loading()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result->{
                    apiResponseHandler(ApiResponse.success(result));
                }, error->{
                    apiResponseHandler(ApiResponse.error(error));
                }));
    }

    /**
     * This method to check permision enable or not.
     * If permisison not enable then it will return permission list.
     * @param wantedPermissions
     * @return ArrayList<String>
     */
    private ArrayList<String> permissionsToRequest(String[] wantedPermissions) {
        ArrayList<String> result = new ArrayList<>();
        for (String perm : wantedPermissions) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }
        return result;
    }

    /**
     * This method to check permission enable or not.
     * If not enable it will return false.
     * @param permission
     * @return
     */
    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //To check play service suttable or not
        if (!checkPlayServices()) {
            locationTv.setText("You need to install Google Play Services to use the App properly");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // stop location updates
        if (googleApiClient != null  &&  googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
    }

    /**
     * This method to check play service is supported version
     * @return boolean
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST);
            } else {
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * This method to update cutrrent location vlaue in UI.
     * @param location
     */
    private void updateLocationUI(Location location) {
        if (!isLocationNameAvailable) {
            locationTv.setText("Latitude : " + location.getLatitude() + "\nLongitude : " + location.getLongitude());
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&  ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (location != null) {
            updateLocationUI(location);
        }
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&  ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "You need to enable permissions to display location !", Toast.LENGTH_SHORT).show();
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            this.location = location;
            updateLocationUI(location);
            if (!isLocationNameAvailable) {
                //isLocationNameAvailable = true;
                //fetchWeatherRequest(location.getLatitude(), location.getLongitude());
                startWorkManager();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case ALL_PERMISSIONS_RESULT:
                for (String perm : permissionsToRequest) {
                    if (!hasPermission(perm)) {
                        permissionsRejected.add(perm);
                    }
                }

                if (permissionsRejected.size() > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            new AlertDialog.Builder(MainActivity.this).
                                    setMessage("These permissions are mandatory to get your location. You need to allow them.").
                                    setPositiveButton("OK", (dialogInterface, i) -> {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            requestPermissions(permissionsRejected.
                                                    toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                        }
                                    }).setNegativeButton("Cancel", null).create().show();
                            return;
                        }
                    }
                } else {
                    if (googleApiClient != null) {
                        googleApiClient.connect();
                    }
                }
                break;
        }
    }
}
