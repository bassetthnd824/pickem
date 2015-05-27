define([
	'app/models/theme',
	'configuration',
	'backbone'
	
], function(Theme, config, Backbone) {
	
	var Themes = Backbone.Collection.extend({
		url        : config.baseUrl + 'rest/theme',
		model      : Theme,
		id         : 'id',
		comparator : function(model) {
			return model.get('themeName');
		}
	});
	
	return Themes;
});