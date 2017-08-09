'use strict';

function igoTogglebox($, selector, ig) {

	return {
		preinit: function(el) {
			var divs = $('>div[template]', el);
			for (var i = 0; i < divs.length; i++) {
				var div = divs.eq(i);
				ig.template(div, div.attr('template'));
			}
		}
	};
}
igoTogglebox.register = null;