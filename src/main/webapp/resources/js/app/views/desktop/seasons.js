define([
	'utilities',
	'text!../../../../templates/desktop/seasons.html'

], function(utilities, seasonsTemplate) {
	var SeasonsView = Backbone.View.extend({
		events     : {
			'click button.add'    : 'showAdd',
			'click button.edit'   : 'showEdit',
		},
		
		initialize : function() {
			this.model.on('update', this.render, this);
		},
		
		render     : function() {
			utilities.applyTemplate($(this.el), seasonsTemplate, {
				model : this.model
			});
			
			return this;
		},
		
		showAdd    : function() {
			require('router').navigate('season/new', true);
		},
		
		showEdit   : function(event) {
			require('router').navigate('season/' + $(event.target).data('id'), true);
		}
	});

	return SeasonsView;
});