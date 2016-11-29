package com.sanid.lib.debugghost.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.sanid.lib.debugghost.BuildConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Created by norbertmoehring on 18/11/2016.
 */

public class WebServerUtils {

    public static final String TAG = "WebServerUtils";

    private final Context mContext;
    private String mVersion;

    public WebServerUtils(Context context) {
        mContext = context;
        mVersion = BuildConfig.VERSION_NAME + "["+BuildConfig.VERSION_CODE+"]";
    }

    public String getPage(Context context, String path) {
        if (path.equals("/")) {
            path = "/index";
        }
        String pageContent = getPageExists(path);

        return pageContent;
    }

    public boolean isDbDownload(String path) {
        return (path.contains("db/download"));
    }

    public boolean isBinary(String path) {
        String ext = getFileExt(path);
        if (ext != null) {
            ext = ext.toLowerCase();
            switch (ext) {
                case "html":
                case "txt":
                case "./":
                case "/":
                case "":
                    return false;
                case "ico":
                case "jpg":
                case "jpeg":
                case "png":
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }

    public String getContentType(String path) {
        String ext = getFileExt(path);
        String contentType = "text/plain";
        if (ext != null) {
            ext = ext.toLowerCase();
            switch (ext) {
                case "./":
                case "/":
                case "":
                case "htm":
                case "html":
                case "shtml":
                    contentType = "text/html";
                    break;
                case "txt":
                    contentType = "text/plain";
                    break;
                case "ico":
                    contentType = "image/x-icon";
                    break;
                case "jpe":
                case "jpg":
                case "jpeg":
                    contentType = "image/jpeg";
                    break;
                case "png":
                    contentType = "image/png";
                    break;
            }
        }
        return contentType;
    }

    public String getFileExt(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
    }

    public InputStream fileExists(String path) {
        File file = new File(path);
        if (file.exists()) {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public InputStream fileExistsInAssets(String path) {
        AssetManager mg = mContext.getResources().getAssets();

        if (path.startsWith("/")) {
            path = path.substring(1);
        } else if (path.startsWith("./")) {
            path = path.substring(2);
        }

        try {
            return mg.open(path);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public String getPageExists(String page) {

        try {
            page = page.replaceFirst("/", "");
            if (page.contains(".") == false) {
                page = page+".html";
            }
            InputStreamReader isr = new InputStreamReader(mContext.getAssets().open(page));
            BufferedReader streamReader = new BufferedReader(isr);
            StringBuilder strBuilder = new StringBuilder();

            String inputStr;
            while ((inputStr = streamReader.readLine()) != null){
                strBuilder.append(inputStr);
            }

            return strBuilder.toString();
        } catch (IOException e) {
            Log.d("SERVER", "asset loading failed: " + e.getMessage());
            return null;
        }
    }

    public String getLocalIpAddress() throws Exception {
        String resultIpv6 = "";
        String resultIpv4 = "";

        for (Enumeration en = NetworkInterface.getNetworkInterfaces();
             en.hasMoreElements();) {

            NetworkInterface intf = (NetworkInterface) en.nextElement();
            for (Enumeration enumIpAddr = intf.getInetAddresses();
                 enumIpAddr.hasMoreElements();) {

                InetAddress inetAddress = (InetAddress) enumIpAddr.nextElement();
                if(!inetAddress.isLoopbackAddress()){
                    if (inetAddress instanceof Inet4Address) {
                        resultIpv4 = inetAddress.getHostAddress().toString();
                    } else if (inetAddress instanceof Inet6Address) {
                        resultIpv6 = inetAddress.getHostAddress().toString();
                    }
                }
            }
        }
        return ((resultIpv4.length() > 0) ? resultIpv4 : resultIpv6);
    }

    public void send404(PrintWriter out) {
        String page = getPageExists("/404.html");

        writeDefaultHeader(HttpURLConnection.HTTP_NOT_FOUND, out);
        out.println(page);
        out.println();
        out.flush();
    }

    public void writeDefaultHeader(int httpStatus, PrintWriter out) {
        out.println("HTTP/1.0 "+httpStatus);
        out.println("Date: ");
        out.println("Server: DebugGhost/"+mVersion+" (Android)");
        out.println("X-Powered-By: SANID (www.sanid.com)");
        out.println("Cache-Control: no-cache");
        out.println("Connection: close");
        out.println();
    }
}
