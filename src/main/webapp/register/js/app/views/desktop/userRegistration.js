define([
	'jquery',
	'utilities',
	'text!templates/desktop/registration.html'
	
], function($, utilities, RegistrationTemplate) {
	var UserRegistrationView = Backbone.View.extend({
		events             : {
			'click button[id="submitButton"]' : 'submitRegistration',
			'click button[id="resetButton"]'  : 'resetForm'
		},
		
		render             : function() {
			$('nav.nav-bar').hide();
			utilities.applyTemplate($(this.el), RegistrationTemplate, {});
			return this;
		},
		
		submitRegistration : function(event) {
			
		},
		
		resetForm          : function(event) {
			$('#emailAddress,#userPass,#confirmPass,#firstName,#lastName').val('');
		}
	});
	
	return UserRegistrationView;
});