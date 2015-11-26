package com.linos.android.clownfiesta;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

public class LoginFragment extends Fragment {
    Activity activity = getActivity();

    public LoginFragment() {
        // Required empty public constructor
    }

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

    }

    public void login(){
        EditText EditTextPassword = (EditText) activity.findViewById(R.id.Loginpassword);
        String password = EditTextPassword.getText().toString();
        EditTextPassword.setError(getString(R.string.error_invalid_password));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://83.212.119.253:8000")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        BasicAuthToken BasicAuthToken = retrofit.create(BasicAuthToken.class);
        Call<TokenCS> call = BasicAuthToken.login("admin", "linoss123");
        call.enqueue(new Callback<TokenCS>() {
            @Override
            // TODO: Store Token
            public void onResponse(Response<TokenCS> response, Retrofit retrofit) {
                String str = response.code() + " " + response.body().toString();
                System.out.println(str);
                Toast.makeText(activity, str, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();

                Toast.makeText(activity, t.toString(), Toast.LENGTH_LONG).show();
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
