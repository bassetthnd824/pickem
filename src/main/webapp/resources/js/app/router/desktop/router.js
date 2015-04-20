/**
 * A module for the router of the desktop application
 */
define("router", [
	'jquery',
	'underscore',
	'configuration',
	'utilities',
	'app/models/login',
	'app/models/userRegistration',
	'app/views/desktop/login',
	'app/views/desktop/userRegistration',
	'text!../templates/desktop/main.html'
],function ($,
			_,
			config,
			utilities,
			Login,
			UserRegistration,
			LoginView,
			UserRegistrationView,
			MainTemplate) {
	
	$(document).ready(new function() {
		utilities.applyTemplate($('body'), MainTemplate)
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
		
		routes           : {
			''         : 'login',
			'register' : 'userRegistration'
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
		}
	});
	
	// Create a router instance
	var router = new Router();
	
	return router;
});