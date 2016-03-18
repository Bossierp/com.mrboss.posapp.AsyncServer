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

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Intent;
import android.util.Log;

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

import java.io.IOException;
import java.io.InputStream;

import android.app.AlertDialog;

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
        Log.e(LOG_TAG, "Called invalid action: "+action);
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

    private void Alert(String msg){
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