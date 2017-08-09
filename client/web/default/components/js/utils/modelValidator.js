'use strict';

define(['./validator.js'], function(Validator) {

	var proto = Validator.prototype,
		_ = function(model) {
			Validator.call(this);
			this.model = model;
			this.super = function(api, name) {
				var args = [this.model[name]].concat(Array.prototype.slice.call(arguments, 2), name);
				return proto[api].apply(this, args);
			};
		};
	_.prototype = {
		minmax: function(name, min, max) {
			return this.super('minmax', name, min, max);
		},

		str_minmax: function(name, min, max) {
			return this.super('str_minmax', name, min, max);
		},

		isset: function(name) {
			return this.super('isset', name);
		},

		empty: function(name) {
			return this.super('empty', name);
		}
	};
	for (var name in proto) {
		if (!_.prototype[name]) {
			_.prototype[name] = proto[name];
		}
	}

	return _;
});