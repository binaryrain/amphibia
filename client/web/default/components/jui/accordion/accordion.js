'use strict';

function juiAccordion($) {

	return {
		preinit: function(el) {
			el.accordion({
				active: Number($('>input', el).val())
			});

			var divs = $('>div', el);
			for (var i = 0; i < divs.length; i++) {
				var div = divs.eq(i),
					template = div.attr('template');
				if (template) {
					div.html($('script' + template).html() || '');
				}
			}

			el.accordion('refresh');
		}
	};
}

juiAccordion.register = function() {
/*{{IMPORT:node_modules/jquery-ui/ui/widgets/accordion.js}}*/
};