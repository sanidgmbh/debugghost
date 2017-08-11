package com.sanid.lib.debugghost.utils;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SizeF;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
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

    public static String getHardwareInfos(Context context) {
        Map<String, String> infos = new LinkedHashMap<>();

        try {
            infos.put("CPU info", readCPUInfo());
            infos.put("MEM info", readMemInfo());
            infos.put("Camera Info", getCameraInfos(context));
        } catch (IOException ex){

        }

        StringBuilder sb = new StringBuilder();

        sb.append("<table class=\"table table-striped\">");
        sb.append("<tbody>");

        int counter = -1;
        for (Map.Entry<String, String> deviceInfo: infos.entrySet()) {
            counter++;
            sb.append("<tr><th data-toggle=\"collapse\" data-target=\"#collapseme" + counter + "\">");
            sb.append(deviceInfo.getKey());
            sb.append("</th><td><div class=\"collapse out\" id=\"collapseme" + counter + "\">");
            sb.append(deviceInfo.getValue());
            sb.append("</div></td></tr>");
        }
        sb.append("<tbody>");
        sb.append("</table>");

        return sb.toString();
    }

    private static String readCPUInfo() throws IOException {
        ProcessBuilder cmd;
        String result="";

        try{
            String[] args = {"/system/bin/cat", "/proc/cpuinfo"};
            cmd = new ProcessBuilder(args);

            Process process = cmd.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line = null;

            while((line = in.readLine()) != null){
                result = result + "<br/>" + line;
            }
            in.close();
        } catch(IOException ex){
            ex.printStackTrace();
        }
        return result;
    }

    private static String readMemInfo() throws IOException {
        ProcessBuilder cmd;
        String result="";

        try{
            String[] args = {"/system/bin/cat", "/proc/meminfo"};
            cmd = new ProcessBuilder(args);

            Process process = cmd.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line = null;

            while((line = in.readLine()) != null){
                result = result + "<br/>" + line;
            }
            in.close();
        } catch(IOException ex){
            ex.printStackTrace();
        }
        return result;
    }

    private static String getCameraInfos(Context context){
        String cameraInfos = "";

        if (checkCameraHardware(context)){
            //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            PackageManager pm = context.getPackageManager();
            int hasPerm = pm.checkPermission(
                    Manifest.permission.CAMERA,
                    context.getPackageName());
            if (hasPerm == PackageManager.PERMISSION_GRANTED) {
                int numberOfCameras = Camera.getNumberOfCameras();
                for (int i = 0; i < numberOfCameras; i++) {
                    Camera camera = getCameraInstance(i);
                    if (camera != null) {
                        cameraInfos += "<br/>";

                        if (i == Camera.CameraInfo.CAMERA_FACING_BACK) {
                            cameraInfos += "<h4>Back-Camera:</h4>";
                        } else
                        if (i == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                            cameraInfos += "<h4>Front-Camera:</h4>";
                        } else {
                            cameraInfos += "<h4>Camera (" + (i+1) + "):</h4>";
                        }

                        Camera.Parameters params = camera.getParameters();

                        cameraInfos += "Max zoom: " + params.getMaxZoom() + "<br/>";

                        cameraInfos += "Max focal length: " + params.getFocalLength();
                        cameraInfos += "<br/>";

                        List<Camera.Size> pictureSizes = params.getSupportedPictureSizes();
                        if (pictureSizes != null) {
                            cameraInfos += "Supported picture sizes:<br/>";

                            cameraInfos += "<pre>";
                            for (Camera.Size size : pictureSizes) {
                                cameraInfos += size.width + " x " + size.height + "</br>";
                            }
                            cameraInfos += "</pre>";
                        }

                        List<Camera.Size> videoSizes = params.getSupportedVideoSizes();
                        if (videoSizes != null){
                            cameraInfos += "Supported video sizes:<br/>";

                            cameraInfos += "<pre>";
                            for (Camera.Size size : videoSizes) {
                                cameraInfos += size.width + " x " + size.height + "</br>";
                            }
                            cameraInfos += "</pre>";
                        }

                        List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
                        if (previewSizes != null){
                            cameraInfos += "Supported preview sizes:<br/>";

                            cameraInfos += "<pre>";
                            for (Camera.Size size : previewSizes) {
                                cameraInfos += size.width + " x " + size.height + "</br>";
                            }
                            cameraInfos += "</pre>";
                        }

                        List<int[]> supportedFpsRanges = params.getSupportedPreviewFpsRange();
                        if (supportedFpsRanges != null) {
                            cameraInfos += "Supported FPS ranges:<br/>";
                            cameraInfos += "<pre>";
                            for (int[] supportedFpsRange : supportedFpsRanges) {
                                cameraInfos += supportedFpsRange[0]+ " - " + supportedFpsRange[1] + "</br>";
                            }
                            cameraInfos += "</pre>";
                        }

                        List<String> flashModes = params.getSupportedFlashModes();
                        if (flashModes != null) {
                            cameraInfos += "Supported flash modes:<br/>";
                            cameraInfos += "<pre>";
                            for (String flashMode : flashModes) {
                                cameraInfos += flashMode + "</br>";
                            }
                            cameraInfos += "</pre>";
                        }

                        List<String> focusModes = params.getSupportedFocusModes();
                        if (focusModes != null) {
                            cameraInfos += "Supported focus modes:<br/>";
                            cameraInfos += "<pre>";
                            for (String focusMode : focusModes) {
                                cameraInfos += focusMode + "</br>";
                            }
                            cameraInfos += "</pre>";
                        }

                        List<Integer> supportedPictureFormats = params.getSupportedPictureFormats();
                        if (supportedPictureFormats != null) {
                            cameraInfos += "Supported picture formats:<br/>";
                            cameraInfos += "<pre>";
                            for (Integer supportedPictureFormat : supportedPictureFormats) {
                                if (supportedPictureFormat == ImageFormat.DEPTH16) {
                                    cameraInfos += "DEPTH16" + "</br>";
                                } else
                                if (supportedPictureFormat == ImageFormat.DEPTH_POINT_CLOUD) {
                                    cameraInfos += "DEPTH_POINT_CLOUD" + "</br>";
                                } else
                                if (supportedPictureFormat == ImageFormat.FLEX_RGB_888) {
                                    cameraInfos += "FLEX_RGB_888" + "</br>";
                                } else
                                if (supportedPictureFormat == ImageFormat.FLEX_RGBA_8888) {
                                    cameraInfos += "FLEX_RGBA_8888" + "</br>";
                                } else
                                if (supportedPictureFormat == ImageFormat.JPEG) {
                                    cameraInfos += "JPEG" + "</br>";
                                } else
                                if (supportedPictureFormat == ImageFormat.NV16) {
                                    cameraInfos += "NV16" + "</br>";
                                } else
                                if (supportedPictureFormat == ImageFormat.NV21) {
                                    cameraInfos += "NV21" + "</br>";
                                } else
                                if (supportedPictureFormat == ImageFormat.PRIVATE) {
                                    cameraInfos += "PRIVATE" + "</br>";
                                } else
                                if (supportedPictureFormat == ImageFormat.RAW10) {
                                    cameraInfos += "RAW10" + "</br>";
                                } else
                                if (supportedPictureFormat == ImageFormat.RAW12) {
                                    cameraInfos += "RAW12" + "</br>";
                                } else
                                if (supportedPictureFormat == ImageFormat.RAW_PRIVATE) {
                                    cameraInfos += "RAW_PRIVATE" + "</br>";
                                } else
                                if (supportedPictureFormat == ImageFormat.RAW_SENSOR) {
                                    cameraInfos += "RAW_SENSOR" + "</br>";
                                } else
                                if (supportedPictureFormat == ImageFormat.RGB_565) {
                                    cameraInfos += "RGB_565" + "</br>";
                                } else
                                if (supportedPictureFormat == ImageFormat.UNKNOWN) {
                                    cameraInfos += "UNKNOWN" + "</br>";
                                } else
                                if (supportedPictureFormat == ImageFormat.YUV_420_888) {
                                    cameraInfos += "YUV_420_888" + "</br>";
                                } else
                                if (supportedPictureFormat == ImageFormat.YUV_422_888) {
                                    cameraInfos += "YUV_422_888" + "</br>";
                                } else
                                if (supportedPictureFormat == ImageFormat.YUV_444_888) {
                                    cameraInfos += "YUV_444_888" + "</br>";
                                } else
                                if (supportedPictureFormat == ImageFormat.YUY2) {
                                    cameraInfos += "YUY2" + "</br>";
                                } else
                                if (supportedPictureFormat == ImageFormat.YV12) {
                                    cameraInfos += "YV12" + "</br>";
                                }
                            }
                            cameraInfos += "</pre>";
                        }

                        List<String> whiteBalances = params.getSupportedWhiteBalance();
                        if (whiteBalances != null) {
                            cameraInfos += "Supported white balance:<br/>";
                            cameraInfos += "<pre>";
                            for (String whiteBalance : whiteBalances) {
                                cameraInfos += whiteBalance + "</br>";
                            }
                            cameraInfos += "</pre>";
                        }

                        camera.release();
                    }
                }
            } else {
                cameraInfos += "<br/>" + "Camera permission not set. Please enable camera permission in App settings.";
            }
        } else {
            cameraInfos += "<br/>" + "Device has no camera";
        }
        return cameraInfos;
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(int cameraId){
        Camera c = null;
        try {
            c = Camera.open(cameraId); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            e.printStackTrace();
        }
        return c; // returns null if camera is unavailable
    }

    /** Check if this device has a camera */
    private static boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
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
            String warn = "<div class=\"alert alert-warning\" role=\"alert\">" +
                    "<strong>No database set!</strong><br /><br />" +
                    "Pass the database name and version to the DebugGhostBridge constructor if you have a SQLite database in your project.<br />" +
                    "If you have no SQLite database in your project, just ignore this message." +
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

    public static String getSharedPreferences(Context context) {
        /*
        /data/data/(package)/shared_prefs/*.xml
         */
        String pkg = context.getPackageName();
        File prefsXmlFolder = new File("/data/data/"+pkg+"/shared_prefs/");
        String[] prefFiles = prefsXmlFolder.list(new PrefsXmlFileFilter());
        String[] prefNames = new String[prefFiles.length];

        if (prefFiles.length == 0) {
            return "<i>no shared preferences found</i>";
        }

        for (int i = 0; i < prefFiles.length; i++) {
            prefNames[i] = prefFiles[i].substring(0, prefFiles[i].lastIndexOf('.'));
        }

        StringBuilder sb = new StringBuilder();

        for (String prefName : prefNames) {
            sb.append("<div class=\"panel panel-success\" style=\"margin-left: 5px; margin-right: 5px;\">");
            sb.append("<div class=\"panel-heading\">");
            sb.append("<h3 class=\"panel-title\">"+prefName+"</h3>");
            sb.append("</div>");
            sb.append("<div class=\"panel-body\" style=\"overflow: auto;\">");

            final SharedPreferences prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
            final Map<String, ?> allEntries = prefs.getAll();

            if (allEntries.size() == 0) {
                sb.append("<i>No shared preferences for the name '"+prefName+"' found</i>");
            } else {
                sb.append("<table class=\"table table-striped\">");
                sb.append("<tbody>");

                for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                    Object typeClass = entry.getValue();
                    String strVal = null;
                    if (typeClass == null) {
                        typeClass = String.class.getSimpleName();
                    } else {
                        strVal = typeClass.toString();
                    }

                    sb.append("<tr><th>");
                    sb.append(entry.getKey());
                    sb.append("</th>");
                    sb.append("<td>");
                    sb.append(typeClass.getClass().getSimpleName());
                    sb.append("</td><td>");
                    sb.append(getControlByType(prefName, entry.getKey(), typeClass, strVal));
                    sb.append("</td>");
                    sb.append("<td>");
                    sb.append(getSaveControl(prefName, entry.getKey()));
                    sb.append("</td></tr>");
                }
                sb.append("<tbody>");
                sb.append("</table>");
            }

            sb.append("</div>");
            sb.append("</div><div style=\"clear: both;\"></div>");
        }

        return sb.toString();
    }

    private static String getSaveControl(String prefsFile, String fieldName) {
        String htmlControl = "<button value=\"save\" type=\"button\" class=\"btn btn-default\" onclick=\"postSharedPrefs('"+prefsFile+"', '"+fieldName+"');\">Save</button>";
        return htmlControl;
    }

    private static String getControlByType(String prefs, String key, Object type, String value) {
        String htmlControl = "";
        String htmlControlVal = "";
        key = prefs + "_debugghostseperator_" + key;
        if (type instanceof Boolean) {
            htmlControlVal = (value.equalsIgnoreCase("true")) ? "checked=\"checked\"" : "";
            htmlControl = "<input id=\""+key+"\" type=\"checkbox\" value=\""+key+"\" "+htmlControlVal+" />";
        } else if (type instanceof Integer) {
            htmlControlVal = value;
            htmlControl = "<input class=\"form-control\" type=\"number\" value=\""+htmlControlVal+"\" id=\""+key+"\">";
        } else if (type instanceof String) {
            htmlControlVal = (value != null) ? value : "null";
            htmlControl = "<input class=\"form-control\" type=\"text\" value=\"" + htmlControlVal + "\" id=\"" + key + "\">";
        } else if (type instanceof Float) {
            htmlControlVal = value;
            htmlControl = "<input class=\"form-control\" type=\"number\" value=\"" + htmlControlVal + "\" id=\"" + key + "\">";
        } else if (type instanceof Long) {
            htmlControlVal = value;
            htmlControl = "<input class=\"form-control\" type=\"number\" value=\"" + htmlControlVal + "\" id=\"" + key + "\">";
        } else if (type instanceof HashSet) {
            StringBuilder sb = new StringBuilder();
            HashSet<String> valueSet = (HashSet) type;
            int rowHeight = Math.min(valueSet.size(), 5);
            sb.append("<textarea class=\"form-control\" id=\"" + key + "\" rows=\""+rowHeight+"\" style=\"margin-top: 0px; margin-bottom: 0px;\">");
            for (String val : valueSet) {
                sb.append(val);
                sb.append("\r\n");
            }
            sb.delete(sb.length()-2, sb.length());
            sb.append("</textarea>");

            htmlControl = sb.toString();
        } else {
            htmlControl = "[DebugGhost has no control configured for type '"+type.getClass().getSimpleName()+"']";
        }
        htmlControl += "<input id=\""+key+"_TYPE\" type=\"hidden\" value=\""+type.getClass().getSimpleName()+"\" />";

        return htmlControl;
    }

    /**
     * From http://stackoverflow.com/questions/13592236/parse-a-uri-string-into-name-value-collection
     */
    public static Map<String, List<String>> splitQuery(String urlQuery) throws UnsupportedEncodingException {
        final Map<String, List<String>> query_pairs = new LinkedHashMap<>();
        final String[] pairs = urlQuery.split("&");
        for (String pair : pairs) {
            final int idx = pair.indexOf("=");
            final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
            if (!query_pairs.containsKey(key)) {
                query_pairs.put(key, new LinkedList<String>());
            }
            final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
            query_pairs.get(key).add(value);
        }
        return query_pairs;
    }

    public static class PrefsXmlFileFilter implements FilenameFilter {
        @Override
        public boolean accept(File directory, String fileName) {
            if (fileName.endsWith(".xml") || fileName.endsWith(".XML")) {
                return true;
            }
            return false;
        }
    }
}
