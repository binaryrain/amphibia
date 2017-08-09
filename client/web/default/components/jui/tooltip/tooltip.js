'use strict';

function juiTooltip($) {

	return {
		preinit: function(el) {
			var div = $('>div', el),
				target = $(div.attr('target')),
				gap = 5, at, my,
				arr = (div.attr('position') || '').split('-');
			if (arr[0] === 'top') {
				my = arr[1] + ' bottom-' + gap;
				at = arr[1] + ' ' + arr[0];
			} else if (arr[0] === 'bottom') {
				my = arr[1] + ' top+' + gap;
				at = arr[1] + ' ' + arr[0];
			} else if (arr[0] === 'left') {
				at = 'right+' + gap + ' ' + arr[1];
				my = arr[0] + ' ' + arr[1];
			} else if (arr[0] === 'right') {
				at = 'left-' + gap + ' ' + arr[1];
				my = arr[0] + ' ' + arr[1];
			} else {
				my = 'center bottom-' + gap;
				at = 'center top';
			}

			var opts = {
					items: '*',
					position: {
						my: my,
						at: at
					},
					show: {
						effect: 'blind',
						delay: 250
					},
					hide: {
						effect: 'explode',
						delay: 250
					},
					tooltipClass: div.attr('tltclass'),
					content: function() {
						return div.html() || target.html();
					}
				};
			target.tooltip(opts);
		}
	};
}

juiTooltip.register = function() {
/*{{IMPORT:node_modules/jquery-ui/ui/widgets/tooltip.js}}*/
};