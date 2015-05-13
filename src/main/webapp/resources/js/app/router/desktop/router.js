/**
 * A module for the router of the desktop application
 */
define('router', [
	'jquery',
	'underscore',
	'configuration',
	'utilities',
	'app/models/login',
	'app/models/userRegistration',
	'app/models/user',
	'app/collections/users',
	'app/views/desktop/login',
	'app/views/desktop/userRegistration',
	'app/views/desktop/users',
	'text!../templates/desktop/main.html'
],function ($,
			_,
			config,
			utilities,
			Login,
			UserRegistration,
			User,
			Users,
			LoginView,
			UserRegistrationView,
			UsersView,
			MainTemplate) {
	
	$(document).ready(new function() {
		utilities.applyTemplate($('body'), MainTemplate);
	})
	
	/**
	 * The Router class contains all the routes within the application - i.e.
	 * URLs and the actions that will be taken as a result.
	 * 
	 * @type {Router}
	 */
	
	var Router = Backbone.Router.extend({
		initialize       : function() {
			//Begin dispatching routes
			Backbone.history.start();
		},
		
		execute          : function(callback, args) {
			var managerOpts = ['#users', '#themes', '#teams', '#venues', '#rivalries', '#seasons', '#seasonWeeks', '#matchups'];
			var activeUser = JSON.parse(sessionStorage.getItem(config.activeUser));
			var isManager = sessionStorage.getItem(config.isManager);
			var isManagerOption = managerOpts.indexOf(window.location.hash) > -1;
			
			if (activeUser) {
				if (!isManagerOption || (isManagerOption && isManager)) {
					if (callback) {
						callback.apply(this, args);
					}
				} else {
					this.main.apply(this, args);
				}
			} else {
				this.login.apply(this, args);
			}
		},
		
		routes           : {
			''         : 'login',
			'register' : 'userRegistration',
			'game'     : 'main',
			'users'    : 'users'
		},
		
		login             : function() {
			var loginView = new LoginView({
				model : new Login(),
				el    : $('#content')
			});
			
			utilities.viewManager.showView(loginView);
		},
		
		userRegistration : function() {
			var userRegistrationView = new UserRegistrationView({
				model : new UserRegistration(),
				el    : $('#content')
			});
			
			utilities.viewManager.showView(userRegistrationView);
		},
		
		main             : function() {
			$('#content').empty().append('This is the main screen');
		},
		
		users            : function() {
			var users = new Users();
			var usersView = new UsersView({
				model : users,
				el    : $('#content')
			});
			
			users.on('reset', function() {
				utilities.viewManager.showView(usersView);
				
			}).fetch({
				reset : true,
				error : function() {
					utilities.displayAlert('Failed to retrieve users from the Pickem server.');
				}
			});
		}
	});
	
	// Create a router instance
	var router = new Router();
	
	return router;
});