'use strict';

function igoButton($) {

	return {
		init: function(el) {
			this.$button = $('>button', el);
			this.onEvent('click', el);
		},

		label: {
			get: function() {
				return this.$button.html();
			},
			set: function(value) {
				this.$button.html(value);
			}
		}
	};
}
igoButton.register = null;