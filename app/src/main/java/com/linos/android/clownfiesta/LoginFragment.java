package com.linos.android.clownfiesta;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.ArrayList;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.POST;

public class LoginFragment extends Fragment {
    /*---------------------------------Declarations/Initializations-------------------------------*/
    private Activity activity;
    private EditText password;
    private EditText username;

    private static final String TAG = "LoginFragment ";
    public LoginFragment() {
        // Required empty public constructor
    }
    /*
    * -------------------------------Fragment related methods --------------------------------------
    * */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false);

    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = getActivity();
        Button LoginBtn = (Button) activity.findViewById(R.id.LoginBtn);
        LoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Add TextWatchers here
                if (true)
                    login();
            }
        });
        username = (EditText) activity.findViewById(R.id.Loginusername);
        password = (EditText) activity.findViewById(R.id.Loginpassword);

    }
    /*-------------------------------------Custom methods-----------------------------------------*/
    public void login(){
        String passwordText = password.getText().toString();
        String usernameText = username.getText().toString();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(LoginSys.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        BasicAuthToken BasicAuthToken = retrofit.create(BasicAuthToken.class);
        Call<TokenCS> call = BasicAuthToken.login(usernameText, passwordText);
        call.enqueue(new Callback<TokenCS>() {
            @Override
            // TODO: Store Token
            public void onResponse(Response<TokenCS> response, Retrofit retrofit) {
                if (response.isSuccess()){
                    String token = response.body().token;
                    SharedPreferences prefs = activity.getSharedPreferences("com.linos.android.clownfiesta", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("token", token);
                    editor.apply();

                    Intent intent = new Intent(activity,CameraActivity.class);
                    startActivity(intent);
                    activity.finish();
                }else try {
                    String ErrorMessagesJSON = response.errorBody().string();
                    Gson gson = new GsonBuilder().create();
                    ErrorGsonResponse Errors = gson.fromJson(ErrorMessagesJSON, ErrorGsonResponse.class);
                    if (Errors.username != null) {
                        username.setError(null);
                        username.setError(Errors.username.get(0));
                    }
                    if (Errors.password != null) {
                        password.setError(null);
                        password.setError(Errors.password.get(0));
                    }
                    if (Errors.non_field_errors != null) {
                        Toast.makeText(activity, Errors.non_field_errors.get(0), Toast.LENGTH_LONG).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();

                Toast.makeText(activity, t.toString(), Toast.LENGTH_LONG).show();
            }
        });
    }
    /*---------------------------------Network Classes and Interfaces ----------------------------*/
    public interface BasicAuthToken{
        @FormUrlEncoded
        @POST("api-token-auth/")
        Call<TokenCS> login(@Field("username") String username,@Field("password") String password);
    }
    class TokenCS {
        String token;
    }

    class ErrorGsonResponse {
        private ArrayList<String> username;
        private ArrayList<String> password;
        private ArrayList<String> non_field_errors;

    }
}
