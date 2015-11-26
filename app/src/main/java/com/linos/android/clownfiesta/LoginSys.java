package com.linos.android.clownfiesta;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
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
        /*Intent intent = new Intent(this,CameraActivity.class);
        this.startActivity(intent);
        */
        RegisterFragment regFrag = new RegisterFragment();
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.container, regFrag)
                        .addToBackStack(null)
                        .commit();
    }

/*    public void LoginBtn(View view){
        //Toast.makeText(getActivity(), "YOO", Toast.LENGTH_SHORT).show();
        EditText EditTextPassword= (EditText) findViewById(R.id.Loginpassword);
        String password = EditTextPassword.getText().toString();
        EditTextPassword.setError(getString(R.string.error_invalid_password));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://83.212.119.253:8000")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        BasicAuthToken BasicAuthToken = retrofit.create(BasicAuthToken.class);
        Call<TokenCS> call = BasicAuthToken.login("admin","linoss123");
        call.enqueue(new Callback<TokenCS>() {
            @Override
            // TODO: Store Token
            public void onResponse(Response<TokenCS> response, Retrofit retrofit) {
                String str = response.code() + " " + response.body().toString();
                System.out.println(str);
                Toast.makeText(LoginActivity, str , Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();

                Toast.makeText(LoginActivity,t.toString(), Toast.LENGTH_LONG).show();
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
    }*/
   /* public void submitForm(View view){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://83.212.119.253:8000")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ServerAPI sResponse = retrofit.create(ServerAPI.class);
        EditText username = (EditText)findViewById(R.id.username) ;
        EditText password = (EditText)findViewById(R.id.password) ;
        EditText email = (EditText)findViewById(R.id.email) ;
        Call<ServerResponse> call = sResponse.create(username.getText().toString(), password.getText().toString(), email.getText().toString());
        call.enqueue(new Callback<ServerResponse>() {
            *//*
            * TODO: Handle several error cases , either here or from android interface
            * *//*
            @Override
            public void onResponse(Response<ServerResponse> response, Retrofit retrofit) {
                Toast.makeText(LoginActivity, "KOBLE", Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
                Toast.makeText(LoginActivity, "Register Failed.", Toast.LENGTH_SHORT).show();
            }
        });
        //Toast.makeText(getActivity(),"Submitting form..",Toast.LENGTH_LONG).show();
    }
*/


}
