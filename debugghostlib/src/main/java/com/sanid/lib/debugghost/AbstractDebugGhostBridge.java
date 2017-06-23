package com.sanid.lib.debugghost;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.sanid.lib.debugghost.commands.GhostCommand;
import com.sanid.lib.debugghost.commands.SharedPrefsGhostCommand;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by norbertmoehring on 05/12/2016.
 */

public abstract class AbstractDebugGhostBridge {

    private static final String LOG_TAG = AbstractDebugGhostBridge.class.getSimpleName();

    private GhostCommandBroadcastReceiver mCommandBroadcastReceiver = new GhostCommandBroadcastReceiver();

    protected final Context mContext;
    private final String mDatabaseName;
    private final int mDatabaseVersion;
    private final int mServerPort;

    private ArrayList<String> mStringCommands = new ArrayList<>();

    private Map<String, GhostCommand> mGhostCommands = new HashMap<>();

    public AbstractDebugGhostBridge(Context context) {
        this(context, null, -1);
    }

    public AbstractDebugGhostBridge(Context context, String databaseName, int databaseVersion) {
        this(context, databaseName, databaseVersion, 8080);

        addInternalGhostCommand(new SharedPrefsGhostCommand(context, "internal_ghost_shared_prefs_command", "internal_ghost_shared_prefs_command", " "));
    }

    public AbstractDebugGhostBridge(Context context, String databaseName, int databaseVersion, int serverPort) {
        this.mContext = context;
        this.mDatabaseName = databaseName;
        this.mDatabaseVersion = databaseVersion;
        this.mServerPort = serverPort;
    }

    private void addInternalGhostCommand(GhostCommand ghostCommand) {
        mGhostCommands.put(ghostCommand.getKey(), ghostCommand);
    }

    public void addGhostCommand(GhostCommand ghostCommand) {
        String key = ghostCommand.getKey();
        String label = ghostCommand.getLabel();
        String value = ghostCommand.getValue();

        if (key == null || key.length() == 0) {
            throw new IllegalArgumentException(GhostCommand.class.getSimpleName() + ": no key set!");
        } else if (label == null || label.length() == 0) {
            throw new IllegalArgumentException(GhostCommand.class.getSimpleName() + ": no label set!");
        } else if (key.contains("~") || label.contains("~") || (value != null && value.contains("~"))) {
            throw new IllegalArgumentException(GhostCommand.class.getSimpleName() + ": key, value or label must not use the char '~'. This is a reserved char for GhostCommands!");
        } else if (mGhostCommands.containsKey(key)) {
            Log.e(LOG_TAG, GhostCommand.class.getSimpleName() + " with key '"+key+"' already exists, ignoring!");
        } else if (mGhostCommands.containsKey(" ")) {
            throw new IllegalArgumentException(GhostCommand.class.getSimpleName() + ": key must not contain whitespace. Use 'this_is_my_key'-pattern!");
        } else {
            mStringCommands.add(ghostCommand.getLabel()+"~"+ghostCommand.getKey()+"~"+((value != null) ? value : ""));
            addInternalGhostCommand(ghostCommand);
        }
    }

    public GhostCommand getGhostCommandByKey(String key) {
        return mGhostCommands.get(key);
    }

    public final void startDebugGhost() {
        mContext.registerReceiver(mCommandBroadcastReceiver, new IntentFilter(DebugGhostService.INTENT_FILTER_COMMANDS));

        Intent serviceIntent = new Intent(mContext, DebugGhostService.class);
        serviceIntent.putExtra(DebugGhostService.INTENT_EXTRA_DB_NAME, mDatabaseName);
        serviceIntent.putExtra(DebugGhostService.INTENT_EXTRA_DB_VERSION, mDatabaseVersion);
        serviceIntent.putExtra(DebugGhostService.INTENT_EXTRA_SERVER_PORT, mServerPort);
        serviceIntent.putStringArrayListExtra(DebugGhostService.INTENT_EXTRA_COMMAND_MAP, mStringCommands);
        mContext.startService(serviceIntent);
    }

    public final void stopDebugGhost() {
        mContext.unregisterReceiver(mCommandBroadcastReceiver);

        Intent serviceIntent = new Intent(mContext, DebugGhostService.class);
        serviceIntent.putExtra(DebugGhostService.INTENT_EXTRA_DB_NAME, mDatabaseName);
        serviceIntent.putExtra(DebugGhostService.INTENT_EXTRA_DB_VERSION, mDatabaseVersion);
        serviceIntent.putExtra(DebugGhostService.INTENT_EXTRA_SERVER_PORT, mServerPort);
        serviceIntent.putStringArrayListExtra(DebugGhostService.INTENT_EXTRA_COMMAND_MAP, mStringCommands);
        mContext.stopService(serviceIntent);
    }

    private void executeGhostCommand(String key, String value) {
        GhostCommand ghostCommand = getGhostCommandByKey(key);

        if (ghostCommand != null) {
            ghostCommand.execute(value);
        }
    }

    private class GhostCommandBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String commandName = intent.getStringExtra(DebugGhostService.INTENT_SERVICE_DEBUG_COMMAND_NAME);
            String value = intent.getStringExtra(DebugGhostService.INTENT_SERVICE_DEBUG_COMMAND_VALUE);

            String decodedValue = value;
            try {
                decodedValue = java.net.URLDecoder.decode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.e(LOG_TAG, "Could not decode value '"+value+"'");
            }

            executeGhostCommand(commandName, decodedValue);
        }
    }
}
