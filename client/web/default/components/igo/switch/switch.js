'use strict';

function igoSwitch($) {

	return {
		init: function(el, self) {
			self.$input = $('>label>input', el).event('change.check', function() {
				self.checked = self.$input.is(':checked');
			});
			this.onEvent('change', self.$input);
		},

		checked: {
			get: function() {
				return this.$input.prop('checked');
			},
			set: function(value) {
				this.$input.prop('checked', value);
			}
		}
	};
}
igoSwitch.register = null;