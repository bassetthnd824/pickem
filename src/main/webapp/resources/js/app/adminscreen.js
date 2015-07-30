$(document).ready(function() {
	$('.date').datetimepicker({
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
	
	$('select.double-select-master').change(function(event) {
		var $this = $(this);
		var $slave = $('#' + $this.data('slaveId'));
		$slave.children(':not(:first)').remove();
		var parameters = {};
		
		parameters[$this.data('parameterName')] = $this.val();
		
		$.ajax({
			url     : $this.data('actionName'),
			data    : parameters,
			type    : 'GET',
			success : function(data, textStatus, jqXHR) {
				var options = [];
				var valueProp = $slave.data('valueProp');
				var textProp = $slave.data('textProp');
				var hiddenValue = $('#' + $slave.data('hiddenValueHolder')).val();
				
				for (var i = 0; i < data.length; i++) {
					var item = data[i];
					var option = '<option value="' + item[valueProp] + '"' + ((parseFloat(hiddenValue) === parseFloat(item[valueProp])) ? ' selected="selected"' : '') + '>' + item[textProp] + '</option>';
					options.push(option);
				}
				
				$slave.append(options.join(''));
			}
		});
	});
	
	if ($('select.double-select-master').val()) {
		$('select.double-select-master').trigger('change');
	}
});