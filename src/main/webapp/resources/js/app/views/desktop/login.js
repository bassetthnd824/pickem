define([
	'utilities',
	'configuration',
	'text!../../../../templates/desktop/login.html'
	
], function(utilities, config, LoginTemplate) {
	var LoginView = Backbone.View.extend({
		events    : {
			'click button[id="loginButton"]' : 'doLogin',
			'click button[id="resetButton"]' : 'resetForm'
		},
		
		render    : function() {
			var activeUser = JSON.parse(sessionStorage.getItem('com.curleesoft.pickem.ActiveUser'));
			
			if (activeUser) {
				sessionStorage.removeItem('com.curleesoft.pickem.ActiveUser');
				$.ajax({
					dataType : 'json',
					url      : 'rest/logout',
					data     : {},
					method   : 'DELETE',
					success  : function(data) {
						console.log('logout success');
					}
				});
			}
			
			$('nav.navbar').hide();
			utilities.applyTemplate($(this.el), LoginTemplate, {});
			return this;
		},
		
		doLogin   : function(event) {
			this.model.set({
				username : $('#username').val(),
				password : $('#password').val(),
			}).save(null, {
				dataType : 'text',
				success  : function(model, response, options) {
					$.getJSON(config.baseUrl + 'rest/user/active', {}, function(data, textStatus, jqXHR) {
						sessionStorage.setItem('com.curleesoft.pickem.ActiveUser', JSON.stringify(data));
						$('nav.navbar').show();
						window.location.hash = '#game';
					});
				}
			});
		},
		
		resetForm : function(event) {
			$('#username,#password').val('');
		}
	});
	
	return LoginView;
});