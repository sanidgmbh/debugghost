package com.sanid.lib.debugghost.server;

import android.content.Context;
import android.util.Log;

import com.sanid.lib.debugghost.utils.GhostDatabaseHelper;
import com.sanid.lib.debugghost.utils.WebServerUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by norbertmoehring on 17/11/2016.
 */

public class GhostServerThread extends Thread {

    public static final String TAG = "GhostServer";

    public static final int STATUS_STOP = 0;
    public static final int STATUS_RUN = 1;
    private int mStatus = STATUS_RUN;

    private final Context mContext;
    private final GhostDatabaseHelper mDatabaseHelper;
    private final WebServerUtils mWebServerUtils;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public GhostServerThread(Context context, String dbName, int dbVersion) {
        mContext = context;
        mWebServerUtils = new WebServerUtils(mContext);
        mDatabaseHelper = new GhostDatabaseHelper(context, dbName, dbVersion);
    }

    public void run() {

        try {
            // Open a server socket listening on port 8080
            String localIp = mWebServerUtils.getLocalIpAddress();
            InetAddress addr = InetAddress.getByName(localIp);
            serverSocket = new ServerSocket(8080, 0, addr);
            Log.d(TAG, "IP: " + localIp);

            while (mStatus == STATUS_RUN) {
                Log.d(TAG, "waiting on client ... ");
                clientSocket = serverSocket.accept();

                Log.d(TAG, "receiving data ... ");

                // Client established connection.
                // Create input and output streams
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                // Read received data and echo it back
                String input = in.readLine();

                if (input == null) {
                    out.println("404");
                } else {
                    String path = mWebServerUtils.extractPath(input);
                    if (mWebServerUtils.getHTTPMethod(input).equals("GET")) {
                        String page = mWebServerUtils.getPage(mContext, "/");
                        //String page = mWebServerUtils.getPage(mContext, path);

                        if (page != null) {
                            String selectedTableName = "<i>nothing selected</i>";
                            String selectedTable = "";

                            String htmlTables = mDatabaseHelper.getHTMLTables();
                            page = page.replace("{{TABLE_LIST}}", htmlTables);

                            if (path.contains("db")) {
                                selectedTableName = path.substring(path.lastIndexOf("/")+1, path.length());
                                selectedTable = mDatabaseHelper.getHTMLTable(selectedTableName);
                            }

                            page = page.replace("{{SELECTED_TABLE_NAME}}", selectedTableName);
                            page = page.replace("{{SELECTED_TABLE}}", selectedTable);
                        }

                        out.println(page);
                    }
                    Log.d(TAG, "received: " + input);
                    //out.println("received: " + input);
                }

                // Perform cleanup
                in.close();
                out.close();
            }

        } catch(Exception e) {
            // Omitting exception handling for clarity
            e.printStackTrace();
        }
    }
}
