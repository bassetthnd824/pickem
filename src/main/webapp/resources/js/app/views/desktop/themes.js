define([
	'utilities',
	'text!../../../../templates/desktop/themes.html'

], function(utilities, themesTemplate) {
	var ThemesView = Backbone.View.extend({
		events    : {
			'click button.add'    : 'showAdd',
			'click button.edit'   : 'showEdit',
		},
		
		render : function() {
			utilities.applyTemplate($(this.el), themesTemplate, {
				model : this.model
			});
			
			return this;
		},
		
		showAdd   : function() {
			require('router').navigate('theme/new', true);
		},
		
		showEdit  : function(event) {
			require('router').navigate('theme/' + $(event.target).data('id'), true);
		}
	});

	return ThemesView;
});