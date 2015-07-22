$(document).ready(function() {
	$('.begin-date').datetimepicker({
		format : 'YYYY-MM-DD'
	});
	
	$('.end-date').datetimepicker({
		format : 'YYYY-MM-DD'
	});
	
	$('.begin-date').blur(function(event) {
		var $this = $(this);
		var beginMoment = moment($this.val());
		
		if ($this.val() && $('#formMode').val() !== 'init' && $('#formMode').val() !== 'search') {
			$('.end-date').data('DateTimePicker').minDate(beginMoment);
			$('.end-date').data('DateTimePicker').defaultDate(beginMoment.add(6, $('.end-date').data('duration')));
		}
	});
	
	$('.end-date').blur(function(event) {
		var $this = $(this);
		var endMoment = moment($this.val());
		
		if ($(this).val() && $('#formMode').val() !== 'init' && $('#formMode').val() !== 'search') {
			$('.begin-date').data('DateTimePicker').maxDate(endMoment);
		}
	});
});