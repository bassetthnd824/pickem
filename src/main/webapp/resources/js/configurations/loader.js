define(function() {
	var environment;
	
	if (Modernizr.touch || Modernizr.mq('only all and (max-width: 480px)')) {
		environment = 'mobile';
	} else {
		environment = 'desktop';
	}
	
	require([environment]);
});