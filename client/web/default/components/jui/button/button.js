'use strict';

function juiButton($) {

	return {
		preinit: function(el) {
			$('>button', el).button();
		}
	};
}

juiButton.register = function() {
/*{{IMPORT:node_modules/jquery-ui/ui/widgets/button.js}}*/
};