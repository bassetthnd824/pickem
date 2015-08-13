$(document).ready(function() {
	var setWeekBeginDate = function(seasonId) {
		$.ajax({
			url     : 'seasons_getSeasonById.action',
			data    : {
				'model.id' : seasonId
			},
			type    : 'GET',
			success : function(data, textStatus, jqXHR) {
				var beginMoment = moment(data.beginDate);
				var daysToAdd = (parseInt($('#seasonWeek_weekNumber').val(), 10) - 1) * 7;
				beginMoment.add(daysToAdd, 'days')
				$('.begin-date').data('DateTimePicker').minDate(beginMoment);
				$('.begin-date').data('DateTimePicker').defaultDate(beginMoment);
				$('.end-date').data('DateTimePicker').minDate(beginMoment);
				beginMoment.add(6, 'days');
				$('.end-date').data('DateTimePicker').maxDate(beginMoment);
				$('.end-date').data('DateTimePicker').defaultDate(beginMoment);
				$('.begin-date').data('DateTimePicker').maxDate(beginMoment);
			}
		});
	};
	
	$('.date').datetimepicker({
		format     : 'YYYY-MM-DD',
		useCurrent : false
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
	
	if ($('#formMode').val() === 'add' || $('#formMode').val() === 'edit') {
		$('#seasonWeek_season_id').change(function(event) {
			var $this = $(this);
			
			if ($('#seasonWeek_weekNumber').val()) {
				setWeekBeginDate($this.val());
			}
		});
		
		$('#seasonWeek_weekNumber').change(function(event) {
			var $this = $(this);
			
			if ($('#seasonWeek_season_id').val()) {
				setWeekBeginDate($('#seasonWeek_season_id').val());
			}
		});
		
		if ($('#seasonWeek_season_id').val() && $('#seasonWeek_weekNumber').val()) {
			$('#seasonWeek_weekNumber').trigger('change');
		}
		
		$('#matchups_seasonWeek_id').change(function(event) {
			var $this = $(this);
			
			$.ajax({
				url     : 'seasonWeeks_getSeasonWeekById.action',
				data    : {
					'model.id' : $this.val()
				},
				type    : 'GET',
				success : function(data, textStatus, jqXHR) {
					var beginMoment = moment(data.beginDate);
					var endMoment = moment(data.endDate);
					
					var daysToAdd = (parseInt($('#seasonWeek_weekNumber').val(), 10) - 1) * 7;
					beginMoment.add(daysToAdd, 'days')
					$('#matchups_matchupDate').data('DateTimePicker').minDate(beginMoment);
					$('#matchups_matchupDate').data('DateTimePicker').maxDate(endMoment);
					$('#matchups_matchupDate').data('DateTimePicker').defaultDate(beginMoment.add(2, 'days'));
				}
			});
		});
		
		$('#matchups_homeTeam_id').change(function(event) {
			var $this = $(this);
			
			$.ajax({
				url     : 'teams_getTeamById.action',
				data    : {
					'model.id' : $this.val()
				},
				type    : 'GET',
				success : function(data, textStatus, jqXHR) {
					$('#matchups_venue_id').val(data.homeVenue.id);
				}
			});
		});
	}
	
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
				
				if ($slave.val()) {
					$slave.trigger('change');
				}
			}
		});
	});
	
	if ($('select.double-select-master').val()) {
		$('select.double-select-master').trigger('change');
	}
});