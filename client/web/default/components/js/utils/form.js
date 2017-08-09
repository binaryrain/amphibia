'use strict';

define(['./modelValidator.js'], function(ModelValidator) {

	var Form = function(ns, watch) {
		this.watch = watch || function() {};
		this.ns = ns;
		this.fields = {};
		this.store = {};
		this.validator = new ModelValidator(this.store);
		this.requiredFields = [];
	};

	Form.prototype = {
		validate: function() {
			var validator = this.validator;
			this.requiredFields.forEach(function(name) {
				validator.empty(name);
			});
			return validator.isValid();
		},

		getErrors: function() {
			return this.validator.getErrors();
		},

		getFields: function() {
			return this.fields;
		},

		errorFields: function(className) {
			className = className || 'error_field';
			var errors = this.getErrors(),
				errFields = {},
				fields = this.fields;
			errors.forEach(function(error) {
				var field = fields[error.name];
				if (field) {
					field.class(className, true);
					errFields[error.name] = field;
				}
			});
			return errFields;
		},

		intercept: function(name, value) {
			if (this.store[name] !== undefined) {
				this.store[name] = value;
				this.watch(this.validate(), name, value);
			}
		},

		builder: function(field) {
			var comp = this.ns.create(field.type, field.sel);
			this.promise.bind(field.name, field.prop || 'value', comp);
			this.fields[field.name] = comp;
			if (field.required !== false) {
				this.requiredFields.push(field.name);
			}
		},

		add: function(model, fields) {
			var store = this.store,
				self = this;
			if (!this.promise) {
				this.promise = window.indigo.watch(model, function(name, value) {
					self.intercept(name, value);
				});
			}
			fields.forEach(function(field, index) {
				self.builder(field, index);
				store[field.name] = model[field.name] || null; //enable this.intercept condition, prevent dispatch a watch on init
			});

			this.watch(this.validate());
		}
	};

	return Form;
});