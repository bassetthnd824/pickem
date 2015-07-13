define([
	'app/models/seasonweek',
	'configuration',
	'backbone'
	
], function(SeasonWeek, config, Backbone) {
	
	var SeasonWeeks = Backbone.Collection.extend({
		url        : config.baseUrl + 'rest/seasonweek',
		model      : SeasonWeek,
		id         : 'id',
		comparator : function(model) {
			return model.get('season').season + model.get('weekNumber').toZeroPaddedString(2);
		}
	});
	
	return SeasonWeeks;
});