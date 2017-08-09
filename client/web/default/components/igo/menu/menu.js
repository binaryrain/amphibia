'use strict';

function igoMenu($) {

	return {
		preinit: function(el) {
			var lis = $('>ul>li', el).event('click.select', function(e) {
				lis.removeClass('selected');
				$(e.currentTarget).addClass('selected');
			});
		},

		init: function(el, self) {
			self.$lis = $('>ul>li', el);
		},

		selected: {
			get: function() {
				return this.$lis.filter('.selected');
			},
			set: function(li) {
				this.$lis.removeClass('selected');
				li.addClass('selected');
			}
		}
	};
}
igoMenu.register = null;