define([
	'configuration',
	'backbone'
], function(config, Backbone) {
	
	var UserRegistration = Backbone.Model.extend({
		urlRoot : config.baseUrl + 'rest/registration'
	});
	
	return UserRegistration;
});