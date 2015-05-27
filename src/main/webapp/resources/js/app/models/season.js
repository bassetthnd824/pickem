define([
	'configuration',
	'backbone'
], function(config, Backbone) {
	
	var Season = Backbone.Model.extend({
		urlRoot : config.baseUrl + 'rest/season'
	});
	
	return Season;
});