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
			var activeUser = JSON.parse(sessionStorage.getItem(config.activeUser));
			
			if (activeUser) {
				sessionStorage.removeItem(config.activeUser);
				sessionStorage.removeItem(config.isManager);
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
					$.getJSON(config.baseUrl + 'rest/user/active', {}, function(user, textStatus, jqXHR) {
						var isManager = false;
						
						for (var i = 0; i < user.userGroups.length; i++) {
							if (user.userGroups[i].group.groupName === 'manager') {
								isManager = true;
								break;
							}
						}
						
						if (!isManager) {
							$('nav.navbar li.dropdown').hide();
						}
						
						$('nav.navbar').show();
						sessionStorage.setItem(config.activeUser, JSON.stringify(user));
						sessionStorage.setItem(config.isManager, isManager);
						require('router').navigate('game', true);
					});
				},
				
				error    : function(model, response, options) {
					alert('login failed');
				}
			});
		},
		
		resetForm : function(event) {
			$('#username,#password').val('');
		}
	});
	
	return LoginView;
});