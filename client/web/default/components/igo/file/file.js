'use strict';

function igoFile($) {

	return {
		fieldName: 'file',
		fileData: null,

		init: function(el, self) {
			self.$input = $('>input', el).event('change.file', function() {
				if (!this.files.length) {
					return;
				}
				var formData = new window.FormData(),
					file = this.files[0],
					ext = file.name.split('.').pop().toLowerCase();
				this.value = null;
				formData.append(self.fieldName || 'file', file);
				self.load(function(data) {
					self.file = data;
				}, formData, ext, file);
			});
		},

		field: {
			get: function() {
				return this.fieldName;
			},
			set: function(value) {
				this.fieldName = value;
			}
		},

		file: {
			get: function() {
				return this.fileData;
			},
			set: function(value) {
				this.fileData = value;
			}
		},

		load: function(callback, formData, ext, file) {
			callback(file.name);
		}
	};
}
igoFile.register = null;