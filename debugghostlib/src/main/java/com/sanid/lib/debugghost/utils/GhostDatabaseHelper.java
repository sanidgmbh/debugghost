package com.sanid.lib.debugghost.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

public class GhostDatabaseHelper extends SQLiteOpenHelper {

    public static final String QUERY_ALL_TABLES = "SELECT name FROM sqlite_master WHERE type='table';";
    public static final String QUERY_TABLE = "SELECT * FROM ##TABLE##";

    private final Context mContext;
    private final String mDbName;

    public GhostDatabaseHelper(Context context, String dbName, int dbVersion) {
        super(context, dbName, null, dbVersion);

        mDbName = dbName;
        mContext = context;
    }

    // Method is called during creation of the database
    @Override
    public void onCreate(SQLiteDatabase database) {
    }


    // Method is called during an upgrade of the database,
    // if you increase the database version
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {

    }

    private Cursor sqlQuery(String query) {
        SQLiteDatabase dbRead = getReadableDatabase();

        Cursor c = dbRead.rawQuery(query, null);
        return c;
    }

    public String getHTMLTables() {
        Cursor c = sqlQuery(QUERY_ALL_TABLES);
        StringBuilder sb = new StringBuilder();

        sb.append("<ul>");
        while (c.moveToNext()) {
            String tName = stringFromCursor(c, "name");
            sb.append("<li>");
            sb.append("<a href=\"/db/"+tName+"\">");
            sb.append(tName);
            sb.append("</a>");
            sb.append("</li>");
        }
        sb.append("</ul>");
        c.close();

        return sb.toString();
    }

    public String getHTMLTable(String tableName) {
        StringBuilder sb = new StringBuilder();
        String out;
        Cursor c = null;
        try {
            c = sqlQuery(QUERY_TABLE.replace("##TABLE##", tableName));

            sb.append("<table>");
            sb.append("<tr>");
            for (int i = 0; i < c.getColumnCount(); i++) {
                sb.append("<th>");
                sb.append(c.getColumnName(i));
                sb.append("</th>");
            }
            sb.append("</tr>");

            while (c.moveToNext()) {
                sb.append("<tr>");
                for (int i = 0; i < c.getColumnCount(); i++) {
                    sb.append("<td>");
                    sb.append(stringFromCursor(c, c.getColumnName(i)));
                    sb.append("</td>");
                }
                sb.append("</tr>");
            }
            sb.append("</table>");

            out = sb.toString();
        } catch (SQLiteException e) {
            out = "Could not read table '"+tableName + "'. Exception: " + e.getMessage();
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return out;
    }

    public static String stringFromCursor(Cursor c, String field) {
        if (c.isNull(c.getColumnIndex(field))) {
            return null;
        }
        return c.getString(c.getColumnIndex(field));
    }

    public static long longFromCursor(Cursor c, String field) {
        if (c.isNull(c.getColumnIndex(field))) {
            return -1l;
        }
        return c.getLong(c.getColumnIndex(field));
    }

    public static int intFromCursor(Cursor c, String field) {
        if (c.isNull(c.getColumnIndex(field))) {
            return -1;
        }
        return c.getInt(c.getColumnIndex(field));
    }

    public static double doubleFromCursor(Cursor c, String field) {
        if (c.isNull(c.getColumnIndex(field))) {
            return -1;
        }
        return c.getDouble(c.getColumnIndex(field));
    }

    public static float floatFromCursor(Cursor c, String field) {
        if (c.isNull(c.getColumnIndex(field))) {
            return -1;
        }
        return c.getFloat(c.getColumnIndex(field));
    }

    public static boolean boolFromCursor(Cursor c, String field) {
        if (c.isNull(c.getColumnIndex(field))) {
            return false;
        }
        int val = c.getInt(c.getColumnIndex(field));
        return (val > 0) ? true : false;
    }

    public String getDbName() {
        return mDbName;
    }

    public String getPathToDbFile() {
        return mContext.getDatabasePath(mDbName).getPath();
    }
}
