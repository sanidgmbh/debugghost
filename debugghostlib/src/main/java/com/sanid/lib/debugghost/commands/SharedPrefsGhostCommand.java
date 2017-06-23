package com.sanid.lib.debugghost.commands;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.sanid.lib.debugghost.utils.GhostUtils;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by norbertmoehring on 21/12/2016.
 */

public class SharedPrefsGhostCommand extends AbstractGhostCommand {

    public SharedPrefsGhostCommand(Context context, String label, String key, String value) {
        super(context, label, key, value);
    }

    @Override
    public void execute(String s) {
        Map<String, List<String>> params;
        String prefsName = null;
        String fieldName = null;
        String fieldValue = null;
        String fieldType = null;
        try {
            params = GhostUtils.splitQuery(s);

            fieldType = params.get("fieldType").get(0);
            prefsName = params.get("prefs").get(0);
            fieldName = params.get("field").get(0);
            fieldValue = params.get("fieldValue").get(0);

            String[] prefsAndField = fieldName.split("_debugghostseperator_");
            prefsName = prefsAndField[0];
            fieldName = prefsAndField[1];

            SharedPreferences sharedPreferences = mContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            switch (fieldType) {
                case "Boolean":
                    editor.putBoolean(fieldName, ("true".equalsIgnoreCase(fieldValue)));
                    break;
                case "Integer":
                    editor.putInt(fieldName, Integer.valueOf(fieldValue));
                    break;
                case "String":
                    editor.putString(fieldName, fieldValue);
                    break;
                case "Float":
                    editor.putFloat(fieldName, Float.valueOf(fieldValue));
                    break;
                case "Long":
                    editor.putLong(fieldName, Long.valueOf(fieldValue));
                    break;
                case "HashSet":
                    String[] setValues = fieldValue.split("\r\n");
                    HashSet<String> stringHashSet = new HashSet<>();
                    for (String setVal : setValues) {
                        stringHashSet.add(setVal);
                    }
                    editor.putStringSet(fieldName, stringHashSet);
                    break;
                default:
                    Log.w("SharedPrefsGhostCommand", "Unsupported SharedPrefs type found: " + fieldType);
                    break;
            }
            editor.commit();
            Toast.makeText(mContext, "DebugGhost updated SharedPrefs\n'"+fieldName+"'", Toast.LENGTH_SHORT).show();

        } catch (UnsupportedEncodingException e) {
            Log.e("SharedPrefsGhostCommand", e.getMessage());
        } catch (Exception e) {
            Log.e("SharedPrefsGhostCommand", e.getMessage());
        }
    }

}
