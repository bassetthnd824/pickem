var path = window.location.pathname;
var relativePath = '';

if (path.indexOf('/register/') >= 0 || path.indexOf('/admin/') >= 0 || path.indexOf('/game/') >= 0) {
	relativePath = '../';
}

requirejs.config({
	baseUrl : '',
	paths   : {
		jquery     : relativePath + 'resources/js/libs/jquery-min-2.1.3',
		underscore : relativePath + 'resources/js/libs/underscore-min-1.8.2',
		text       : relativePath + 'resources/js/libs/text-2.0.12',
		bootstrap  : relativePath + 'resources/js/libs/bootstrap-min-3.3.4',
		backbone   : relativePath + 'resources/js/libs/backbone-min-1.1.2',
		utilities  : relativePath + 'resources/js/app/utilities',
		router     : relativePath + 'resources/js/app/router/desktop/router'
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
	$('head').append('<link rel="stylesheet" href="' + relativePath + 'resources/css/bootstrap-min-3.3.4.css" type="text/css" media="all"/>' +
			'<link rel="stylesheet" href="' + relativePath + 'resources/css/bootstrap-theme-min-3.3.4.css" type="text/css" media="all"/>' +
			'<link rel="stylesheet" href="' + relativePath + 'resources/css/pickem.css" type="text/css" media="all"/>' +
			'<link href="http://fonts.googleapis.com/css?family=Rokkitt" rel="stylesheet" type="text/css">');
});

require(['initializer', 'router'], function() {
	
});

define('configuration', {
	baseUrl : ''
});