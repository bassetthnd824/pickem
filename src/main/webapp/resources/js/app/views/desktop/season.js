define([
	'require',
	'utilities',
	'text!../../../../templates/desktop/season.html'

], function(require, utilities, seasonTemplate) {
	var SeasonView = Backbone.View.extend({
		events     : {
			'click #saveButton'   : 'save',
			'click #cancelButton' : 'cancel',
			'click #deleteButton' : 'deleteRow',
		},
		
		initialize : function() {
			this.model.bind('change', this.render, this);
		},
		
		render     : function() {
			utilities.applyTemplate($(this.el), seasonTemplate, {
				model : this.model
			});
			
			if (this.model.isNew()) {
				$('#deleteButton').hide();
			} else {
				$('#deleteButton').show();
			}
			
			$('#beginDate').datetimepicker({
				format : 'YYYY-MM-DD'
			});
			$('#endDate').datetimepicker({
				format : 'YYYY-MM-DD'
			});
			$('#beginDate').on('dp.change', function(e) {
				$('#endDate').data('DateTimePicker').minDate(e.date);
			});
			$('#endDate').on('dp.change', function(e) {
				$('#beginDate').data('DateTimePicker').maxDate(e.date);
			});
			
			return this;
		},
		
		save       : function() {
			this.model.set({
				season    : $('#season').val(),
				beginDate : $('#beginDate').val() + 'T00:00:00.000-0600',
				endDate   : $('#endDate').val() + 'T00:00:00.000-0600'
			
			}).save(null, {
				dataType : 'text',
				success  : function(model, response, options) {
					require('router').navigate('seasons', true);
				},
				
				error    : function(model, response, options) {
					alert('Season could not be updated on the server');
				}
			});
			
		},
		
		cancel     : function() {
			require('router').navigate('seasons', true);
		},
		
		deleteRow  : function() {
			this.model.destroy({
				wait    : true,
				
				success : function(model, response) {
					require('router').navigate('seasons', true);
				},
				
				error   : function(model, response) {
					alert('Season could not be deleted from the server');
				}
			});
		}
	});

	return SeasonView;
});