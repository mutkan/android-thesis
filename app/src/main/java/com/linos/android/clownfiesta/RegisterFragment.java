package com.linos.android.clownfiesta;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.ResponseBody;

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


public class RegisterFragment extends Fragment {
    /*---------------------------------Declarations/Initializations-------------------------------*/
    private EmailValidator validator;
    private EditText email;
    private EditText password;
    private EditText username;
    private Activity activity;
    private static final String TAG = "RegisterFragment ";
    public RegisterFragment() {
        // Required empty public constructor
    }

    /* -------------------------------Fragment related methods ------------------------------------*/
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
    /*-------------------------------------Custom methods-----------------------------------------*/
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
                    .baseUrl(LoginSys.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        ServerAPI sResponse = retrofit.create(ServerAPI.class);
        final EditText username = (EditText)activity.findViewById(R.id.username) ;
        EditText password = (EditText)activity.findViewById(R.id.password) ;
        final EditText email = (EditText)activity.findViewById(R.id.email) ;
        Call<ResponseBody> call = sResponse.create(username.getText().toString(), password.getText().toString(), email.getText().toString());
        call.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Response<ResponseBody> response, Retrofit retrofit) {
                    if (response.isSuccess()){
                        // If registration is successful , jump back to Login Fragment
                        Toast.makeText(activity, "You registered successfully" ,Toast.LENGTH_LONG).show();
                        getFragmentManager().popBackStack();
                    }else try {
                        String ErrorMessagesJSON = response.errorBody().string();
                        //Log.d(TAG,ErrorMessages);
                        Gson gson = new GsonBuilder().create();
                        GsonResponse serverResponse = gson.fromJson(ErrorMessagesJSON, GsonResponse.class);
                        /* if there is an email error */
                        if (serverResponse.email != null) {
                            email.setError(null);
                            email.setError(serverResponse.email.get(0));
                        }
                        /* if there is a username error */
                        if (serverResponse.username != null) {
                            username.setError(null);
                            username.setError(serverResponse.username.get(0));
                        }

                    }catch (IOException e){
                        e.printStackTrace();
                    }

            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
                Toast.makeText(activity, "Server is unavailable", Toast.LENGTH_LONG).show();
            }
        });
    }

    /*---------------------------------Network Classes and Interfaces ----------------------------*/
    public interface ServerAPI{
        // a method to create Users through django rest framework
        @FormUrlEncoded
        @POST("android/register/")
        Call<ResponseBody> create(@Field("username") String username,@Field("password") String password,@Field("email") String email);
    }

    /*
    * Server's Response converter ( JSON to Java objects )  in case of failure ( e.g username or email is in use )
    */
    class GsonResponse {
        private ArrayList<String> username;

        private ArrayList<String> email;

    }
}
