define([
	'utilities',
	'text!../../../../templates/desktop/users.html'

], function(utilities, usersTemplate) {
	var UsersView = Backbone.View.extend({
		events : {
			"click a" : "update"
		},
		render : function() {
			utilities.applyTemplate($(this.el), usersTemplate, {
				model : this.model
			});
			
			return this;
		}
	});

	return UsersView;
});