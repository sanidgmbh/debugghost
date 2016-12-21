package com.sanid.lib.debugghost.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by norbertmoehring on 03/12/2016.
 */

public class GhostUtils {

    private static SimpleDateFormat serverDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

    public static String getApplicationName(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
    }

    public static Bitmap getApplicationIcon(Context context) {
        Drawable icon = null;
        final String packageName = context.getPackageName();

        try {
            icon = context.getPackageManager().getApplicationIcon(packageName);
            Bitmap b = drawableToBitmap(icon);
            return b; //Bitmap.createScaledBitmap(b, 120, 120, false);
        } catch ( PackageManager.NameNotFoundException e ) {
            //e.printStackTrace();
        }

        return null;
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static String getDeviceInfos(Context context) {
        Map<String, String> infos = new LinkedHashMap<>();

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int densityDpi = dm.densityDpi;

        infos.put("API Level", String.valueOf(Build.VERSION.SDK_INT));
        infos.put("OS Version", Build.VERSION.RELEASE);
        infos.put("Model", Build.MODEL);
        infos.put("Manufacturer", Build.MANUFACTURER);
        infos.put("Brand", Build.BRAND);
        infos.put("Device / Product", Build.DEVICE + " / " + Build.PRODUCT);
        infos.put("Kernel-Version", System.getProperty("os.version"));

        StringBuilder sb = new StringBuilder();

        sb.append("<table class=\"table table-striped\">");
        sb.append("<tbody>");

        for (Map.Entry<String, String> deviceInfo: infos.entrySet()) {
            sb.append("<tr><th>");
            sb.append(deviceInfo.getKey());
            sb.append("</th><td>");
            sb.append(deviceInfo.getValue());
            sb.append("</td></tr>");
        }
        sb.append("<tbody>");
        sb.append("</table>");

        return sb.toString();
    }

    public static String getScreenInfos(Context context) {
        Map<String, String> infos = new LinkedHashMap<>();

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int densityDpi = dm.densityDpi;
        float density = dm.density;
        int height = dm.heightPixels;
        int width = dm.widthPixels;

        infos.put("Screen", String.valueOf(width + " x " + height));
        infos.put("Density / DPI", String.valueOf(densityDpi) + " / " + getHumanReadableDpi(density));
        infos.put("Current orientation", getHumanReadableOrientation(context));
        infos.put("Display", Build.DISPLAY);


        StringBuilder sb = new StringBuilder();

        sb.append("<table class=\"table table-striped\">");
        sb.append("<tbody>");

        for (Map.Entry<String, String> deviceInfo: infos.entrySet()) {
            sb.append("<tr><th>");
            sb.append(deviceInfo.getKey());
            sb.append("</th><td>");
            sb.append(deviceInfo.getValue());
            sb.append("</td></tr>");
        }
        sb.append("<tbody>");
        sb.append("</table>");

        return sb.toString();
    }

    private static String getHumanReadableOrientation(Context context) {
        switch (context.getResources().getConfiguration().orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                return "Landscape";
            case Configuration.ORIENTATION_PORTRAIT:
                return "Portrait";
            default:
                return "unknown";
        }
    }

    private static String getHumanReadableDpi(float density) {
        String densityName = "unknown";

        if (density == 0.75) {
            densityName = "ldpi";
        } else if (density == 1.0) {
            densityName = "mdpi";
        } else if (density == 1.5) {
            densityName = "hdpi";
        } else if (density == 2.0) {
            densityName = "xhdpi";
        } else if (density == 3.0) {
            densityName = "xxhdpi";
        } else if (density == 4.0) {
            densityName = "xxxhdpi";
        } else if (density < 0.75) {
            densityName = "lower then ldpi";
        } else if (density > 4.0) {
            densityName = "higher then xxxhdpi";
        }

        return densityName;
    }

    public static String getNoDatabaseAlert(GhostDBHelper databaseHelper) {
        if (databaseHelper != null) {
            return "";
        } else {
            String warn = "<div class=\"alert alert-warning\" role=\"alert\">\n" +
                    "    <strong>No database set</strong><br />\n" +
                    "    Use the intent flags<br />\n" +
                    "    <span class=\"code\">DebugGhostService.INTENT_EXTRA_DB_NAME</span> and<br />\n" +
                    "    <span class=\"code\">DebugGhostService.INTENT_EXTRA_DB_VERSION</span> when starting the service.\n" +
                    "</div>";
            return warn;
        }
    }
    public static String getNoCommandsAlert(ArrayList<String> commands) {
        if (commands == null || commands.size() == 0) {
            String warn = "<div class=\"alert alert-warning\" role=\"alert\">\n" +
                    "    <strong>No Commands listed</strong><br />\n" +
                    "    Have a look at the left panel to learn see to add commands" +
                    "</div>";
            return warn;
        }
        return "";
    }

    public static String getServerTime() {
        Calendar calendar = Calendar.getInstance();
        serverDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return serverDateFormat.format(calendar.getTime());
    }

    public static String getCommandList(ArrayList<String> commands) {
        if (commands == null || commands.size() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (String commandPattern : commands) {
            String[] cmdSplit = commandPattern.split("~");
            if (cmdSplit.length != 3) {
                Log.e("DebugGhost", "Cannot read command pattern '"+commandPattern+"', skipping ...");
                continue;
            }

            if (cmdSplit[2].contains("[") == true && cmdSplit[2].contains("]") == true) {
                continue;
            }

            sb.append("<button type=\"button\" style=\"margin-bottom: 5px;\" class=\"btn btn-default\" onclick=\"postCommand('/commands/");
            sb.append(cmdSplit[1]);
            sb.append("','");
            sb.append(cmdSplit[2]);
            sb.append("');\">");
            sb.append(cmdSplit[0]);
            sb.append("</button>");
        }

        return sb.toString();
    }

    // TODO try to remember the last value if the user changed it in the text field
    public static String getCommandInputList(ArrayList<String> commands) {
        if (commands == null || commands.size() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (String commandPattern : commands) {
            String[] cmdSplit = commandPattern.split("~");
            if (cmdSplit.length != 3) {
                Log.e("DebugGhost", "Cannot read command pattern '"+commandPattern+"', skipping ...");
                continue;
            }
            if (cmdSplit[2].contains("[") == false && cmdSplit[2].contains("]") == false) {
                continue;
            }

            String realValue = cmdSplit[2].replace("[", "").replace("]", "");

            sb.append("<div class=\"input-group\" style=\"margin-bottom: 5px;\">");
            sb.append("<span class=\"input-group-btn\">");
            sb.append("<button class=\"btn btn-default\" type=\"button\" onclick=\"postCommandValue('/commands/");
            sb.append(cmdSplit[1]);
            sb.append("','");
            sb.append(cmdSplit[1]);
            sb.append("');\" >");
            sb.append(cmdSplit[0]);
            sb.append("</button></span>");
            sb.append("<input id=\""+cmdSplit[1]+"\" type=\"text\" class=\"form-control\" placeholder=\"set your value here\" value=\""+realValue+"\">");
            sb.append("</div>");
        }

        return sb.toString();
    }
}
