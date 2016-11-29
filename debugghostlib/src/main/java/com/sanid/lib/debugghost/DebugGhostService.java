package com.sanid.lib.debugghost;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.sanid.lib.debugghost.server.GhostServer;

public class DebugGhostService extends Service implements GhostServer.OnServerListener {

    private static final String LOG_TAG = "DebugGhost";

    public static final String INTENT_EXTRA_DB_NAME = "INTENT_EXTRA_DB_NAME";
    public static final String INTENT_EXTRA_DB_VERSION = "INTENT_EXTRA_DB_VERSION";

    private static final String INTENT_SERVICE_FILTER = "DebugGhostServiceIntentFilter";
    private static final String INTENT_SERVICE_ACTION = "INTENT_SERVICE_ACTION";
    private static final int INTENT_SERVICE_ACTION_STOP_SERVICE = 1;
    private static final int INTENT_SERVICE_ACTION_STOP_SERVER = 2;
    private static final int INTENT_SERVICE_ACTION_START_SERVER = 3;
    private static final int INTENT_SERVICE_ACTION_SHOW_GHOST_PANEL = 4;

    private static final int GHOST_NOTIFICATION_ID = 666;

    private GhostServer ghostServer = null;

    private WindowManager windowManager;
    private GestureDetector gestureDetector;
    private ImageView ivGhost;
    private WindowManager.LayoutParams mWindowParams;
    private boolean ghostVisible = false;

    public DebugGhostService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Not used
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // the following is to show the little ghost on the screen which can be dragged. not of interest at the moment
        gestureDetector = new GestureDetector(this, new SingleTapConfirm());
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        ivGhost = new ImageView(this);
        ivGhost.setImageResource(R.drawable.ic_ghost);

        mWindowParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mWindowParams.gravity = Gravity.TOP | Gravity.LEFT;
        mWindowParams.x = 0;
        mWindowParams.y = mWindowParams.height - 100;


        configureGhost(mWindowParams);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerReceiver(stopServiceReceiver, new IntentFilter(INTENT_SERVICE_FILTER));

        handleGhostNotification(null);

        if (intent != null && intent.hasExtra(INTENT_EXTRA_DB_NAME) && intent.hasExtra(INTENT_EXTRA_DB_VERSION)) {

            String dbName = intent.getStringExtra(INTENT_EXTRA_DB_NAME);
            int dbVersion = intent.getIntExtra(INTENT_EXTRA_DB_VERSION, 0);

            ghostServer = new GhostServer(getBaseContext(), dbName, dbVersion);
            ghostServer.setOnServerListener(this);
            ghostServer.start();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void configureGhost(final WindowManager.LayoutParams params) {
        ivGhost.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    Toast.makeText(getBaseContext(), "Buhuu", Toast.LENGTH_SHORT).show();
                    return true;
                } else {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            return true;
                        case MotionEvent.ACTION_UP:
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(ivGhost, params);
                            return true;
                    }
                    return false;
                }
            }
        });

    }

    @Override
    public void onDestroy() {
        if (ivGhost != null && ghostVisible) {
            hideGhostPanel();
        }

        unregisterReceiver(stopServiceReceiver);
        dismissGhostNotification();

        super.onDestroy();
    }

    @Override
    public void onServerStarted(String ip, int port) {
        handleGhostNotification(ip + ":" + port);
    }

    @Override
    public void onServerStopped() {
        handleGhostNotification(null);
    }

    private void resumeServer() {
        if (ghostServer != null){
            ghostServer.start();
        }
    }

    public void stopServer() {
        if (ghostServer != null){
            ghostServer.stop();
        }
    }

    private class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true;
        }

    }

    public void dismissGhostNotification() {
        NotificationManager mNotificationManager = (NotificationManager) getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);;
        mNotificationManager.cancel(GHOST_NOTIFICATION_ID);
    }

    @SuppressLint("NewApi")
    public void handleGhostNotification(String serverAddress) {
        String title = (serverAddress != null) ? LOG_TAG + " ("+serverAddress+")" : LOG_TAG;

        Intent serviceIntent = new Intent(INTENT_SERVICE_FILTER);

        Notification.Builder builder = new Notification.Builder(getBaseContext())
                .setSmallIcon(R.drawable.ic_ghost)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_ghost))
                .setContentTitle(title)
                .setPriority(Notification.PRIORITY_MAX)
                .setOngoing(true);

        Intent resultIntent = new Intent(getBaseContext(), DebugGhostService.class);
        resultIntent.putExtra(INTENT_SERVICE_ACTION, INTENT_SERVICE_ACTION_STOP_SERVICE);
        resultIntent.setAction(Intent.ACTION_DEFAULT);

        serviceIntent.putExtra(INTENT_SERVICE_ACTION, INTENT_SERVICE_ACTION_STOP_SERVICE);
        PendingIntent resultPendingIntent = PendingIntent.getBroadcast(this, INTENT_SERVICE_ACTION_STOP_SERVICE, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);

        int apiLevel = Build.VERSION.SDK_INT;
        if (apiLevel >= Build.VERSION_CODES.KITKAT_WATCH) {

            Intent ghostIntent = new Intent(INTENT_SERVICE_FILTER);
            String action;
            int actionId = -1;
            int iconResId = -1;
            if (ghostServer != null && ghostServer.getStatus() == GhostServer.STATUS_RUN) {
                builder.setSmallIcon(R.drawable.ic_start_server);
                builder.setContentText("Server running. Click to stop service!");

                action = "Stop Server";
                iconResId = R.drawable.ic_stop_server;
                actionId = INTENT_SERVICE_ACTION_STOP_SERVER;
            } else {
                builder.setContentText("Server stopped. Click to stop service!");
                builder.setSmallIcon(R.drawable.ic_stop_server);
                action = "Start Server";
                iconResId = R.drawable.ic_start_server;
                actionId = INTENT_SERVICE_ACTION_START_SERVER;
            }

            ghostIntent.putExtra(INTENT_SERVICE_ACTION, actionId);
            PendingIntent pIntentStopServer = PendingIntent.getBroadcast(this, actionId, ghostIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Notification.Action.Builder actionBuilder = new Notification.Action.Builder(iconResId, action, pIntentStopServer);

            builder.addAction(actionBuilder.build());

            Intent ghostPanelIntent = new Intent(INTENT_SERVICE_FILTER);
            ghostPanelIntent.putExtra(INTENT_SERVICE_ACTION, INTENT_SERVICE_ACTION_SHOW_GHOST_PANEL);
            PendingIntent pIntentShowGhost = PendingIntent.getBroadcast(this, INTENT_SERVICE_ACTION_SHOW_GHOST_PANEL, ghostPanelIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Notification.Action.Builder actionShowGhostBuilder = new Notification.Action.Builder(R.drawable.ic_show_ghost, "Ghost", pIntentShowGhost);

            builder.addAction(actionShowGhostBuilder.build());
        }

        if (apiLevel >= Build.VERSION_CODES.JELLY_BEAN){
            mNotificationManager.notify(GHOST_NOTIFICATION_ID, builder.build());
        } else{
            mNotificationManager.notify(GHOST_NOTIFICATION_ID, builder.getNotification());
        }

    }

    private void bustGhost() {
        stopServer();
        stopSelf();
    }

    protected BroadcastReceiver stopServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                int action = intent.getIntExtra(INTENT_SERVICE_ACTION, INTENT_SERVICE_ACTION_STOP_SERVICE);

                switch (action) {
                    case INTENT_SERVICE_ACTION_SHOW_GHOST_PANEL:
                        Log.d(LOG_TAG, "Action: SHOW/HIDE GHOST_PANEL");
                        if (ghostVisible) {
                            hideGhostPanel();
                        } else {
                            showGhostPanel();
                        }
                        break;
                    case INTENT_SERVICE_ACTION_STOP_SERVER:
                        Log.d(LOG_TAG, "Action: STOP_SERVER");
                        stopServer();
                        break;
                    case INTENT_SERVICE_ACTION_START_SERVER:
                        Log.d(LOG_TAG, "Action: START_SERVER");
                        resumeServer();
                        break;
                    case INTENT_SERVICE_ACTION_STOP_SERVICE:
                        Log.d(LOG_TAG, "Action: STOP_SERVICE");
                        bustGhost();
                        break;
                }

            } else {
                Log.e(LOG_TAG, "GhostService Action should always have an actionId defined! Stopping full service because I don't know what to do!");
                bustGhost();
            }
        }
    };

    private void showGhostPanel() {
        ghostVisible = true;
        windowManager.addView(ivGhost, mWindowParams);
    }
    private void hideGhostPanel() {
        ghostVisible = false;
        windowManager.removeView(ivGhost);
    }

}
