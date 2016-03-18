/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/

package com.mrboss.posapp.AsyncServer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.util.Log;
import android.util.Base64;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.text.SimpleDateFormat;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Context;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import android.app.AlertDialog;

import android.database.Cursor;
import android.database.CursorWindow;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;


public class AsyncServer extends CordovaPlugin {

    private static final String LOG_TAG = "AsyncServerPlugin";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if ("SyncDataToAndroid".equals(action)) {
            try {
                testScop(callbackContext);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception occurred: ".concat(e.getMessage()));
                return false;
            }
            return true;
        }
        else if("UploadData".equals(action)){
            String strjson = args.getString(0);
            String webapiurl = args.getString(1);
            String dbname = args.getString(2);
            int deleteDay = args.getInt(3);
            boolean deleteLocalData = args.getBoolean(4);
            UploadData(strjson, webapiurl, dbname, deleteDay, deleteLocalData, callbackContext);
            return true;
        }
        Log.e(LOG_TAG, "Called invalid action: " + action);
        return false;
    }

    public void testScop(CallbackContext callbackContext) throws IOException {
        ConnectivityManager connMgr = (ConnectivityManager)
                                      this.cordova.getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            // fetch data
            try {
                HttpPostTask hpt = new HttpPostTask();
                hpt.callbackContext = callbackContext;
                hpt.execute();

                // Makes sure that the InputStream is closed after the app is
                // finished using it.
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // display error
            Alert("请先连接网络");
        }
    }

    private class HttpPostTask extends AsyncTask {
        public CallbackContext callbackContext;
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected Object doInBackground(Object[] params) {
            try {
                String myurl = "http://192.168.1.31:555/ChainShop3.5_server/aspx/Webapi.aspx";
                String myparams = "Method=SyncDataToAndroid&mac=00:E0:B4:05:2E:83&clientInfoListStr=[]&msg=";
                return new String(HttpHelper.HttpPost(myurl, myparams));
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "Unable to retrieve web page. URL may be invalid.";
        }

        @Override
        protected void onPostExecute(Object o) {
            //Alert(o.toString().length() + "");
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, o.toString()));
            callbackContext.success();
        }
    }

    public void UploadData(String strjson, String webapiurl, String dbname, int deleteDay, boolean deleteLocalData, CallbackContext callbackContext) {
        UploadDataTask udt = new UploadDataTask();
        udt.dbname = dbname;
        udt.deleteDay = deleteDay;
        udt.deleteLocalData = deleteLocalData;
        udt.strjson = strjson;
        udt.webapiurl = webapiurl;
        udt.callbackContext = callbackContext;
        udt.execute();
    }
    public class SQLTable {
        private String TableName;
        private String Sql;
        private String IDColumnName;
        public SQLTable(String tableName, String sql, String iDColumnName) {
            this.TableName = tableName;
            this.Sql = sql;
            this.IDColumnName = iDColumnName;
        }
    }
    public static boolean SyncData = false;
    private class UploadDataTask extends AsyncTask {
        //          public CallbackContext callbackContext;
        // onPostExecute displays the results of the AsyncTask.
        public String strjson = "";
        public String webapiurl = "";
        public String dbname = "";
        public int deleteDay = 10;
        public boolean deleteLocalData = false;
        public CallbackContext callbackContext;
        @Override
        protected Object doInBackground(Object[] params) {
            try {
                if (SyncData) {
                    return "Syncing";
                }
                SyncData = true;
                boolean sendpass = false;
                List<SQLTable> tableList = MakeUpLoadTable(strjson);
                //构建发送服务器的jsonData
                JSONObject json = GetDataSet(tableList, dbname);
                Gson gson = new Gson();
                String jsondate = gson.toJson(json);
                String myparams = "Method=SendDataFromAndroid&jsondate=" + strjson;
                String result = new String(HttpHelper.HttpPost(webapiurl, myparams));
                if (result == "true") {
//                  SetLog(sb, "数据上传成功...");
                    sendpass = true;
                } else {
//                  SetLog(sb, "数据上传失败...");
                    return "Upload Error";
                }

                if (sendpass) {
//                  SetLog(sb, "开始备份数据和删除180天以前的备份数据和日志...");
                    //数据备份及删除180天前的数据备份文件
//                  BackupDataSet(physicalApplicationPath, sendbyte);
//                  SetLog(sb, "备份和删除完成...");

                    //记录上传数据到UploadLog表
//                  SetLog(sb, "开始记录上传数据到UploadLog表...");
                    SetUploadLog(tableList, json, dbname);
//                  SetLog(sb, "记录完成...");

                    if (deleteLocalData) {
//                      SetLog(sb, string.Format("开始删除{0}天前的本地数据...", deleteDay + 30));
                        //删除N天前的数据，这里增加保留日期，是因为来电功能需要查询消费列表
                        DelDataSet(tableList, json, deleteDay + 30, dbname);
//                      SetLog(sb, "删除完成...");
                    }
                }

                // 同步库存
//              SetLog(sb, "开始同步本地库存数据...");
//              SyncPosStore(application, dataSyncService);
//              SetLog(sb, "本地库存数据同步完成...");
                return "OK";
            } catch (IOException e) {
                return e.getMessage();
            } catch (Exception e) {
                return e.getMessage();
            }
        }


        @Override
        protected void onPostExecute(Object o) {
            // Alert(o.toString() + ":" + webapiurl);
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, o.toString()));
            callbackContext.success();
            SyncData = false;
        }
    }

    private SQLiteDatabase GetSQLiteDatabase(String dbname) {
        Context mContext = this.cordova.getActivity();
        File dbfile = mContext.getDatabasePath(dbname);

        if (!dbfile.exists()) {
            dbfile.getParentFile().mkdirs();
        }

        //打开或创建数据库
        return SQLiteDatabase.openOrCreateDatabase(dbfile, null);
    }

    private void SetUploadLog(List<SQLTable> tableList, JSONObject ds, String dbname) {
        SQLiteDatabase db = GetSQLiteDatabase(dbname);
        db.beginTransaction();//开启事物
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timeNow = df.format(new Date());
            db.execSQL("delete from UploadLog where IsUpload=0");

            SQLTable st = null;
            Iterator it = ds.keys();
            while (it.hasNext()) {
                String key = (String) it.next();
                JSONArray dtTmp = ds.getJSONArray(key);
                st = null;
                for (SQLTable tmp : tableList) {
                    if (tmp.TableName == key) {
                        st = tmp;
                    }
                }
                if (st != null) {
                    for (int i = 0; i < dtTmp.length(); i++) {
                        JSONObject row = dtTmp.getJSONObject(i);
                        db.execSQL("insert into UploadLog(TableName,ID,UploadTime,IsUpload) values('" + key + "','" + row.getString(st.IDColumnName) + "','" + timeNow + "'," + 1 + ")");
                    }
                }
            }

            db.setTransactionSuccessful();//事物处理成功
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();//结束事物
        }
    }

    // 多表发送：根据数据字典删除搜索的表格内容
    private void DelDataSet(List<SQLTable> tableList, JSONObject ds, int deleteDay, String dbname) {
        SQLiteDatabase db = GetSQLiteDatabase(dbname);
        db.beginTransaction();//开启事物
        try {

            // 删除N天前已上传的数据
            SQLTable table;

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.add(Calendar.DATE, -deleteDay);
            String dtime = df.format(cal.getTime());

            for (int i = 0; i < tableList.size(); i++) {
                table = tableList.get(i);

                if (table.TableName.equals("SaleMst")) {
                    db.execSQL("delete from SaleMst where HappenDate < '" + dtime + "' and ID in (select ID from UploadLog where IsUpload = 1 and TableName = 'SaleMst')");
                } else if (table.TableName.equals("SaleDtl") || table.TableName.equals("SaleMehtodDtl") || table.TableName.equals("SalePaymentFor911") || table.TableName.equals("SalePaymentForMicropay") || table.TableName.equals("SalePaymentForAlipayTradePay")) {
                    db.execSQL("delete from " + table.TableName + " where PaperNo not in (select PaperNo from SaleMst)");
                } else if (table.TableName.equals("Turnover")) {
                    db.execSQL("delete from Turnover where Date is not null and Date < '" + dtime + "' and Id in (select ID from UploadLog where IsUpload = 1 and TableName = 'Turnover')");
                } else if (table.TableName.equals("BalanceOfPayments")) {
                    db.execSQL("delete from BalanceOfPayments where HappenDate < '" + dtime + "' and Evenid in (select ID from UploadLog where IsUpload = 1 and TableName = 'BalanceOfPayments')");
                } else if (table.TableName.equals("OperationNote")) {
                    db.execSQL("delete from OperationNote where EndDate < '" + dtime + "' and Evenid in (select ID from UploadLog where IsUpload = 1 and TableName = 'OperationNote')");
                } else if (table.TableName.equals("LoadLog")) {
                    db.execSQL("delete from LoadLog where SDateTime < '" + dtime + "' and Id in (select ID from UploadLog where IsUpload = 1 and TableName = 'LoadLog')");
                } else if (table.TableName.equals("MemberCardRecord")) {
                    db.execSQL("delete from MemberCardRecord where HappenDate < '" + dtime + "' and Evenid in (select ID from UploadLog where IsUpload = 1 and TableName = 'MemberCardRecord')");
                }
                db.execSQL("delete from UploadLog where TableName = '" + table.TableName + "' and ID not in (select " + table.IDColumnName + " from " + table.TableName + ")");
            }
            db.setTransactionSuccessful();//事物处理成功
        } finally {
            db.endTransaction();//结束事物
        }
    }

    public JSONObject GetDataSet(List<SQLTable> tableList, String dbname) {
        JSONObject result = new JSONObject();
        SQLiteDatabase db = GetSQLiteDatabase(dbname);
        db.beginTransaction();//开启事物
        try {
            result = GetJsonTableSet(tableList, db);

            db.setTransactionSuccessful();//事物处理成功
        } finally {
            db.endTransaction();//结束事物
        }

        return result;
    }

    private JSONObject GetJsonTableSet(List<SQLTable> tableList, SQLiteDatabase db) {
        JSONObject rowsResult = new JSONObject();
        for (int j = 0; j < tableList.size(); j++) {
            SQLTable st = tableList.get(j);
            Cursor cur = db.rawQuery(st.Sql, new String[] {});
            // If query result has rows
            if (cur != null && cur.moveToFirst()) {
                JSONArray rowsArrayResult = new JSONArray();
                String key = "";
                int colCount = cur.getColumnCount();

                // Build up JSON result object for each row
                do {
                    JSONObject row = new JSONObject();
                    try {
                        for (int i = 0; i < colCount; ++i) {
                            key = cur.getColumnName(i);

                            if (android.os.Build.VERSION.SDK_INT >= 11) {
                                // Use try & catch just in case android.os.Build.VERSION.SDK_INT >= 11 is lying:
                                try {
                                    bindPostHoneycomb(row, key, cur, i);
                                } catch (Exception ex) {
                                    bindPreHoneycomb(row, key, cur, i);
                                }
                            } else {
                                bindPreHoneycomb(row, key, cur, i);
                            }
                        }

                        rowsArrayResult.put(row);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } while (cur.moveToNext());

                try {
                    rowsResult.put(st.TableName, rowsArrayResult);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (cur != null) {
                cur.close();
            }
        }
        return rowsResult;
    }

    private List<SQLTable> MakeUpLoadTable(String strjson) {
        Gson gson = new Gson();
        return gson.fromJson(strjson, new TypeToken<List<SQLTable>>() {}.getType());
    }

    private void bindPostHoneycomb(JSONObject row, String key, Cursor cur, int i) throws JSONException {
        int curType = cur.getType(i);

        switch (curType) {
        case Cursor.FIELD_TYPE_NULL:
            row.put(key, JSONObject.NULL);
            break;
        case Cursor.FIELD_TYPE_INTEGER:
            row.put(key, cur.getLong(i));
            break;
        case Cursor.FIELD_TYPE_FLOAT:
            row.put(key, cur.getDouble(i));
            break;
        case Cursor.FIELD_TYPE_BLOB:
            row.put(key, new String(Base64.encode(cur.getBlob(i), Base64.DEFAULT)));
            break;
        case Cursor.FIELD_TYPE_STRING:
        default: /* (not expected) */
            row.put(key, cur.getString(i));
            break;
        }
    }

    private void bindPreHoneycomb(JSONObject row, String key, Cursor cursor, int i) throws JSONException {
        // Since cursor.getType() is not available pre-honeycomb, this is
        // a workaround so we don't have to bind everything as a string
        // Details here: http://stackoverflow.com/q/11658239
        SQLiteCursor sqLiteCursor = (SQLiteCursor) cursor;
        CursorWindow cursorWindow = sqLiteCursor.getWindow();
        int pos = cursor.getPosition();
        if (cursorWindow.isNull(pos, i)) {
            row.put(key, JSONObject.NULL);
        } else if (cursorWindow.isLong(pos, i)) {
            row.put(key, cursor.getLong(i));
        } else if (cursorWindow.isFloat(pos, i)) {
            row.put(key, cursor.getDouble(i));
        } else if (cursorWindow.isBlob(pos, i)) {
            row.put(key, new String(Base64.encode(cursor.getBlob(i), Base64.DEFAULT)));
        } else { // string
            row.put(key, cursor.getString(i));
        }
    }

    private void Alert(String msg) {
        Dialog alertDialog = new AlertDialog.Builder(this.cordova.getActivity()).
        setTitle("对话框的标题").
        setMessage(msg).
        setCancelable(false).
        setNegativeButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
            }
        }).
        create();
        alertDialog.show();
    }
}