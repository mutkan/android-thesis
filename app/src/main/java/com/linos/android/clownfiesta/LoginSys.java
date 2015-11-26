package com.linos.android.clownfiesta;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.squareup.okhttp.ResponseBody;

import java.io.IOException;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.Header;
import retrofit.http.Headers;
import retrofit.http.POST;

public class LoginSys extends Activity{
    Activity LoginActivity = this;
    private final static String TAG = "LoginSys";
    public final static String BASE_URL = "http://83.212.119.253:8000";
    private Activity activity;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"Entering onCreate");
        super.onCreate(savedInstanceState);
        activity = this;
        setContentView(R.layout.activity_login_sys);
        SharedPreferences prefs = getSharedPreferences("com.linos.android.clownfiesta",MODE_PRIVATE);
        String token = prefs.getString("token", "Not Found");
        if (!token.equals("Not Found")){
            Log.d(TAG, token);
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            TokenChecking TokenChecking = retrofit.create(TokenChecking.class);
            Call<ResponseBody> call = TokenChecking.tokenauth("Token "+token);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Response<ResponseBody> response, Retrofit retrofit) {
                    if (response.isSuccess()){
                        Intent intent = new Intent(activity, CameraActivity.class);
                        startActivity(intent);
                        finish();
                    }else try{
                        Log.d(TAG, response.errorBody().string());

                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Throwable t) {

                }
            });

        }else  if (savedInstanceState==null){
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, new LoginFragment())
                    .commit();
        }
    }

    public void EnableRegisterFragment(View view) {
        RegisterFragment regFrag = new RegisterFragment();
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.container, regFrag)
                        .addToBackStack(null)
                        .commit();
    }


    public interface TokenChecking{
        @POST("android/respectmyauthoritay/")
        Call<ResponseBody> tokenauth(@Header("Authorization") String token);
    }
}
