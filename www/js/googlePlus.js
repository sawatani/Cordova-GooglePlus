var plugin = {
	getAccessToken: function(taker, error) {
		console.log('Into getAccessToken');
		cordova.exec(taker, error, 'GooglePlusConnectPlugin', 'login', []);
	}
}
module.exports = plugin;
