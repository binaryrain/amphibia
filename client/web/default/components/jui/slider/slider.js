'use strict';

function juiSlider($) {

	return {
		preinit: function(el) {
			var handle = $('>pre>.ui-slider-handle', el),
				slider = $('>pre', el),
				opts = {
					min: Number(slider.attr('min')) || 0,
					max: Number(slider.attr('max')) || 100
				};
			if (handle.length) {
				opts.create = function() {
					handle.text(slider.slider('value'));
				};
				opts.slide = function(event, ui) {
					handle.text(ui.value);
				};
			}
			if (slider.attr('value') !== undefined) {
				opts.value = Number(slider.attr('value'));
			}

			slider.slider(opts);
		}
	};
}

juiSlider.register = function() {
/*{{IMPORT:node_modules/jquery-ui/ui/widgets/slider.js}}*/
};