'use strict';

function juiTabs($) {

	return {
		preinit: function(el) {
			var tabs = $('>div', el).tabs({
				active: Number($('>input', el).val())
			});

			var divs = $('>div>div', el);
			for (var i = 0; i < divs.length; i++) {
				var div = divs.eq(i),
					template = div.attr('template');
				if (template) {
					div.html($('script' + template).html() || '');
				}
			}

			tabs.tabs('refresh');
		}
	};
}

juiTabs.register = function() {
/*{{IMPORT:node_modules/jquery-ui/ui/widgets/tabs.js}}*/
};