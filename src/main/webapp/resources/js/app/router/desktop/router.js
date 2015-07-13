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
	'app/models/seasonweek',
	'app/models/theme',
	'app/models/userRegistration',
	'app/models/user',
	'app/collections/collectionscache',
	'app/views/desktop/login',
	'app/views/desktop/season',
	'app/views/desktop/seasons',
	'app/views/desktop/seasonweek',
	'app/views/desktop/seasonweeks',
	'app/views/desktop/theme',
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
			SeasonWeek,
			Theme,
			UserRegistration,
			User,
			collectionscache,
			LoginView,
			SeasonView,
			SeasonsView,
			SeasonWeekView,
			SeasonWeeksView,
			ThemeView,
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
			var managerOpts = ['#users', '#themes', '#teams', '#venues', '#rivalries', '#seasons', '#seasonweeks', '#matchups'];
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
			''               : 'login',
			'game'           : 'main',
			'register'       : 'userRegistration',
			'season/:id'     : 'season',
			'seasons'        : 'seasons',
			'seasonweek/:id' : 'seasonweek',
			'seasonweeks'    : 'seasonweeks',
			'theme/:id'      : 'theme',
			'themes'         : 'themes',
			'users'          : 'users'
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
			var seasons = collectionscache.seasons;
			var seasonsView = new SeasonsView({
				model : seasons,
				el    : $('#content')
			});
			
			seasons.on('reset', function() {
				utilities.viewManager.showView(seasonsView);
			});
			
			seasons.trigger('reset');
		},
		
		seasonweek        : function(id) {
			var model = null;
			
			if (id === 'new') {
				model = new SeasonWeek();
			} else {
				model = new SeasonWeek({
					id : id
				});
			}
			
			var seasonWeekView = new SeasonWeekView({
				model   : model,
				el      : $("#content")
			});
			
			model.on("change", function() {
				utilities.viewManager.showView(seasonWeekView);
			});
			
			if (model.isNew()) {
				seasonWeekView.render();
			} else {
				model.fetch({
					error : function() {
						utilities.displayAlert("Failed to retrieve the seasonweek from the Pickem server.");
					}
				});
			}
		},
		
		seasonweeks       : function() {
			var seasonWeeks = new SeasonWeeks();
			var seasonWeeksView = new SeasonWeeksView({
				model : seasonWeeks,
				el    : $('#content')
			});
			
			seasonWeeks.on('reset', function() {
				utilities.viewManager.showView(seasonWeeksView);
			});
		},
		
		theme            : function(id) {
			var model = null;
			
			if (id === 'new') {
				model = new Theme();
			} else {
				model = new Theme({
					id : id
				});
			}
			
			var themeView = new ThemeView({
				model : model,
				el    : $("#content")
			});
			
			model.on("change", function() {
				utilities.viewManager.showView(themeView);
			});
			
			if (model.isNew()) {
				themeView.render();
			} else {
				model.fetch({
					error : function() {
						utilities.displayAlert("Failed to retrieve the theme from the Pickem server.");
					}
				});
			}
		},
		
		themes           : function() {
			var themes = new Themes();
			var themesView = new ThemesView({
				model : themes,
				el    : $('#content')
			});
			
			themes.on('reset', function() {
				utilities.viewManager.showView(themesView);
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
			});
		}
	});
	
	// Create a router instance
	var router = new Router();
	
	return router;
});