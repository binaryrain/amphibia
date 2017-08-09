'use strict';

function juiDialog($) {

	return {
		preinit: function(el) {
			var dialog,
				div = $('>div', el),
				buttons = div.find('input').val(),
				opts = {
					modal: true,
					autoOpen: false,
					buttons: {}
				};

			if (buttons) {
				JSON.parse(buttons).forEach(function(item) {
					opts.buttons[item.label] = function() {
						$(this).trigger(item.event);
						if (item.event === 'close') {
							$(this).dialog('close');
						}
					};
				});
			}

			dialog = div.dialog(opts);
			$('*[dialog_id="' + el.attr('id') + '"]').event('click.show', function() {
				dialog.dialog('open');
			});
		}
	};
}

juiDialog.register = function() {
/*{{IMPORT:node_modules/jquery-ui/ui/widgets/dialog.js}}*/
};