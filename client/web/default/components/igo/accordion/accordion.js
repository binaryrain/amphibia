'use strict';

function igoAccordion($, selector, ig) {

	return {
		preinit: function(el) {
			var divs = $('>div>div[template]', el);
			for (var i = 0; i < divs.length; i++) {
				var div = divs.eq(i);
				ig.template(div, div.attr('template'));
			}
		}
	};
}
igoAccordion.register = null;