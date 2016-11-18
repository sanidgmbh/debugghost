package com.sanid.lib.debugghost.utils;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Created by norbertmoehring on 18/11/2016.
 */

public class WebServerUtils {

    private Context mContext;

    public WebServerUtils(Context context) {
        mContext = context;
    }

    public String getHTTPMethod(String input) {
        String method = input.substring(0, input.indexOf(" "));
        return method.trim();
    }

    public String extractPath(String input) {
        String path = input.substring(input.indexOf("/"), input.lastIndexOf(" "));
        return path;
    }

    public String getPage(Context context, String path) {
        if (path.equals("/")) {
            path = "/index";
        }

        String pageContent = getPageExists(path);

        if (pageContent == null) {
            pageContent = "404 NOT FOUND";
        }

        return pageContent;
    }

    private String getPageExists(String page) {

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
}
