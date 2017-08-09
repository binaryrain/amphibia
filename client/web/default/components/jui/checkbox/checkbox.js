'use strict';

function juiCheckbox($) {

	return {
		preinit: function(el) {
			$('>input', el).checkboxradio();
		}
	};
}

juiCheckbox.register = function() {
/*{{IMPORT:node_modules/jquery-ui/ui/widgets/checkboxradio.js}}*/
};