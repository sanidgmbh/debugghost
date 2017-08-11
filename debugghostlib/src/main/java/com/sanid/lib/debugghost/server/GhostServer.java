package com.sanid.lib.debugghost.server;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.sanid.lib.debugghost.DebugGhostService;
import com.sanid.lib.debugghost.commands.Base64GhostCommand;
import com.sanid.lib.debugghost.utils.GhostDBHelper;
import com.sanid.lib.debugghost.utils.GhostUtils;
import com.sanid.lib.debugghost.utils.GhostWebServerUtils;

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
import java.net.URLDecoder;
import java.util.ArrayList;

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
    private final GhostDBHelper mDatabaseHelper;
    private final GhostWebServerUtils mGhostWebServerUtils;
    private OnServerListener mListener;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    private InetAddress mAddr;
    private String mLocalIp;
    private final int mLocalPort;

    private Thread connectionThread = null;
    private final ArrayList<String> mCommands;

    public GhostServer(Context context, int serverPort, String dbName, int dbVersion, ArrayList<String> commands) {
        mContext = context;
        mLocalPort = serverPort;
        mGhostWebServerUtils = new GhostWebServerUtils(mContext);
        mCommands = commands;
        if (dbName != null && dbVersion > 0) {
            mDatabaseHelper = new GhostDBHelper(context, dbName, dbVersion);
        } else {
            mDatabaseHelper = null;
        }
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
            mLocalIp = mGhostWebServerUtils.getLocalIpAddress();
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
                                            if (mGhostWebServerUtils.isBinary(path) || mGhostWebServerUtils.isDbDownload(path)) {
                                                if (sendFile(path, clientSocket.getOutputStream()) == false) {
                                                    mGhostWebServerUtils.send404(out);
                                                }
                                            } else {
                                                handleGet(path, out);
                                            }
                                            break;
                                        case "POST":
                                            handlePost(path, in, out);
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
        int cacheTime = 21600;
        int byteLen = -1;
        if (mGhostWebServerUtils.isDbDownload(path)) {
            is = mGhostWebServerUtils.fileExists(mDatabaseHelper.getPathToDbFile());
            cacheTime = -1;
        } else if (path.contains("project_application_icon")) {
            is = mGhostWebServerUtils.fileAppIcon();
            // TODO get correct content-length
            byteLen = 13100;
        } else {
            is = mGhostWebServerUtils.fileExistsInAssets(path);
            // TODO get correct content-length
            byteLen = 2600;
        }
        if (is != null) {
            try {
                StringBuilder header = new StringBuilder("HTTP/1.1 200 OK\n");
                header.append("Content-Type: " + mGhostWebServerUtils.getContentType(path)+"\n");
    //          header.append("Content-Length: " + byteLen +"\n");
                header.append("\n");

                byte[] contentType = header.toString().getBytes();
                outputStream.write(contentType, 0, contentType.length);

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

    private void handlePost(String path, BufferedReader in, PrintWriter out) {
        // code for reading post data from
        // http://stackoverflow.com/questions/3033755/reading-post-data-from-html-form-sent-to-serversocket
        String postData = "";
        try {
            String line;
            Integer postDataI = 0;
            while ((line = in.readLine()) != null && (line.length() != 0)) {
//                System.out.println("HTTP-HEADER: " + line);
                if (line.indexOf("Content-Length:") > -1) {
                    postDataI = new Integer(
                            line.substring(
                                    line.indexOf("Content-Length:") + 16,
                                    line.length())).intValue();
                }
            }

            // read the post data
            if (postDataI > 0) {
                char[] charArray = new char[postDataI];
                in.read(charArray, 0, postDataI);
                postData = new String(charArray);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String returnPath = null;
        if (path.contains("commands")) {
            String command = path.substring(path.lastIndexOf("/")).replace("/", "");

            if (!command.equals("internal_ghost_sql_command")){
                String postString = postData.replace("data=", "");
                if (postString.contains("returnPath")) {
                    try {
                        returnPath = GhostUtils.splitQuery(postString).get("returnPath").get(0);
                    } catch (Exception e) {
                        Log.e(TAG, "Exception getting returnPath: " + e.getMessage());
                        returnPath = null;
                    }
                }

                Intent commandIntent = new Intent(DebugGhostService.INTENT_FILTER_COMMANDS);
                commandIntent.putExtra(DebugGhostService.INTENT_SERVICE_DEBUG_COMMAND_NAME, command);
                commandIntent.putExtra(DebugGhostService.INTENT_SERVICE_DEBUG_COMMAND_VALUE, postString);

                mContext.sendBroadcast(commandIntent);
            } else {
                // Ghost Command SQL
                String postString = postData.replace("data=", "");
                postString = URLDecoder.decode(postString);
                returnPath = "sql/" + Base64GhostCommand.encodeBase64(postString);
                returnPath = returnPath.replaceAll("\n", "");
            }
        }

        if (returnPath != null) {
            mGhostWebServerUtils.send301(out, "/"+returnPath);
        } else {
            mGhostWebServerUtils.send301(out, "/commands");
        }

    }

    private void handleGet(String path, PrintWriter out) {
        String page = mGhostWebServerUtils.getPage(mContext, path);
//        String page = mGhostWebServerUtils.getPage(mContext, "/");

        if (page != null) {

            switch (path) {
                case "commands":
                case "/commands":
                    page = handleCommand(page, path);
                    break;
                case "device":
                case "/device":
                    page = handleDevice(page, path);
                    break;
                case "prefs":
                case "/prefs":
                    page = handlePrefs(page, path);
                    break;
                default:
                    page = handleIndex(page, path);
                break;
            }

            mGhostWebServerUtils.writeDefaultHeader(HttpURLConnection.HTTP_OK, out, 30);
            out.println(page);
            out.println();
            out.flush();
        } else {
            mGhostWebServerUtils.send404(out);
        }
    }

    private String handleCommand(String page, String path) {

        page = page.replace("{{PROJECT_NAME}}", GhostUtils.getApplicationName(mContext));
//        page = page.replace("{{ALERT_VISIBLE}}", GhostUtils.getNoCommandsAlert(mCommands));
        page = page.replace("{{COMMAND_LIST}}", GhostUtils.getCommandList(mCommands));
        page = page.replace("{{COMMAND_LIST_INPUT}}", GhostUtils.getCommandInputList(mCommands));

        return page;
    }

    private String handlePrefs(String page, String path) {

        page = page.replace("{{PROJECT_NAME}}", GhostUtils.getApplicationName(mContext));
        page = page.replace("{{SHARED_PREFS_LIST}}", GhostUtils.getSharedPreferences(mContext));

        return page;
    }

    private String handleDevice(String page, String path) {

        page = page.replace("{{PROJECT_NAME}}", GhostUtils.getApplicationName(mContext));
        page = page.replace("{{DEVICE_INFOS}}", GhostUtils.getDeviceInfos(mContext));
        page = page.replace("{{SCREEN_INFOS}}", GhostUtils.getScreenInfos(mContext));
        page = page.replace("{{HW_INFOS}}", GhostUtils.getHardwareInfos(mContext));

        return page;
    }

    private String handleIndex(String page, String path) {
        String selectedTableName = "<i>nothing selected</i>";
        String selectedTable = "";

        page = page.replace("{{PROJECT_NAME}}", GhostUtils.getApplicationName(mContext));
        page = page.replace("{{DATABASE_VISIBLE}}", (mDatabaseHelper != null) ? "visible" : "hidden");
        page = page.replace("{{ALERT_VISIBLE}}", GhostUtils.getNoDatabaseAlert(mDatabaseHelper));

        if (mDatabaseHelper != null) {
            page = page.replace("{{DATABASE_NAME}}", mDatabaseHelper.getDbName());

            if (path.contains("db")) {
                selectedTableName = path.substring(path.lastIndexOf("/") + 1, path.length());
                page = page.replace("{{QUERY_STATEMENT}}", "SELECT * from " + selectedTableName);
                selectedTable = mDatabaseHelper.getHTMLTable(selectedTableName);
            } else if (path.contains("sql")){
                String sqlQueryBase64 = path.substring(path.lastIndexOf("/") + 1, path.length());
                String sqlStatement = Base64GhostCommand.decodeBase64(sqlQueryBase64);
                page = page.replace("{{QUERY_STATEMENT}}", sqlStatement);

                if (isSelectStatement(sqlStatement)) {
                    selectedTableName = sqlStatement;
                    selectedTable = mDatabaseHelper.getHTMLTableFromQuery(sqlStatement);
                } else {
                    String[] splitStatement = sqlStatement.split(" ");
                    if (isUpdateStatement(sqlStatement)) {
                        if (splitStatement.length >= 2) {
                            selectedTableName = splitStatement[1];
                        }
                    } else if (isInsertStatement(sqlStatement)){
                        if (splitStatement.length >= 3) {
                            selectedTableName = splitStatement[2];
                        }
                    } else if (isDeleteStatement(sqlStatement)){
                        if (splitStatement.length >= 3) {
                            selectedTableName = splitStatement[2];
                        }
                    } else {
                        if (splitStatement.length >= 3) {
                            selectedTableName = splitStatement[2];
                        }
                    }
                    selectedTableName = selectedTableName.replaceAll(";", "");
                    String[] splitMultipleStatements = sqlStatement.split(";");
                    if (splitMultipleStatements != null){
                        for (String splitPart: splitMultipleStatements){
                            splitPart = splitPart.replaceAll(";", "");
                            if (splitPart.length() > 8) {
                                selectedTable = mDatabaseHelper.getHTMLTableFromQuery(splitPart);
                            }
                        }
                    } else {
                        selectedTable = mDatabaseHelper.getHTMLTableFromQuery(sqlStatement);
                    }
                    if (!selectedTableName.equals("<i>nothing selected</i>") && !selectedTable.startsWith("<div class=\"alert alert-danger\" role=\"alert\">") && !isDropTableStatement(sqlStatement)){
                        selectedTable = mDatabaseHelper.getHTMLTableFromQuery("SELECT * from " + selectedTableName);
                    }
                }
            }
        }

        String htmlTables = mDatabaseHelper.getHTMLTables();
        page = page.replace("{{TABLE_LIST}}", htmlTables);

        page = page.replace("{{SELECTED_TABLE_NAME}}", selectedTableName);
        page = page.replace("{{SELECTED_TABLE}}", selectedTable);

        if (selectedTableName.equals("<i>nothing selected</i>")){
            page = page.replace("{{QUERY_STATEMENT}}", "");
        }

        return page;
    }

    private boolean isSelectStatement(String statement){
        String trimmedStatement = statement.trim();
        if (trimmedStatement.regionMatches(true, 0, "select", 0, 6)){
            return true;
        }

        return false;
    }

    private boolean isUpdateStatement(String statement){
        String trimmedStatement = statement.trim();
        if (trimmedStatement.regionMatches(true, 0, "update", 0, 6)){
            return true;
        }

        return false;
    }

    private boolean isInsertStatement(String statement){
        String trimmedStatement = statement.trim();
        if (trimmedStatement.regionMatches(true, 0, "insert into", 0, 11)){
            return true;
        }

        return false;
    }

    private boolean isDeleteStatement(String statement){
        String trimmedStatement = statement.trim();
        if (trimmedStatement.regionMatches(true, 0, "delete from", 0, 11)){
            return true;
        }

        return false;
    }

    private boolean isDropTableStatement(String statement){
        String trimmedStatement = statement.trim();
        if (trimmedStatement.regionMatches(true, 0, "drop table", 0, 10)){
            return true;
        }

        return false;
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
