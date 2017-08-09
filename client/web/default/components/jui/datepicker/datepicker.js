'use strict';

function juiDatepicker($) {

	return {
		preinit: function(el) {
			var bg = {},
				input = $('>input', el),
				prompt = input.attr('prompt'),
				date = input.attr('ts') ? new Date(Number(input.attr('ts'))) : null;

			if (input.attr('d')) {
				bg.dayNamesMin = input.attr('d').split(',');
			}
			if (input.attr('m')) {
				bg.monthNames = input.attr('m').split(',');
			}
			$.datepicker.regional.bg = bg;
			$.datepicker.setDefaults($.datepicker.regional.bg);

			input.datepicker().datepicker('setDate', date);

			if (!date && prompt) {
				input.val(prompt);
			}
		}
	};
}

juiDatepicker.register = function() {
/*{{IMPORT:node_modules/jquery-ui/ui/widgets/datepicker.js}}*/
};