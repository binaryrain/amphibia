'use strict';

window.indigoMain(function($, indigo) {
	indigo.debug('Init index.js');


	var main = function() {
		require([
			'request'
		], function(Request) {
			var wait_overlay = window.top.$('.wait_overlay'),
				ajax = Request(indigo.contextPath, {
					contentType: 'application/json',
					overlay: function(type, skipOverlay) {
						if (!skipOverlay) {
							if (type === Request.OVERLAY_SHOW) {
								wait_overlay.show();
							} else {
								wait_overlay.hide();
							}
						}
					}
				});

			indigo.namespace('.step1', function(ns) {
				var model = {},
					fileData = null,
					file = ns.create('File'),
					span = ns.$('.file span');

				indigo.watch(model, function(name, value) {
						if (name === 'url') {
							model.file = null;
							span.text('');
						} else {
							model.url = null;
							span.text(value);
						}
					})
					.bind('url', 'value', ns.create('Input')).trigger('keydown keyup')
					.bind('file', 'file', file);

				file.load = function(callback, formData, ext, file) {
					ajax.file(function(err, data) {
						if (err || !data.id) {
							return _.errorDialog(err, 'error');
						}
						callback(file.name);
					}, '/file/upload', formData);
				};

				ns.create('Button').click = function() {
					ajax.post(function(err, result) {
						console.log(err, result)
					},'/save/step1', model);
				}
			});
		});
	};

	require.config({
		baseUrl: indigo.componentPath + '/js/utils',
		paths: {
			request: 'request' + (indigo.DEBUG ? '' : '.min')
		},
		callback: main
	});
});