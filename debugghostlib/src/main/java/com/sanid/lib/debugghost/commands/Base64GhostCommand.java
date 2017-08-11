package com.sanid.lib.debugghost.commands;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.sanid.lib.debugghost.utils.GhostUtils;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by sergeykern on 08/08/2017.
 */

public class Base64GhostCommand extends AbstractGhostCommand {

    public Base64GhostCommand(Context context, String label, String key, String value) {
        super(context, label, key, value);
    }

    @Override
    public void execute(String s) {

    }

    public static String encodeBase64(String str){
        return Base64.encodeToString(str.getBytes(), Base64.DEFAULT);
    }

    public static String decodeBase64(String str){
        return new String(Base64.decode(str, Base64.DEFAULT));
    }

}
