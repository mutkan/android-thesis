package com.linos.android.clownfiesta;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class CameraActivity extends Activity  implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST=9000;

    /*
    * Building request
    * */
    private LocationRequest mLocationRequest;

    /*
    * The main fragment we're gonna use
    * */
    private CameraFragment mCameraFragment;
    /*
    * Google Client , in order to use google services(location)
    * */
    private GoogleApiClient client;

    /*
    * A final tag for debugging
    * */
    private final String TAG ="CameraActivity";
    /* Improve performance.We dont have to evaluate the token all the time */
    @Override
    public void onBackPressed(){
        moveTaskToBack(true);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        /*  if we didnt save anything on last onPause() then create the main Fragment  */
        if( savedInstanceState == null){
            mCameraFragment = CameraFragment.newInstance();
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, mCameraFragment).commit();
        }
        /* Build a new client , set this class to implement the two following interfaces
        *  and include LocationServices API  */
        client = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        /*
        * Build a Location request , asking for the right priority and intervals between gps checks
        * */
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(3*1000)
                .setFastestInterval(1 * 1000);

    }
    public void MenuBtn(View view){
        SharedPreferences prefs = getSharedPreferences("com.linos.android.clownfiesta",MODE_PRIVATE);
        String token = prefs.getString("token", "Not Found");
        if (!token.equals("Not Found")){
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("token");
            editor.apply();

            Intent intent = new Intent(this,LoginSys.class);
            startActivity(intent);
            finish();
        }
    }
    @Override
    protected void onResume(){
        super.onResume();
        client.connect();
    }

    @Override
    protected void onPause(){
        super.onPause();
        if(client.isConnected()){
            LocationServices.FusedLocationApi.removeLocationUpdates(client,this);
            client.disconnect();
        }
    }
    /* Once we're connected to google client */
    @Override
    public void onConnected(Bundle bundle) {
        Location location = LocationServices.FusedLocationApi.getLastLocation(client);
        if (location !=null)
            newLocationHandler(location);
        LocationServices.FusedLocationApi.requestLocationUpdates(client, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
    /*
    * If connection fails start another activity to solve the issue
    * */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if(connectionResult.hasResolution()){
            try{
                connectionResult.startResolutionForResult(this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
            }catch (IntentSender.SendIntentException e){
                e.printStackTrace();
            }
        }else{
            Log.i(TAG,"Location services connection failed with code "
                    +connectionResult.getErrorCode());
        }
    }
    /*
    * Handle new incoming location updates
    * */
    private void newLocationHandler(Location location){
        Log.d(TAG, "New location came..");
        mCameraFragment.setGPStext(location.getLatitude() + " , "+location.getLongitude());
    }
    /*
    * This is called once new Location is available
    * */
    @Override
    public void onLocationChanged(Location location) {
        newLocationHandler(location);
    }
}