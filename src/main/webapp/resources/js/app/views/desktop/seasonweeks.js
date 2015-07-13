define([
	'utilities',
	'text!../../../../templates/desktop/seasonweeks.html'

], function(utilities, seasonWeeksTemplate) {
	var SeasonWeeksView = Backbone.View.extend({
		events    : {
			'click button.add'    : 'showAdd',
			'click button.edit'   : 'showEdit',
		},
		
		render    : function() {
			utilities.applyTemplate($(this.el), seasonWeeksTemplate, {
				model : this.model
			});
			
			return this;
		},
		
		showAdd   : function() {
			require('router').navigate('seasonweek/new', true);
		},
		
		showEdit  : function(event) {
			require('router').navigate('seasonweek/' + $(event.target).data('id'), true);
		}
	});

	return SeasonWeeksView;
});