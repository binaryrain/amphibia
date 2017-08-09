'use strict';

function igoDialog($, selector, ig) {

	return {
		preinit: function(el) {
			var ref = this.initForms(el, {});

			ig.template(ref.$header, ref.$header.attr('uid'));
			ig.template(ref.$section, ref.$section.attr('uid'));
			ig.template(ref.$footer, ref.$footer.attr('uid'));

			el.find('.close').event('click.hide', function() {
				el.hide();
				el.trigger('close');
			});

			$('*[dialog_id="' + el.attr('id') + '"]').event('click.show', function() {
				el.show();
			});
		},

		initForms: function(el, ref) {
			ref.$header = $('>div>header', el);
			ref.$section = $('>div>section', el);
			ref.$footer = $('>div>footer', el);
			return ref;
		},

		init: function(el) {
			this.initForms(el, this);
			this.onEvent('close', el);
			this.defaultTitle = this.$header.find('.title').html();
			this.defaultContent = this.$section.find('.content').html();
		},

		title: {
			get: function() {
				return this.$header.find('.title').html();
			},
			set: function(value) {
				return this.$header.find('.title').html(value);
			}
		},

		content: {
			get: function() {
				return this.$section.find('.content').html();
			},
			set: function(value) {
				return this.$section.find('.content').html(value);
			}
		},

		show: {
			get: function() {
				return this.$el.css('display') !== 'none';
			},
			set: function(value) {
				this.$el.css('display', value ? 'block' : 'none');
				if (value) {
					this.$section.find('input, textarea').eq(0).focus();
				} else {
					this.$el.trigger('close');
				}
			}
		}
	};
}
igoDialog.register = null;