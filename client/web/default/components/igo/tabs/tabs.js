'use strict';

function igoTabs($, selector, ig) {

	return {
		preinit: function(el) {
			var section = $('>section', el),
				divs = section.find('>div'),
				inputs = $('>div>input', el).event('change.tabs', function() {
					divs.hide();
					var index = inputs.filter(':checked').parent().index();
					divs.eq(index).show();
				});
			for (var i = 0; i < divs.length; i++) {
				var div = divs.eq(i);
				ig.template(div, div.attr('template'));
			}
			inputs.trigger('change.tabs');
		}
	};
}
igoTabs.register = null;