package com.linos.android.clownfiesta;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class LoginSys extends Activity{

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
}
