package com.sanid.lib.debugghost.commands;

import android.content.Context;

/**
 * Created by norbertmoehring on 05/12/2016.
 */

public abstract class AbstractGhostCommand implements GhostCommand {

    protected static final String LOG_TAG = "GhostCommand";

    protected final Context mContext;
    protected String mLabel;
    protected String mKey;
    protected String mValue;

    public AbstractGhostCommand(Context context, String label, String key, String value) {
        mContext = context;
        mLabel = label;
        mKey = key;
        mValue = value;
    }

    public void setLabel(String label) {
        this.mLabel = label;
    }

    public void setKey(String key) {
        this.mKey = key;
    }

    public void setValue(String value) {
        this.mValue = value;
    }

    @Override
    public String getLabel() {
        return mLabel;
    }

    @Override
    public String getKey() {
        return mKey;
    }

    @Override
    public String getValue() {
        return mValue;
    }

    @Override
    public abstract void execute(String data);
}
