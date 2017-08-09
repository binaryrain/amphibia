'use strict';

define([], function() {
	var _ = function(value, code, name) {
		this.value = value;
		this.code = code;
		this.name = name;
	};
	_.INVALID_VALUE = 0;
	_.INVALID_TYPE = 1;
	_.INVALID_MIN = 2;
	_.INVALID_MAX = 3;
	return _;
});