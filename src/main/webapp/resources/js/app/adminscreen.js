$(document).ready(function() {
	$('#beginDate').datetimepicker({
		format : 'YYYY-MM-DD'
	});
	
	$('#endDate').datetimepicker({
		format : 'YYYY-MM-DD'
	});
	
	$('#beginDate').on('dp.change', function(e) {
		if (e.date && $(this).val()) {
			$('#endDate').data('DateTimePicker').minDate(e.date);
			$('#endDate').data('DateTimePicker').defaultDate(moment(e.date).add(7, $('#endDate').data('duration')));
		}
	});
	
	$('#endDate').on('dp.change', function(e) {
		if (e.date && $(this).val()) {
			$('#beginDate').data('DateTimePicker').maxDate(e.date);
		}
	});
});