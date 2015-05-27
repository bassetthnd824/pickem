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
				}
			});
		},
		
		resetForm          : function(event) {
			$('#emailAddress,#userPass,#confirmPass,#firstName,#lastName').val('');
		}
	});
	
	return UserRegistrationView;
});