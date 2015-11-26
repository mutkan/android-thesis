package com.linos.android.clownfiesta;

import android.widget.EditText;

import java.util.regex.Pattern;

/**
 * Created by linos on 10/28/15.
 */
public class EmailValidator {
    private static final String EMAIL_REGEX = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    public boolean isEmailAddr(EditText editText){
        return isValid(editText,"Invalid E-mail",EMAIL_REGEX);
    }
    public boolean isValid(EditText editText,String errorMsg,String regex){
        String text = editText.getText().toString().trim();
        editText.setError(null);

        if(!Pattern.matches(regex,text)){
            editText.setError(errorMsg);
            return false;
        }
        return true;
    }
    public boolean Empty(EditText editText){
        editText.setError(null);
        if (editText.getText().length() == 0){
            editText.setError("Required Field");
            return true;
        }else
            return false;
    }
}
