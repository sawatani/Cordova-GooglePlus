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

import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;

public class GooglePlus extends CordovaPlugin {
    private static final int REQUEST_CODE_RESOLVE_ERR = 9000;
    private static final String TAG = "GooglePlusPlugin";
    public static final String ACTION_LOGIN = "login";

    private GoogleApiClient apiClient;

    @Override
    public void initialize(final CordovaInterface cordova, final CordovaWebView webView) {
	super.initialize(cordova, webView);
	cordova.setActivityResultCallback(this);
	Log.d(TAG, "Initialized");
    }

    @Override
    public boolean execute(final String action, final CordovaArgs args, final CallbackContext callback)
	    throws JSONException {
	if (action.equals(ACTION_LOGIN)) {
	    loginToken(callback);
	    return true;
	} else {
	    return false;
	}
    }

    private void loginToken(final CallbackContext callback) {
	final String[] scopeUrls = new String[] { Scopes.PLUS_LOGIN, "https://www.googleapis.com/auth/userinfo.email" };
	final Scope[] scopes = new Scope[scopeUrls.length];
	for (int i = 0; i < scopeUrls.length; i++) {
	    scopes[i] = new Scope(scopeUrls[i]);
	}
	final String scoping = String.format("oauth2:%s", TextUtils.join(" ", scopeUrls));

	final GoogleApiClient.ConnectionCallbacks connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {

	    @Override
	    public void onConnected(Bundle bundle) {
		final String accountName = Plus.AccountApi.getAccountName(apiClient);
		Log.d(TAG, "Obtaining token by user(" + accountName + "): " + scoping);
		cordova.getThreadPool().execute(new Runnable() {
		    @Override
		    public void run() {
			try {
			    final String token = GoogleAuthUtil.getToken(cordova.getActivity(), accountName, scoping);
			    Log.d(TAG, "Connected(" + accountName + "): " + token);
			    callback.success(token);
			} catch (Exception e) {
			    e.printStackTrace();
			    callback.error(e.getLocalizedMessage());
			}
		    }
		});
	    }

	    @Override
	    public void onConnectionSuspended(int state) {
		Log.d(TAG, "onConnectionSuspended: " + state);
		apiClient.connect();
	    }
	};
	final GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {

	    @Override
	    public void onConnectionFailed(ConnectionResult result) {
		Log.e(TAG, "onConnectionFailed:" + result);
		if (result.hasResolution()) {
		    Log.i(TAG, "startResolutionForResult: " + REQUEST_CODE_RESOLVE_ERR);
		    try {
			result.startResolutionForResult(cordova.getActivity(), REQUEST_CODE_RESOLVE_ERR);
		    } catch (SendIntentException e) {
			apiClient.connect();
		    }
		} else {
		    callback.error(result.toString());
		}
	    }
	};

	final GoogleApiClient.Builder builder = new GoogleApiClient.Builder(cordova.getActivity())
		.addConnectionCallbacks(connectionCallbacks).addOnConnectionFailedListener(onConnectionFailedListener)
		.addApi(Plus.API, Plus.PlusOptions.builder().build());
	for (Scope scope : scopes) {
	    builder.addScope(scope);
	}
	apiClient = builder.build();
	apiClient.connect();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
	Log.i(TAG, "onActivityResult:" + requestCode);
	if (requestCode == REQUEST_CODE_RESOLVE_ERR) {
	    if (!apiClient.isConnecting()) {
		Log.w(TAG, "Not Connecting... Try Connect.");
		apiClient.connect();
	    }
	}
    }
}
