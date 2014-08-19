var plugin = {
	getAccessToken: function(accountName, taker, error) {
		var arg;
		if (accountName) {
			arg = [accountName];
		} else {
			arg = [];
		}
		console.log('Calling GooglePlusConnectPlugin#getAccessToken(' + arg +')');
		cordova.exec(taker, error, 'GooglePlusConnectPlugin', 'login', arg);
	},
	disconnect: function(accountName, taker, error) {
		var arg;
		if (accountName) {
			arg = [accountName];
		} else {
			arg = [];
		}
		console.log('Calling GooglePlusConnectPlugin#disconnect(' + arg +')');
		cordova.exec(taker, error, 'GooglePlusConnectPlugin', 'disconnect', arg);
	},
	profile: function(accountName, taker, error) {
		var arg;
		if (accountName) {
			arg = [accountName];
		} else {
			arg = [];
		}
		console.log('Calling GooglePlusConnectPlugin#profile(' + arg +')');
		cordova.exec(taker, error, 'GooglePlusConnectPlugin', 'profile', arg);
	}
};
module.exports = plugin;
