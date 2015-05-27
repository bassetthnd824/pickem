define([
	'require',
	'utilities',
	'text!../../../../templates/desktop/theme.html'

], function(require, utilities, themeTemplate) {
	var ThemeView = Backbone.View.extend({
		events     : {
			'click #saveButton'   : 'save',
			'click #cancelButton' : 'cancel',
			'click #deleteButton' : 'deleteRow',
		},
		
		initialize : function() {
			this.model.bind('change', this.render, this);
		},
		
		render     : function() {
			utilities.applyTemplate($(this.el), themeTemplate, {
				model : this.model
			});
			
			if (this.model.isNew()) {
				$('#deleteButton').hide();
			} else {
				$('#deleteButton').show();
			}
			
			return this;
		},
		
		save       : function() {
			this.model.set({
				themeName : $('#themeName').val(),
				themePath : $('#themePath').val(),
				active    : $('#active').val()
			
			}).save(null, {
				dataType : 'text',
				success  : function(model, response, options) {
					require('router').navigate('themes', true);
				},
				
				error    : function(model, response, options) {
					alert('Theme could not be updated on the server');
				}
			});
			
		},
		
		cancel     : function() {
			require('router').navigate('themes', true);
		},
		
		deleteRow  : function() {
			this.model.destroy({
				wait    : true,
				
				success : function(model, response) {
					require('router').navigate('themes', true);
				},
				
				error   : function(model, response) {
					alert('Theme could not be deleted from the server');
				}
			});
		}
	});

	return ThemeView;
});