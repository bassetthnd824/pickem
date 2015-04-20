requirejs.config({
	baseUrl : 'resources/js',
	paths   : {
		jquery     : 'libs/jquery-min-2.1.3',
		underscore : 'libs/underscore-min-1.8.2',
		text       : 'libs/text-2.0.12',
		bootstrap  : 'libs/bootstrap-min-3.3.4',
		backbone   : 'libs/backbone-min-1.1.2',
		utilities  : 'app/utilities',
		router     : 'app/router/desktop/router'
	},
	shim    : {
		'backbone'   : {
			deps    : ['jquery', 'bootstrap', 'underscore'],
			exports : 'Backbone'
		},
		'underscore' : {
			exports : '_'
		}
	}
});

define('initializer', ['jquery'], function($) {
	$.ajaxSetup({ cache : false });
	$('head').append('<link rel="stylesheet" href="resources/css/bootstrap-min-3.3.4.css" type="text/css" media="all"/>' +
			'<link rel="stylesheet" href="resources/css/bootstrap-theme-min-3.3.4.css" type="text/css" media="all"/>' +
			'<link rel="stylesheet" href="resources/css/pickem.css" type="text/css" media="all"/>' +
			'<link href="http://fonts.googleapis.com/css?family=Rokkitt" rel="stylesheet" type="text/css">');
});

require(['initializer', 'router'], function() {
	
});

define('configuration', {
	baseUrl : ''
});