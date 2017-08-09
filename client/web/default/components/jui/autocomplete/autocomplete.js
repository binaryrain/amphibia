'use strict';

function juiAutocomplete($) {

	return {
		preinit: function(el) {
			var input = $('>input', el);
			input.autocomplete({
				source: input.attr('tags').split(',')
			});
		}
	};
}

juiAutocomplete.register = function() {
/*{{IMPORT:node_modules/jquery-ui/ui/widgets/autocomplete.js}}*/
};