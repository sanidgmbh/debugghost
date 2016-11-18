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
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.sanid.lib.debugghost.server.GhostServerThread;

import static android.content.ContentValues.TAG;

public class DebugGhostService extends Service {

    public static final String INTENT_EXTRA_DB_NAME = "INTENT_EXTRA_DB_NAME";
    public static final String INTENT_EXTRA_DB_VERSION = "INTENT_EXTRA_DB_VERSION";

    private GhostServerThread serverThread = null;

    private WindowManager windowManager;
    private GestureDetector gestureDetector;
    private ImageView ivGhost;

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

        /*
        // the following is to show the little ghost on the screen which can be dragged. not of interest at the moment


        gestureDetector = new GestureDetector(this, new SingleTapConfirm());
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        ivGhost = new ImageView(this);
        ivGhost.setImageResource(R.mipmap.ic_ghost);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 100;

        configureGhost(params);

        windowManager.addView(ivGhost, params);
        */
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        handleGhostNotification();

        if (intent != null && intent.hasExtra(INTENT_EXTRA_DB_NAME) && intent.hasExtra(INTENT_EXTRA_DB_VERSION)) {

            String dbName = intent.getStringExtra(INTENT_EXTRA_DB_NAME);
            int dbVersion = intent.getIntExtra(INTENT_EXTRA_DB_VERSION, 0);

            serverThread = new GhostServerThread(getBaseContext(), dbName, dbVersion);
            Log.d(TAG, "starting ghost server ... ");
            serverThread.start();
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
        if (ivGhost != null) windowManager.removeView(ivGhost);

        serverThread.interrupt();
        serverThread = null;

        unregisterReceiver(stopServiceReceiver);
        dismissMusicNotification();

        super.onDestroy();
    }

    private class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true;
        }

    }

    private static final int GHOST_NOTIFICATION_ID = 666;

    public static final int INTENT_EXTRA_GHOST_SERVICE_ACTION_STOP = 0;
    public static final String INTENT_EXTRA_GHOST_SERVICE_ACTION = "INTENT_EXTRA_GHOST_SERVICE_ACTION";

    public void dismissMusicNotification() {
        NotificationManager mNotificationManager = (NotificationManager) getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);;
        mNotificationManager.cancel(GHOST_NOTIFICATION_ID);
    }

    @SuppressLint("NewApi")
    public void handleGhostNotification() {

        registerReceiver(stopServiceReceiver, new IntentFilter("GhostServiceFilter"));

        Notification.Builder builder = new Notification.Builder(getBaseContext())
                .setSmallIcon(R.mipmap.ic_ghost)
                .setContentTitle("DebugGhost")
                .setContentText("Running. Click to stop service!")
                .setOngoing(true);

        Intent resultIntent = new Intent(getBaseContext(), DebugGhostService.class);
        resultIntent.putExtra(INTENT_EXTRA_GHOST_SERVICE_ACTION, INTENT_EXTRA_GHOST_SERVICE_ACTION_STOP);
        resultIntent.setAction(Intent.ACTION_DEFAULT);

        PendingIntent resultPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent("GhostServiceFilter"), PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);;

        int apiLevel = Build.VERSION.SDK_INT;
        if (apiLevel >= Build.VERSION_CODES.JELLY_BEAN){
            mNotificationManager.notify(GHOST_NOTIFICATION_ID, builder.build());
        } else{
            mNotificationManager.notify(GHOST_NOTIFICATION_ID, builder.getNotification());
        }

    }

    // We need to declare the receiver with onReceive function as below
    protected BroadcastReceiver stopServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopSelf();
        }
    };
}
