'use strict';

function juiMenu($) {

	return {
		preinit: function(el) {
			$('>ul', el).menu();
		}
	};
}

juiMenu.register = function() {
/*{{IMPORT:node_modules/jquery-ui/ui/widgets/menu.js}}*/
};