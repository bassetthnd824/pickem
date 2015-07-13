define([
	'backbone',
	'underscore',
	'app/collections/seasons',
	'app/collections/seasonweeks',
	'app/collections/themes',
	'app/collections/users'
], function(
	Backbone, 
	_,
	Seasons,
	SeasonWeeks,
	Themes,
	Users
) {
	var collectionscache = {};
	
	collectionscache.seasons = new Seasons();
	collectionscache.seasonWeeks = new SeasonWeeks();
	collectionscache.themes = new Themes();
	collectionscache.users = new Users();
	
	collectionscache.seasons.fetch({
		reset : true,
		error : function() {
			utilities.displayAlert('Failed to retrieve seasons from the Pickem server.');
		}
	});
	
	collectionscache.seasonWeeks.fetch({
		reset : true,
		error : function() {
			utilities.displayAlert('Failed to retrieve seasonweeks from the Pickem server.');
		}
	});
	
	collectionscache.themes.fetch({
		reset : true,
		error : function() {
			utilities.displayAlert('Failed to retrieve themes from the Pickem server.');
		}
	});
	
	collectionscache.users.fetch({
		reset : true,
		error : function() {
			utilities.displayAlert('Failed to retrieve users from the Pickem server.');
		}
	});
	
	return collectionscache;
});