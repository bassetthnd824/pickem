define([
	'configuration',
	'backbone'
], function(config, Backbone) {
	
	var User = Backbone.Model.extend({
		urlRoot : config.baseUrl + 'rest/user'
	});
	
	return User;
});