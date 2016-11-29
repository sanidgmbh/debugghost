package com.sanid.lib.debugghost.server;

import android.content.Context;
import android.util.Log;

import com.sanid.lib.debugghost.utils.GhostDatabaseHelper;
import com.sanid.lib.debugghost.utils.WebServerUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by norbertmoehring on 17/11/2016.
 */

public class GhostServer {

    public interface OnServerListener {
        void onServerStarted(String ip, int port);
        void onServerStopped();
    }

    public static final String TAG = "GhostServer";

    public static final int STATUS_STOP = 0;
    public static final int STATUS_RUN = 1;
    private int mStatus = STATUS_RUN;

    private final Context mContext;
    private final GhostDatabaseHelper mDatabaseHelper;
    private final WebServerUtils mWebServerUtils;
    private OnServerListener mListener;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    private InetAddress mAddr;
    private String mLocalIp;
    private final int mLocalPort;

    private Thread connectionThread = null;

    public GhostServer(Context context, int serverPort, String dbName, int dbVersion) {
        mContext = context;
        mLocalPort = serverPort;
        mWebServerUtils = new WebServerUtils(mContext);
        mDatabaseHelper = new GhostDatabaseHelper(context, dbName, dbVersion);
    }

    public GhostServer(Context context, String dbName, int dbVersion) {
        this(context, 8080, dbName, dbVersion);
    }

    public void setOnServerListener(OnServerListener listener) {
        mListener = listener;
    }

    public int getStatus() {
        return mStatus;
    }

    public void stop() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket client = new Socket(mLocalIp, mLocalPort);
                    PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                    out.print(".exit");
                    out.close();
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void start() {
        mStatus = STATUS_RUN;

        init();
        runIt();
    }

    private void init() {
        try {
            mLocalIp = mWebServerUtils.getLocalIpAddress();
            mAddr = InetAddress.getByName(mLocalIp);
        } catch (Exception e) {
            Log.e(TAG, "Could not obtain local address: " + e.getMessage());
            mStatus = STATUS_STOP;
        }
    }

    private void runIt() {
        if (connectionThread != null && connectionThread.isInterrupted() == false) {
            connectionThread.interrupt();
            connectionThread = null;
        }

        connectionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(mLocalPort, 0, mAddr);
                    Log.d(TAG, "IP: " + mLocalIp + " | port: " + mLocalPort);

                    while (mStatus == STATUS_RUN) {
                        if (mListener != null) {
                            mListener.onServerStarted(mLocalIp, mLocalPort);
                        }
                        Log.d(TAG, "waiting on client data ... ");
                        clientSocket = serverSocket.accept();

                        Log.d(TAG, "receiving data ... ");
                        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        out = new PrintWriter(clientSocket.getOutputStream(), false);

                        String input = in.readLine();
                        Log.d(TAG, "input = " + input);

                        if (input == null) {
                            // drop it
                        } else {
                            if (isHandleCommand(input)) {
                                //
                            } else {
                                String[] inData = canHandleData(input);
                                if (inData != null) {
                                    final String method = inData[0];
                                    final String path = inData[1];
                                    final String protocol = inData[2];

                                    switch (method) {
                                        case "GET":
                                            if (mWebServerUtils.isBinary(path) || mWebServerUtils.isDbDownload(path)) {
                                                if (sendFile(path, clientSocket.getOutputStream()) == false) {
                                                    mWebServerUtils.send404(out);
                                                }
                                            } else {
                                                handleGet(path, out);
                                            }
                                            break;
                                        case "POST":
                                            handlePost(path, out);
                                            break;
                                        default:
                                            break;
                                    }

                                } else {
                                    // TODO return 500
                                    out.println("500");
                                    out.println("");
                                    out.flush();
                                }
                            }
                        }
                        in.close();
                        out.close();
                    }

                    serverSocket.close();

                    if (mListener != null) {
                        mListener.onServerStopped();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        connectionThread.start();
    }

    private boolean sendFile(String path, OutputStream outputStream) {
        boolean fileFound = false;
        InputStream is;
        if (mWebServerUtils.isDbDownload(path)) {
            is = mWebServerUtils.fileExists(mDatabaseHelper.getPathToDbFile());
        } else {
            is = mWebServerUtils.fileExistsInAssets(path);
        }
        if (is != null) {
            try {
                PrintWriter out = new PrintWriter(outputStream);
                out.print("ContentType: " + mWebServerUtils.getContentType(path));
                mWebServerUtils.writeDefaultHeader(HttpURLConnection.HTTP_OK, out);

                byte[] buffer = new byte[1024];
                int len = is.read(buffer);
                while (len != -1) {
                    outputStream.write(buffer, 0, len);
                    len = is.read(buffer);
                }
                fileFound = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return fileFound;
    }

    private void handleGet(String path, PrintWriter out) {
        String page = mWebServerUtils.getPage(mContext, "/");

        if (page != null) {
            String selectedTableName = "<i>nothing selected</i>";
            String selectedTable = "";

            String htmlTables = mDatabaseHelper.getHTMLTables();
            page = page.replace("{{TABLE_LIST}}", htmlTables);

            if (path.contains("db")) {
                selectedTableName = path.substring(path.lastIndexOf("/") + 1, path.length());
                selectedTable = mDatabaseHelper.getHTMLTable(selectedTableName);
            }

            page = page.replace("{{SELECTED_TABLE_NAME}}", selectedTableName);
            page = page.replace("{{SELECTED_TABLE}}", selectedTable);
            page = page.replace("{{DATABASE_NAME}}", mDatabaseHelper.getDbName());

            mWebServerUtils.writeDefaultHeader(HttpURLConnection.HTTP_OK, out);
            out.println(page);
            out.println();
            out.flush();
        } else {
            mWebServerUtils.send404(out);
        }
    }

    private void handlePost(String path, PrintWriter out) {
        // TODO
    }

    private String[] canHandleData(String input) {
        String[] inData = null;
        try {
            inData = input.split(" ");
        } catch (Exception e) {
            Log.w(TAG, "Data input can not be handled!");
        }

        return inData;
    }

    private boolean isHandleCommand(String input) {
        if (".exit".equals(input)) {
            mStatus = STATUS_STOP;
            return true;
        }
        return false;
    }
}
