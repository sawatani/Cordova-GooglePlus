#Cordova-GooglePlus

Cordova Plugin for Google+ login/disconnect
## Install

    cordova plugin add https://github.com/sawatani/Cordova-GooglePlus.git --variable IOS_CLIENT_ID='YOUR-CLIENT_ID-FOR-IOS'

## Usage
### Login

    googlePlusConnectPlugin.getAccessToken(email, onSuccess, onError)

* email  
On Android, If _email_ is null or invalid, Account Select Dialog will be shown.  
On iOS, this parameter is ignored and uses Default Account.
* onSuccess  
Taking success object which has properties of accountName and accessToken.  
_accountName_ is name of Google+ account and represent email address.  
_accessToken_ is token for access to user info.
* onError  
Taking error message.

This access require the permission to basic_userinfo, email, and action of AddActivity.

### Disconnect

    googlePlusConnectPlugin.disconnect(email, onSuccess, onError)

* email  
The email address which is taken by getAccessToken method.
* onSuccess  
Invoked on success with no parameters.
* onError  
Taking error message.
