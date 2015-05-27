define([
	'utilities',
	'text!../../../../templates/desktop/themes.html'

], function(utilities, themesTemplate) {
	var ThemesView = Backbone.View.extend({
		events : {
			"click a" : "update"
		},
		render : function() {
			utilities.applyTemplate($(this.el), themesTemplate, {
				model : this.model
			});
			
			return this;
		}
	});

	return ThemesView;
});