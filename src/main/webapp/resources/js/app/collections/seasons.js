define([
	'app/models/season',
	'configuration',
	'backbone'
	
], function(Season, config, Backbone) {
	
	var Seasons = Backbone.Collection.extend({
		url        : config.baseUrl + 'rest/season',
		model      : Season,
		id         : 'id',
		comparator : function(model) {
			return model.get('season');
		}
	});
	
	return Seasons;
});