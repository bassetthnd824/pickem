define([
	'require',
	'utilities',
	'app/collections/collectionscache',
	'text!../../../../templates/desktop/seasonweek.html'

], function(require, utilities, collectionscache, seasonWeekTemplate) {
	var SeasonWeekView = Backbone.View.extend({
		events     : {
			'click #saveButton'   : 'save',
			'click #cancelButton' : 'cancel',
			'click #deleteButton' : 'deleteRow',
		},
		
		initialize : function() {
			this.model.bind('change', this.render, this);
		},
		
		render     : function() {
			var thisView = this;
			
			utilities.applyTemplate($(thisView.el), seasonWeekTemplate, {
				model   : thisView.model,
				seasons : collectionscache.seasons
			});
			
			if (thisView.model.isNew()) {
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
				season     : {
					id : $('#season').val()
				},
				weekNumber : $('#weekNumber').val(),
				beginDate  : $('#beginDate').val() + 'T00:00:00.000-0600',
				endDate    : $('#endDate').val() + 'T00:00:00.000-0600'
			
			}).save(null, {
				dataType : 'text',
				success  : function(model, response, options) {
					require('router').navigate('seasonweeks', true);
				},
				
				error    : function(model, response, options) {
					alert('SeasonWeek could not be updated on the server');
				}
			});
			
		},
		
		cancel     : function() {
			require('router').navigate('seasonweeks', true);
		},
		
		deleteRow  : function() {
			this.model.destroy({
				wait    : true,
				
				success : function(model, response) {
					require('router').navigate('seasonweeks', true);
				},
				
				error   : function(model, response) {
					alert('SeasonWeek could not be deleted from the server');
				}
			});
		}
	});

	return SeasonWeekView;
});