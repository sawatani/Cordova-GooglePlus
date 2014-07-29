var plugin = {
	getAccessToken: function(accountName, taker, error) {
		var arg;
		if (accountName) {
			arg = [accountName]
		} else {
			arg = []
		}
		console.log('Calling GooglePlusConnectPlugin#getAccessToken(' + arg +')');
		cordova.exec(taker, error, 'GooglePlusConnectPlugin', 'login', arg);
	}
}
module.exports = plugin;
