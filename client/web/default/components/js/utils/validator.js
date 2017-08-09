'use strict';

define(['./errcode.js'], function(ErrCode) {

	var proto, _ = function() {
		this.errors = [];
	};
	_.prototype = proto = {
		addError: function(val, code, name) {
			this.errors.push(new ErrCode(val, code, name));
			return false;
		},

		validate: function(val, name, asserts) {
			for (var i = 0; i < asserts.length; i += 2) {
				var code = asserts[i], assert = asserts[i + 1],
					errors = this.errors;
				errors.some(function(error, index) {
					if (error.code === code && error.name === name) {
						errors.splice(index, 1);
						return false;
					}
				});

				if (typeof assert === 'function' ? assert() : assert) {
					return this.addError(val, code, name);
				}
			}
			return true;
		},

		isValid: function() {
			return this.errors.length === 0;
		},

		isError: function() {
			return this.errors.length !== 0;
		},

		getErrors: function() {
			return this.errors;
		},

		reset: function() {
			this.errors.length = 0;
			return this;
		},

		/**
		 * Retrieve min and max length of number.
		 * @param {Mixed} val The variable to be checked.
		 * @param {Number} [min] Minimum length allowed for model value.
		 * @param {Number} [max] Maximum length allowed for model value.
		 * @param {String} [name] Name of the field or variable.
		 * @return {Boolean} Return true is valid otherwise false.
		*/
		minmax: function(val, min, max, name) {
			val = parseFloat(val);
			return this.validate(val, name, [
				ErrCode.INVALID_VALUE, isNaN(val),
				ErrCode.INVALID_MIN, val < min,
				ErrCode.INVALID_MAX, max && val > max]);
		},

		/**
		 * Retrieve min and max length of string.
		 * @param {Mixed} val The variable to be checked.
		 * @param {Number} [min] Minimum length allowed for model value.
		 * @param {Number} [max] Maximum length allowed for model value.
		 * @param {String} [name] Name of the field or variable.
		 * @return {Boolean} Return true is valid otherwise false.
		*/
		str_minmax: function(val, min, max, name) {
			return this.validate(val, name, [
				ErrCode.INVALID_VALUE, typeof val !== 'string',
				ErrCode.INVALID_MIN, function() { return val.trim().length < min },
				ErrCode.INVALID_MAX, function() { return val.length > max }]);
		},

		/**
		 * Determine if a variable is set and is not NULL
		 * @param {Mixed} val The variable to be checked.
		 * @param {String} [name] Name of the field or variable.
		 * @return {Boolean} Return true is valid otherwise false.
		*/
		isset: function(val, name) {
			return this.validate(val, name, [
				ErrCode.INVALID_VALUE, val === undefined || val === null]);
		},

		/**
		 * Determine whether a string is empty.
		 * @param {Mixed} val The variable to be checked.
		 * @param {String} [name] Name of the field or variable.
		 * @return {Boolean} Return true is valid otherwise false.
		*/
		empty: function(val, name) {
			return this.validate(val, name, [
				ErrCode.INVALID_VALUE, val === undefined || val === null,
				ErrCode.INVALID_TYPE, function() { return typeof val !== 'string'; },
				ErrCode.INVALID_VALUE, function() { return val.trim().length === 0 }]);
		}
	};
	return _;
});