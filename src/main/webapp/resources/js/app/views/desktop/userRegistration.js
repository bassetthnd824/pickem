define([
	'jquery',
	'utilities',
	'text!../../../../templates/desktop/registration.html'
	
], function($, utilities, RegistrationTemplate) {
	var UserRegistrationView = Backbone.View.extend({
		events             : {
			'click button[id="submitButton"]' : 'submitRegistration',
			'click button[id="resetButton"]'  : 'resetForm'
		},
		
		render             : function() {
			$('nav.navbar').hide();
			utilities.applyTemplate($(this.el), RegistrationTemplate, {});
			return this;
		},
		
		submitRegistration : function(event) {
			this.model.set({
				emailAddress : $('#emailAddress').val(),
				userPass     : $('#userPass').val(),
				confirmPass  : $('#confirmPass').val(),
				firstName    : $('#firstName').val(),
				lastName     : $('#lastName').val()
			}).save();
			
			$('nav.navbar').show();
			window.location.hash = '#game';
		},
		
		resetForm          : function(event) {
			$('#emailAddress,#userPass,#confirmPass,#firstName,#lastName').val('');
		}
	});
	
	return UserRegistrationView;
});