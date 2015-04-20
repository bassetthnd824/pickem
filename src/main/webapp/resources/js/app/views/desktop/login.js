define([
	'utilities',
	'text!../../../../templates/desktop/login.html'
	
], function(utilities, LoginTemplate) {
	var LoginView = Backbone.View.extend({
		events    : {
			'click button[id="loginButton"]' : 'doLogin',
			'click button[id="resetButton"]' : 'resetForm'
		},
		
		render    : function() {
			$('nav.navbar').hide();
			utilities.applyTemplate($(this.el), LoginTemplate, {});
			return this;
		},
		
		doLogin   : function(event) {
			this.model.save({
				username : $('#username').val(),
				password : $('#password').val(),
			});
		},
		
		resetForm : function(event) {
			$('#username,#password').val('');
		}
	});
	
	return LoginView;
});