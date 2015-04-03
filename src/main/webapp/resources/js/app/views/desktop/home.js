define([
        
	'utilities',
	'text!../../../../templates/desktop/home.html'
	
], function(utilities, HomeTemplate) {
	var HomeView = Backbone.View.extend({
		events    : {
			'click button[id="loginButton"]' : 'doLogin',
			'click button[id="resetButton"]' : 'resetForm'
		},
		
		render    : function() {
			utilities.applyTemplate($(this.el), HomeTemplate, {});
			return this;
		},
		
		doLogin   : function(event) {
			
		},
		
		resetForm : function(event) {
			$('#j_username,#j_password').val('');
		}
	});
	
	return HomeView;
});