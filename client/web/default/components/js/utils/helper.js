'use strict';

define([], function() {

	return {
		substitute: function(str) {
			for (var i = 1; i < arguments.length; i += 2) {
				str = str.replace(new RegExp(arguments[i].replace('{', '\\{').replace('}', '\\}'), 'g'), arguments[i + 1]);
			}
			return str;
		},

		scrollView: function(comp) {
			comp.$el[0].scrollIntoView(true);
		}
	};
});