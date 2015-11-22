package com.linos.android.clownfiesta;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.Headers;
import retrofit.http.POST;


public class LoginSys extends Activity{
    Activity LoginActivity = this;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_sys);
        if (savedInstanceState==null){
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, new LoginFragment())
                    .commit();
        }
    }

    public void EnableRegisterFragment(View view) {
        Intent intent = new Intent(this,CameraActivity.class);
        this.startActivity(intent);

        /* RegisterFragment regFrag = RegisterFragment.newInstance("","");
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.container, regFrag)
                        .addToBackStack(null)
                        .commit();
        */
    }

    public void LoginBtn(View view){
        //Toast.makeText(getActivity(), "YOO", Toast.LENGTH_SHORT).show();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://83.212.119.253:8000")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        BasicAuthToken BasicAuthToken = retrofit.create(BasicAuthToken.class);
        Call<TokenCS> call = BasicAuthToken.login("admin","linoss123");
        call.enqueue(new Callback<TokenCS>() {
            @Override
            public void onResponse(Response<TokenCS> response, Retrofit retrofit) {
                String str = response.code() + " " + response.body().toString();
                System.out.println(str);
                Toast.makeText(LoginActivity, str , Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
                Toast.makeText(LoginActivity, "Login Failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public interface BasicAuthToken{
        @FormUrlEncoded
        @POST("api-token-auth/")
        Call<TokenCS> login(@Field("username") String username,@Field("password") String password);
    }
    class TokenCS {
        String token;
        public String toString() {
            return (token);
        }
    }


}
