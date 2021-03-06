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

import java.io.IOException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.PlusClient;
import com.google.android.gms.plus.model.people.Person;

public class GooglePlusConnect extends CordovaPlugin {
    private static final String TAG = "GooglePlusPlugin";

    public static final int REQUEST_PICK_ACCOUNT = 9000;
    public static final int REQUEST_AUTH_RECOVER = 9001;
    public static final int REQUEST_PCLIENT_RECOVER = 9002;
    public static final int REQUEST_GCLIENT_RECOVER = 9003;

    public static final String ACTION_LOGIN = "login";
    public static final String ACTION_DISCONNECT = "disconnect";
    public static final String ACTION_PROFILE = "profile";

    private static final String[] scopeUrls = new String[] { Scopes.PLUS_LOGIN,
	    "https://www.googleapis.com/auth/userinfo.email" };

    private CallbackContext currentCallback;
    private PlusClient pClient;
    private GoogleApiClient gClient;
    private String accountName;
    private String[] actions = new String[] { "https://schemas.google.com/AddActivity" };

    @Override
    public void initialize(final CordovaInterface cordova, final CordovaWebView webView) {
	super.initialize(cordova, webView);
	Log.d(TAG, "Initialized");
    }

    @Override
    public boolean execute(final String action, final CordovaArgs args, final CallbackContext callback)
	    throws JSONException {
	currentCallback = callback;
	accountName = args.optString(0);
	if (action.equals(ACTION_LOGIN)) {
	    login();
	    return true;
	} else if (action.equals(ACTION_DISCONNECT)) {
	    disconnect();
	    return true;
	} else if (action.equals(ACTION_PROFILE)) {
	    profile();
	    return true;
	} else {
	    return false;
	}
    }

    private void login() {
	if (accountName != null && accountName.length() > 0) {
	    obtainToken();
	} else {
	    final Intent picker = AccountPicker.newChooseAccountIntent(null, null,
		    new String[] { GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE }, true, null, null, null, null);
	    cordova.startActivityForResult(this, picker, REQUEST_PICK_ACCOUNT);
	}
    }

    private void obtainToken() {
	final String scoping = String.format("oauth2:%s", TextUtils.join(" ", scopeUrls));
	Log.d(TAG, "Obtaining token by user(" + accountName + "): " + scoping);
	cordova.getThreadPool().execute(new Runnable() {
	    @Override
	    public void run() {
		try {
		    Log.d(TAG, "First try to get token");
		    final String waste = GoogleAuthUtil.getToken(cordova.getActivity(), accountName, scoping);
		    // TODO Check token if valid
		    Log.d(TAG, "Clearing the token: " + waste);
		    GoogleAuthUtil.clearToken(cordova.getActivity(), waste);
		    final Bundle bundle = new Bundle();
		    if (actions != null && actions.length > 0) {
			final String actionString = TextUtils.join(" ", actions);
			bundle.putString(GoogleAuthUtil.KEY_REQUEST_VISIBLE_ACTIVITIES, actionString);
		    }
		    Log.d(TAG, "SignIn with " + bundle);
		    Log.d(TAG, "Second try to get token");
		    final String token = GoogleAuthUtil.getToken(cordova.getActivity(), accountName, scoping, bundle);
		    Log.d(TAG, "Connected(" + accountName + "): " + token);
		    final JSONObject result = new JSONObject().put("accountName", accountName)
			    .put("accessToken", token);
		    Log.d(TAG, "Callbacking result: " + result);
		    currentCallback.success(result);
		} catch (UserRecoverableAuthException ex) {
		    Log.e(TAG, "Recovering authorization", ex);
		    cordova.startActivityForResult(GooglePlusConnect.this, ex.getIntent(), REQUEST_AUTH_RECOVER);
		} catch (IOException ex) {
		    ex.printStackTrace();
		    currentCallback.error(ex.getLocalizedMessage());
		} catch (GoogleAuthException ex) {
		    ex.printStackTrace();
		    if ("BadUsername".equals(ex.getMessage())) {
			Log.e(TAG, "Invoked with BadUsername(" + accountName + "). re-select account...", ex);
			accountName = null;
			login();
		    } else {
			currentCallback.error(ex.getLocalizedMessage());
		    }
		} catch (JSONException ex) {
		    ex.printStackTrace();
		    currentCallback.error(ex.getLocalizedMessage());
		}
	    }
	});
    }

    private void profile() {
	Log.d(TAG, "Getting profile of " + accountName);
	cordova.getThreadPool().execute(new Runnable() {
	    @Override
	    public void run() {
		gClient = new GoogleApiClient.Builder(cordova.getActivity()).addApi(Plus.API)
			.addScope(Plus.SCOPE_PLUS_LOGIN).addScope(Plus.SCOPE_PLUS_PROFILE)
			.addScope(new Scope("https://www.googleapis.com/auth/userinfo.email"))
			.setAccountName(accountName).build();
		gClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {

		    @Override
		    public void onConnectionSuspended(int reason) {
			Log.d(TAG, "GoogleApiClient connection suspended: " + reason);
		    }

		    @Override
		    public void onConnected(Bundle bundle) {
			try {
			    final Person me = Plus.PeopleApi.getCurrentPerson(gClient);
			    Log.d(TAG, "Current person: " + me);
			    final String name = me.getDisplayName();
			    final String avatar = me.getImage().getUrl();
			    final String userId = me.getId();
			    final JSONObject result = new JSONObject().put("id", userId).put("email", accountName)
				    .put("name", name).put("avatar", avatar);
			    Log.d(TAG, "Callbacking result: " + result);
			    currentCallback.success(result);
			} catch (JSONException ex) {
			    ex.printStackTrace();
			    currentCallback.error(ex.getLocalizedMessage());
			}
		    }
		});
		gClient.registerConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {

		    @Override
		    public void onConnectionFailed(ConnectionResult result) {
			Log.d(TAG, "GoogleApiClient: onConnectionFailed: " + result);
			if (result.hasResolution()) {
			    try {
				cordova.setActivityResultCallback(GooglePlusConnect.this);
				result.startResolutionForResult(cordova.getActivity(), REQUEST_GCLIENT_RECOVER);
			    } catch (SendIntentException ex) {
				pClient.connect();
			    }
			} else {
			    currentCallback.error("PlusClient: Failed to connect");
			}
		    }
		});
		gClient.connect();
	    }
	});
    }

    private void disconnect() {
	Log.d(TAG, "Disconnecting with " + accountName);
	cordova.getThreadPool().execute(new Runnable() {
	    final GooglePlayServicesClient.ConnectionCallbacks connectionCallbacks = new GooglePlayServicesClient.ConnectionCallbacks() {

		@Override
		public void onDisconnected() {
		    Log.d(TAG, "PlusClient: Disconnected while creating.");
		    currentCallback.error("PlusClient: Disconnected Somehow");
		}

		@Override
		public void onConnected(Bundle bundle) {
		    Log.d(TAG, "PlusClient: Connected");
		    pClient.clearDefaultAccount();
		    pClient.revokeAccessAndDisconnect(new PlusClient.OnAccessRevokedListener() {

			@Override
			public void onAccessRevoked(ConnectionResult result) {
			    Log.d(TAG, "PlusClient: onAccessRevoked: " + result);
			    currentCallback.success();
			}
		    });
		}
	    };
	    final GooglePlayServicesClient.OnConnectionFailedListener onConnectionFailedListener = new GooglePlayServicesClient.OnConnectionFailedListener() {
		@Override
		public void onConnectionFailed(ConnectionResult result) {
		    Log.d(TAG, "PlusClient: onConnectionFailed: " + result);
		    if (result.hasResolution()) {
			try {
			    cordova.setActivityResultCallback(GooglePlusConnect.this);
			    result.startResolutionForResult(cordova.getActivity(), REQUEST_PCLIENT_RECOVER);
			} catch (SendIntentException ex) {
			    pClient.connect();
			}
		    } else {
			currentCallback.error("PlusClient: Failed to connect");
		    }
		}
	    };

	    @Override
	    public void run() {
		pClient = new PlusClient.Builder(cordova.getActivity(), connectionCallbacks, onConnectionFailedListener)
			.setAccountName(accountName).build();
		pClient.connect();
	    }
	});
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
	super.onActivityResult(requestCode, resultCode, intent);
	Log.i(TAG, "onActivityResult:" + requestCode);
	switch (requestCode) {
	case REQUEST_PICK_ACCOUNT:
	    if (resultCode == Activity.RESULT_OK) {
		accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
		obtainToken();
	    } else {
		currentCallback.error("No account selected");
	    }
	    break;

	case REQUEST_AUTH_RECOVER:
	    if (resultCode == Activity.RESULT_OK) {
		obtainToken();
	    } else {
		currentCallback.error("Cannot authrorize");
	    }
	    break;

	case REQUEST_PCLIENT_RECOVER:
	    if (resultCode == Activity.RESULT_OK && pClient != null) {
		pClient.connect();
	    } else {
		currentCallback.error("PlusClient: Failed to retry connection");
	    }
	    break;

	case REQUEST_GCLIENT_RECOVER:
	    if (resultCode == Activity.RESULT_OK && pClient != null) {
		gClient.connect();
	    } else {
		currentCallback.error("GoogleApiClient: Failed to retry connection");
	    }
	    break;

	default:
	    break;
	}
    }
}
