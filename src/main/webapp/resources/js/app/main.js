$(document).ready(function() {
	var renumberMatchups = function($parentElement) {
		var rank = parseInt($('#numberOfConferenceTeams').val(), 10);
		
		$parentElement.find('.matchup-rank span').each(function(i) {
			var $thisSpan = $(this);
			$thisSpan.text(rank);
			$thisSpan.next('input[type="hidden"]').val(rank);
			rank--;
		});
	};
	
	$('button.up-button').click(function(event) {
		var $this = $(this);
		var $thisRow = $this.parents('.weekly-matchup');
		var $prevRow = $thisRow.prev();
		var $thisRowParent = $thisRow.parents('.weekly-matchups');
		
		if ($prevRow.length > 0) {
			$thisRow.insertBefore($prevRow);
		}
		
		renumberMatchups($thisRowParent);
	});
	
	$('button.down-button').click(function(event) {
		var $this = $(this);
		var $thisRow = $this.parents('.weekly-matchup');
		var $nextRow = $thisRow.next();
		var $thisRowParent = $thisRow.parents('.weekly-matchups');
		
		if ($nextRow.length > 0) {
			$thisRow.insertAfter($nextRow);
		}
		
		renumberMatchups($thisRowParent);
	});
});