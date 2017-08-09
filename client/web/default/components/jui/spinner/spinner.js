'use strict';

function juiSpinner($) {

	return {
		preinit: function(el) {
			var input = $('>input', el),
				opts = {
					numberFormat: input.attr('numberFormat')
				};
			['value', 'min', 'max', 'step'].forEach(function(name) {
				if (input.attr(name) !== undefined) {
					opts[name] = Number(input.attr(name));
				}
			});
			input.spinner(opts);
			if (input.attr('culture')) {
				input.spinner('option', 'culture', 'ja-JP');
			}
		}
	};
}

juiSpinner.register = function() {
/*{{IMPORT:node_modules/jquery-ui/ui/widgets/spinner.js}}*/
};