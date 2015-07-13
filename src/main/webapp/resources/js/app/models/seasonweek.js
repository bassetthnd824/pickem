define([
	'configuration',
	'backbone'
], function(config, Backbone) {
	
	var SeasonWeek = Backbone.Model.extend({
		urlRoot : config.baseUrl + 'rest/seasonweek'
	});
	
	return SeasonWeek;
});