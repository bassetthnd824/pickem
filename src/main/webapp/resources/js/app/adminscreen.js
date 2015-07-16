$(document).ready(function() {
	$('#beginDate').datetimepicker({
		format : 'YYYY-MM-DD'
	});
	
	$('#endDate').datetimepicker({
		format : 'YYYY-MM-DD'
	});
	
	$('#beginDate').blur(function(event) {
		var $this = $(this);
		var beginMoment = moment($this.val());
		
		if ($this.val()) {
			$('#endDate').data('DateTimePicker').minDate(beginMoment);
			$('#endDate').data('DateTimePicker').defaultDate(beginMoment.add(6, $('#endDate').data('duration')));
		}
	});
	
	$('#endDate').blur(function(event) {
		var $this = $(this);
		var endMoment = moment($this.val());
		
		if ($(this).val()) {
			$('#beginDate').data('DateTimePicker').maxDate(endMoment);
		}
	});
});