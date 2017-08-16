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

			var txtPreview = $('#preview'),
				step1Model = {}, swaggerModel = {},
				projectName, dlgNewSwagger, swaggerDropdown,
				propertiesDropdown, projectFile;

			indigo.namespace('.dlgSwagger', function(ns) {
				dlgNewSwagger = ns.create('igoDialog');

				var submitButton = ns.create('Button', '#submit'),
					file = ns.create('File'),
					span = ns.$('.fileName');

				indigo.watch(swaggerModel, function(name, value, model) {
					if (name === 'url') {
						model.file = null;
						span.text('');
					} else {
						model.url = null;
						span.text(value);
					}
					step1Model['-i'] = value;
					submitButton.disabled = !(model.file || model.url);
				})
				.bind('url', 'value', ns.create('Input')).trigger('keydown keyup')
				.bind('file', 'file', file);

				file.load = function(callback, formData, ext, file) {
					if ('json' !== ext.toLowerCase()) {
						//_.errorDialog({error: 'Invalid Extension'}, 'error');
					}
					ajax.file(function(err, data) {
						if (err || !data.file) {
							return _.errorDialog(err, 'error');
						}
						callback(data.file);
					}, '/file/upload', formData);
				};

				submitButton.click = function() {
					var path = swaggerModel.file || swaggerModel.url;
					swaggerPreview(path);
					swaggerDropdown.addRow(path, true);
					dlgNewSwagger.show = false;
				}
			});

			indigo.namespace('.step1', function(ns) {
				projectName = ns.create('igoInput', '#name');
				(swaggerDropdown = ns.create('Dropdown', '#swagger')).change = function(e, target) {
					if(target.index === 0) {
						swaggerModel.url = null;
						swaggerModel.file = null;
						dlgNewSwagger.show = true;
					} else {
						swaggerPreview(target.value);
					}
				};

				(propertiesDropdown = ns.create('Dropdown', '#properties')).change = function(e, target) {
					var uploadProperties = indigo.create('File', '#uploadProperties');
					uploadProperties.load = function(callback, formData, ext, file) {
						if ('json' !== ext.toLowerCase()) {
							//_.errorDialog({error: 'Invalid Extension'}, 'error');
						}
						ajax.file(function(err, data) {
							if (err || !data.file) {
								return _.errorDialog(err, 'error');
							}
							step1Model['-p'] = data.file;
							propertiesDropdown.addRow(data.file, true);
							callback(data.file);
							preview(data.file);
						}, '/file/upload', formData);
					};

					if(target && target.index !== 0) {
						preview(target.value);
					}

					setTimeout(function() {propertiesDropdown.open = false;}, 500);
				};

				indigo.bind(step1Model, '-s', 'checked', ns.create('Checkbox', '#schemas'))
						.bind('tests', '-t', ns.create('Checkbox', '#tests'))

				ns.create('Button', '#compile').click = function() {
					step1Model['-n'] = projectName.value;
					ajax.post(function(err, data) {
						if (err) {
							txtPreview.html(err);
						} else {
							txtPreview.html(data);
							projectFile = data.trim().split('\n').pop();
							$('.step2').removeClass('disabled');
						}
					}, '/compile', step1Model);
				};
			});
			propertiesDropdown.itemRenderer = '<li>%LABEL%</li>';

			indigo.namespace('.step2', function(ns) {
				var dd = ns.create('Dropdown');

				ns.create('Button', '#export').click = function() {
					ajax.post(function(err, data) {
						if (err) {
							txtPreview.html(err);
						} else {
							txtPreview.html(data);
							$('.step2').removeClass('disabled');
						}
					}, '/export', {'-f': dd.option.attr('data'), '-i': projectFile});
				};
			});

			function swaggerPreview(path) {
				preview(path, function(data) {
					if (!projectName.value && data && data.info) {
						projectName.value = data.info.title.replace(/\s/g, '');
					}
				});
			}

			function preview(path, callback) {
				ajax.post(function(err, data) {
					if (err) {
						txtPreview.html(err);
					} else {
						txtPreview.html(data);
						if (typeof data === 'object') {
							try {
								txtPreview.html(JSON.stringify(data, null, 4));
							} catch (e) {}
						}
						if (callback) {
							callback(data);
						}
					}
				}, '/preview', {path: path});
			}
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