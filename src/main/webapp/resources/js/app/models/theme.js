define([
	'configuration',
	'backbone'
], function(config, Backbone) {
	
	var Theme = Backbone.Model.extend({
		urlRoot : config.baseUrl + 'rest/theme'
	});
	
	return Theme;
});