define([
	'app/models/user',
	'configuration',
	'backbone'
	
], function(User, config, Backbone) {
	
	var Users = Backbone.Collection.extend({
		url        : config.baseUrl + 'rest/users',
		model      : User,
		id         : 'id',
		comparator : function(model) {
			return model.get('userName');
		}
	});
	
	return Users;
});