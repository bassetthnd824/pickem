/**
 * A module for the router of the desktop application
 */
define('router', [
	'jquery',
	'underscore',
	'configuration',
	'utilities',
	'app/models/login',
	'app/models/season',
	'app/models/theme',
	'app/models/userRegistration',
	'app/models/user',
	'app/collections/seasons',
	'app/collections/themes',
	'app/collections/users',
	'app/views/desktop/login',
	'app/views/desktop/season',
	'app/views/desktop/seasons',
	'app/views/desktop/themes',
	'app/views/desktop/userRegistration',
	'app/views/desktop/users',
	'text!../templates/desktop/main.html'
],function ($,
			_,
			config,
			utilities,
			Login,
			Season,
			Theme,
			UserRegistration,
			User,
			Seasons,
			Themes,
			Users,
			LoginView,
			SeasonView,
			SeasonsView,
			ThemesView,
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
				if ('#register' !== window.location.hash) {
					this.login.apply(this, args);
				} else {
					callback.apply(this, args);
				}
			}
		},
		
		routes           : {
			''           : 'login',
			'game'       : 'main',
			'register'   : 'userRegistration',
			'season/:id' : 'season',
			'seasons'    : 'seasons',
			'themes'     : 'themes',
			'users'      : 'users'
		},
		
		login             : function() {
			var loginView = new LoginView({
				model : new Login(),
				el    : $('#content')
			});
			
			utilities.viewManager.showView(loginView);
		},
		
		main             : function() {
			$('#content').empty().append('This is the main screen');
		},
		
		userRegistration : function() {
			var userRegistrationView = new UserRegistrationView({
				model : new UserRegistration(),
				el    : $('#content')
			});
			
			utilities.viewManager.showView(userRegistrationView);
		},
		
		season            : function(id) {
			var model = null;
			
			if (id === 'new') {
				model = new Season();
			} else {
				model = new Season({
					id : id
				});
			}
			
			var seasonView = new SeasonView({
				model : model,
				el    : $("#content")
			});
			
			model.on("change", function() {
				utilities.viewManager.showView(seasonView);
			});
			
			if (model.isNew()) {
				seasonView.render();
			} else {
				model.fetch({
					error : function() {
						utilities.displayAlert("Failed to retrieve the season from the Pickem server.");
					}
				});
			}
		},
		
		seasons           : function() {
			var seasons = new Seasons();
			var seasonsView = new SeasonsView({
				model : seasons,
				el    : $('#content')
			});
			
			seasons.on('reset', function() {
				utilities.viewManager.showView(seasonsView);
				
			}).fetch({
				reset : true,
				error : function() {
					utilities.displayAlert('Failed to retrieve seasons from the Pickem server.');
				}
			});
		},
		
		themes           : function() {
			var themes = new Themes();
			var themesView = new ThemesView({
				model : themes,
				el    : $('#content')
			});
			
			themes.on('reset', function() {
				utilities.viewManager.showView(themesView);
				
			}).fetch({
				reset : true,
				error : function() {
					utilities.displayAlert('Failed to retrieve themes from the Pickem server.');
				}
			});
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