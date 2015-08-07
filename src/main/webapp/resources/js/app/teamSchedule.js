$(document).ready(function() {
	$('select[name="teamId"]').change(function(event) {
		var $this = $(this);
		var parameters = {
			teamId : $this.val()
		};
		
		$.ajax({
			url     : 'teamSchedule_get.action',
			data    : parameters,
			type    : 'GET',
			success : function(data, textStatus, jqXHR) {
				$this.parents('div[class*="col-xs-"]').find('table').remove();
				var compiledTemplate = _.template($('#tableTemplate').html());
				$this.parents('div[class*="col-xs-"]').append(compiledTemplate({matchups : data}));
			}
		});
	});
});