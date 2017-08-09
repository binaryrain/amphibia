'use strict';

function juiDropdown($) {

	return {
		preinit: function(el) {
			$('>select', el).dropdown()
				.dropdown('menuWidget');
		}
	};
}

juiDropdown.register = function($) {
/*{{IMPORT:node_modules/jquery-ui/ui/widgets/selectmenu.js}}*/

	$.widget('custom.dropdown', $.ui.selectmenu, {
		_renderItem: function(ul, item) {
			if (item.disabled) {
				return $('<li style="display: none">').append($('<div>')).appendTo(ul);
			}

			var li = $('<li>'),
				wrapper = $('<div>', {
					text: item.label,
					class: item.element.attr('menu-class'),
					style: item.element.attr('menu-style')
				});

			$('<span>', {
				class: item.element.attr('icon-class'),
				style: item.element.attr('icon-style')
			}).appendTo(wrapper);

			return li.append(wrapper).appendTo(ul);
		}
	});
};