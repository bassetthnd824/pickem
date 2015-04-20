define([
	'configuration',
	'backbone'
], function(config, Backbone) {
	
	var Login = Backbone.Model.extend({
		urlRoot : config.baseUrl + 'rest/login'
	});
	
	return Login;
});