'use strict';

function juiProgressbar($) {

	return {
		preinit: function(el) {
			var label = $('>div>span', el),
				bar = $('>pre', el).progressbar({
					value: label.attr('value') === undefined ? false : Number(label.attr('value')),
				change: function() {
					label.text(bar.progressbar('value') + '%');
				},
				complete: function() {
					label.text(label.attr('complete'));
				}
			});
		}
	};
}

juiProgressbar.register = function() {
/*{{IMPORT:node_modules/jquery-ui/ui/widgets/progressbar.js}}*/
};