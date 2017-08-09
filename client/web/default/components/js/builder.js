'use strict';

(function(ig) {
	var staticPath = ig.staticPath || '/static',
		contextPath = ig.contextPath || './build',
		targets = document.querySelectorAll('[indigo-builder]'),
		componentIndex = 0,
		packages = {},
		templates = {},
		libs = [],
		loader = function(url, callback) {
			var xhr = new window.XMLHttpRequest();
			xhr.open('GET', url);
			xhr.onreadystatechange = function() {
				if(xhr.readyState === window.XMLHttpRequest.DONE && xhr.status === 200) {
					callback(xhr.responseText);
				}
			};
			xhr.send();
		},
		init = function(pkgs) {
			for (var i = 0; i < pkgs.length; i++) {
				if (!packages[pkgs[i]]) {
					return;
				}
			}
			for (i = 0; i < targets.length; i++) {
				ig.builder(targets[i].getAttribute('indigo-builder'), targets[i]);
			}
		},
		createModel = function(model, opts) {
			model = model || {};
			model.contextPath = contextPath,
			model.baseStaticPath = staticPath,
			model.opts = opts || {},
			model.$ = function(type, opts) {
				var begin, html = '';
				if (type.charCodeAt(0) < 91) {
					type = 'igo' + type; //Button -> igoButton 
				}
				if (templates[type]) {
					opts = opts || {};
					model.opts = opts || {};
					model.componentIndex = ++componentIndex;
					begin = '<c _=' + type + ' tabindex="-1" class="init' + model.$get('class') + '"' + model.$get('disabled', 'disabled') + model.$attr('id');
					html = window.ejs.render(templates[type], model);
					if (opts.show === false) {
						opts.parentStyle = 'display: none; ' + (opts.parentStyle || '');
					}
					html = begin + model.$attr('parentStyle', 'style') + '>' + html + '</c>';
				}
				return html;
			};
			model.$get = function(name, val) {
				if (model.opts[name] !== undefined) {
					if (val === undefined) {
						return ' ' + model.opts[name];
					} else {
						return ' ' + val;
					}
				}
				return '';
			};
			model.$attr = function(name, tagName) {
				if (model.opts[name] !== undefined) {
					if (tagName === undefined) {
						return ' ' + name + '="' + model.opts[name] + '"';
					} else {
						return ' '  + tagName + '="' + model.opts[name] + '"';
					}
				}
				return '';
			};
			model.$attrs = function(name) {
				var attrs = [],
					obj = model.opts[name];
				for (var key in obj) {
					attrs.push(key + '="' + obj[key] + '"');
				}
				return attrs.length ? ' ' + attrs.join(' ') : '';
			};
			model.$css = function(name, tagName) {
				if (model.opts[name] !== undefined) {
					if (tagName === undefined) {
						return ' ' + name + ': ' + model.opts[name] + ';';
					} else {
						return ' ' + tagName + ': ' + model.opts[name] + ';';
					}
				}
				return '';
			};
			model.$title = function(title) {
				title = title || model.opts.title;
				return ' tabindex="' + model.componentIndex + '"' + (title ? ' title="' + title + '"' : '');
			};
			model.$label = function(title) {
				title = title || model.opts.title;
				return ' tabindex="' + model.componentIndex + '"' + (title ? ' aria-label="' + title + '"' : '');
			};
			model.$assign = function(opts, model) {
				model = model || opts;
				return createModel(model, opts);
			};
			model.$finalize = function() {
				return arguments.length ? '<input igo-main type="hidden" value="' + [].slice.call(arguments).join() + '"/>' : '';
			};
			return model;
		};

	ig.package = function(pkg, callback) {
		if (packages[pkg]) {
			return callback();
		}
		loader('./build/' + pkg + 'Components.html', function(data) {
			var arr = data.split(/<<\[\[(.*)\]\]>>/);
			for (var i = 1; i < arr.length; i += 2) {
				libs.push(arr[i]);
				templates[arr[i].replace('!', '')] = arr[i + 1];
			}
			packages[pkg] = true;
			init(pkgs);
			callback(pkg);
		});
	};

	ig.builder = function(contentPath, parent) {
		var model = createModel();
		loader(contentPath, function(data) {
			var html = window.ejs.render(data, model);
			parent.innerHTML = html;

			var script = document.createElement('script');
			script.src = ig.DEBUG ? 'js/loader.js' : contextPath + '/static/js/loader.min.js';
			if (ig.DEBUG === false) {
				script.setAttribute('uri', contextPath);
			}
			script.setAttribute('rel', 'igocore');
			script.setAttribute('libs', libs.join(','));
			parent.appendChild(script);

			var scripts = parent.querySelectorAll('script');
			for (var i = 0; i < scripts.length; i++) { //execute embedded scripts
				try {
					script = scripts[i];
					/*jshint evil:true */
					if ((script.type || '').indexOf('template') === -1) {
						eval(script.innerHTML);
					}
				} catch (e) { window.console.error(e, script.innerHTML);}
			}
			parent.dispatchEvent(new window.Event('CONTENT_LOADED'));
		});
	};

	var script = document.querySelector('[indigo-pkgs]'),
		pkgs = script ? script.getAttribute('indigo-pkgs').split(',') : [];
	pkgs.forEach(function(pkg) {
		if (!templates[pkg]) {
			ig.package(pkg, function() {});
		} else {
			init(pkgs);
		}
	});
})(window.top.indigoJS = (window.top.indigoJS || {}));