define([
	'configuration',
	'backbone'
], function(config, Backbone) {
	
	var UserRegistration = Backbone.Model.extend({
		urlRoot : config.baseUrl + relativePath + 'rest/registration'
	});
	
	return UserRegistration;
});