package com.linos.android.clownfiesta;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.POST;


public class RegisterFragment extends Fragment {
    private EmailValidator validator;
    private EditText email;
    private EditText password;
    private EditText username;
    private Activity activity;

    public RegisterFragment() {
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
        return inflater.inflate(R.layout.fragment_register, container, false);
    }
    @Override
    public void onActivityCreated(Bundle bundle){
        super.onActivityCreated(bundle);
        activity = getActivity();
        username = (EditText) activity.findViewById(R.id.username);
        email =(EditText) activity.findViewById(R.id.email);
        password = (EditText) activity.findViewById(R.id.password);
        validator = new EmailValidator();
        Button submitBtn = (Button) activity.findViewById(R.id.SubmitBtn);
        setUpWatchers();

    }
    private void setUpWatchers(){
        email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                validator.isEmailAddr(email);
            }
        });
        Button SubmitBtn = (Button) activity.findViewById(R.id.SubmitBtn);
        SubmitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validator.isEmailAddr(email))
                    submitForm();
            }
        });
    }


    public void submitForm(){
        Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://83.212.119.253:8000")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        ServerAPI sResponse = retrofit.create(ServerAPI.class);
        EditText username = (EditText)activity.findViewById(R.id.username) ;
        EditText password = (EditText)activity.findViewById(R.id.password) ;
        EditText email = (EditText)activity.findViewById(R.id.email) ;
        Call<ServerResponse> call = sResponse.create(username.getText().toString(), password.getText().toString(), email.getText().toString());
        call.enqueue(new Callback<ServerResponse>() {

            //  TODO: Handle several error cases , either here or from android interface

            @Override
            public void onResponse(Response<ServerResponse> response, Retrofit retrofit) {
                try {
                    if (response.isSuccess()){
                        ;   //TODO: Handle success , jump to login fragment ?
                    }else{
                        Toast.makeText(activity, response.errorBody().string(), Toast.LENGTH_LONG).show();
                    }
                }catch (IOException e){
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

    public interface ServerAPI{
        @FormUrlEncoded
        @POST("android/register/")
        Call<ServerResponse> create(@Field("username") String username,@Field("password") String password,@Field("email") String email);
    }
    class ServerResponse {
        public String email;
        public List<String> username;
        public String passwordHASH;
        public String toString() {
            return username + " " + email + " " + passwordHASH;
        }
    }
}
