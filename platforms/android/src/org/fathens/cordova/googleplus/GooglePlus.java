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

package org.fathens.cordova.googleplus;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONException;

import android.content.IntentSender.SendIntentException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.plus.PlusClient;

public class GooglePlus extends CordovaPlugin {
    private static final int REQUEST_CODE_RESOLVE_ERR = 9000;
    private static final String TAG = "GooglePlusPlugin";
    public static final String ACTION_LOGIN = "login";

    private PlusClient mPlusClient;

    @Override
    public void initialize(final CordovaInterface cordova, final CordovaWebView webView) {
	super.initialize(cordova, webView);
	Log.d(TAG, "Initialized");
    }

    @Override
    public boolean execute(final String action, final CordovaArgs args, final CallbackContext callback)
	    throws JSONException {
	if (action.equals(ACTION_LOGIN)) {
	    final String[] scopes = new String[] { Scopes.PLUS_LOGIN, "https://www.googleapis.com/auth/userinfo.email" };
	    mPlusClient = new PlusClient.Builder(cordova.getActivity(), new ConnectionCallbacks() {
		@Override
		public void onConnected(final Bundle bundle) {
		    final AsyncTask<Object, Object, Object> task = new AsyncTask<Object, Object, Object>() {
			@Override
			protected Object doInBackground(Object... params) {
			    try {
				final String accountName = mPlusClient.getAccountName();
				Log.d(TAG, "Obtaining token by user: " + accountName);
				final String token = GoogleAuthUtil.getToken(cordova.getActivity()
					.getApplicationContext(), accountName, "oauth2:" + TextUtils.join(" ", scopes));
				Log.d(TAG, "Connected(" + accountName + "): " + token);
				callback.success(token);
			    } catch (Exception e) {
				e.printStackTrace();
				callback.error(e.getLocalizedMessage());
			    }
			    return null;
			}
		    };
		    task.execute((Void) null);
		}

		@Override
		public void onDisconnected() {
		    Log.d(TAG, "Disconnected");
		    callback.error("Disconnected");
		}
	    }, new OnConnectionFailedListener() {
		@Override
		public void onConnectionFailed(ConnectionResult result) {
		    if (result.hasResolution()) {
			try {
			    result.startResolutionForResult(cordova.getActivity(), REQUEST_CODE_RESOLVE_ERR);
			} catch (SendIntentException e) {
			    mPlusClient.connect();
			}
		    } else {
			callback.error(result.toString());
		    }
		}
	    }).setScopes(scopes).build();
	    mPlusClient.connect();
	    return true;
	} else {
	    return false;
	}
    }
}
