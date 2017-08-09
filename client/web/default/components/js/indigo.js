'use strict';

(function(top) {
	var ig = top.indigoJS = top.indigoJS || {},
		stateModel = function(o) {
			var states = o.__states__;
			if (!states) {
				var value = {};
				states = Object.defineProperty(o, '__states__', {
					get: function() { return value; },
					set: function(val) { return value = val; },
					enumerable: false
				}).__states__;
			}
			return states;
		};

	ig.uid = function () {
		return 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx'.replace(/x/g, function() {
			return (Math.random() * 16 | 0).toString(16);
		});
	};
	ig.import = function() {
		var callback, classes = [],
			length = arguments.length;
		if (typeof arguments[length - 1] === 'function') {
			callback = arguments[--length];
		}
		for (var i = 0; i < length; i++) {
			var type = arguments[i];
			if (type.charCodeAt(0) < 91) {
				type = 'igo' + type; //Button -> igoButton 
			}
			var factory = top[type];
			if (typeof factory !== 'function') {
				throw new Error('ClassNotFoundException: ' + type);
			}
			if (!factory.classInstance) {
				var selector = '[_=' + type + ']',
					apis = factory(window.$, selector, ig),
					clazz = function(el) {
						this.$el = el;
						this.init(el, this);
					};
				factory.classInstance = clazz;
				clazz.selector = selector;

				clazz.prototype.constructor = clazz;
				clazz.prototype.toString = function() {
					return selector + '::' + this.$el.parent().html();
				};
				clazz.prototype.define = function(name, get, set) {
					var descriptor = {};
					if (get) { descriptor.get = get; }
					if (set) { descriptor.set = set; }
					Object.defineProperty(this, name, descriptor);
				};
				clazz.prototype.class = function(name, isAdd) {
					ig.class(this.$el, name, isAdd);
				};
				clazz.prototype.focus = function() {
					this.$el.focus();
				};
				clazz.prototype.onEvent = function(type, comp, intercept) {
					var _hanlder,
						uid = ig.uid();
					Object.defineProperty(this, type, {
						get: function() {
							return _hanlder;
						},
						set: function(hanlder) {
							comp.event(type + '.' + uid, _hanlder = hanlder);
							if (intercept) {
								intercept(hanlder, uid);
							}
						}
					});
				};
				clazz.prototype.init = function() {};

				if (!apis.disabled) {
					apis.disabled = {
						get: function() {
							return !!this.$el.attr('disabled');
						},
						set: function(value) {
							ig.attr(this.$el, 'disabled', value);
						}
					};
				}

				if (!apis.show) {
					apis.show = {
						get: function() {
							return (this.$el.length && this.$el[0].getClientRects().length > 0);
						},
						set: function(value) {
							value ? this.$el.show() : this.$el.hide();
						}
					};
				}

				for (var name in apis) {
					var descriptor = apis[name];
					if (descriptor && descriptor.set && descriptor.get) {
						descriptor.set = (function(set, propName) {
							return function(value) {
								var states = stateModel(this);
								if (!states[propName]) {
									states[propName] = true;
									ig.info(propName, value, this.toString());
									set.call(this, value);
									this.$el.trigger(propName, [value]);
								}
								delete states[propName];
							};
						})(descriptor.set, name);
						Object.defineProperty(clazz.prototype, name, descriptor);
					} else {
						clazz.prototype[name] = descriptor;
					}
				}
			}
			classes.push(factory.classInstance);
		}
		if (callback) {
			callback.apply(this, classes);
		} else {
			return classes[0];
		}
	};
	ig.create = function(clazz, idxOrSelector, parent) {
		parent = parent || window.$('body');
		if (typeof clazz === 'string') {
			clazz = this.import(clazz);
		}
		var el, els = parent.find(clazz.selector),
			type = typeof(idxOrSelector);
		if (type === 'number') {
			el = els.eq(idxOrSelector);
		} else if (type === 'string') {
			el = els.filter(idxOrSelector);
		} else {
			el = els.eq(0);
		}
		return new clazz(el);
	};
	ig.namespace = function(selector, callbak) {
		var parent = window.$(selector),
			ns = {
				$el: parent,
				$: function(sel) { return parent.find(sel); },
				create: function(type, idxOrSelector) {
					return ig.create.call(ig, type, idxOrSelector, parent);
				}
			};
		if (parent.length !== 1) {
			top.console.log('WARN: namespace expected only one matching selector: ' + parent.length);
		}
		if (callbak) { callbak(ns); }
		return ns;
	};
	ig.watch = function(model, watch) {
		watch = watch || function() {
			if (watch) {
				watch.apply(ig, arguments);
			}
		};
		return {
			watch: watch,
			bind: function(prop, chain, host, $watch) {
				if ($watch) {
					return ig.bind(model, prop, [{chain: chain, host: host, $watch: $watch}], host, watch);
				}
				return ig.bind(model, prop, chain, host, watch);
			}
		};
	};
	ig.bind = function(model, prop, chain, host, watch) {
		var states = stateModel(model),
			promise, chains = [];
		model = model || {};
		if (Array.isArray(host)) {
			host.forEach(function(host) {
				chains.push({chain: chain, host: host, $watch: watch});
			});
		} else if (Array.isArray(chain)) {
			chains = chain;
		} else {
			chains[0] = {chain: chain, host: host, $watch: watch};
		}
		if (prop) {
			var val;
			chains.forEach(function(o, index) {
				if (!o.chain || !o.host) {
					for (var key in o) {//transform {chain: host, $watch: $watch} - > {chain: chain, host: host, $watch: $watch}
						if (key !== '$watch') {
							o = {chain: key, host: o[key], $watch: o.$watch};
							break;
						}
					}
					chains[index] = o;
				}
				o.host.$el.on(o.chain, function(e, value) {
					model[prop] = value;
				});
				val = o.host[o.chain];
				if (model[prop] !== val) {
					if (model[prop] !== undefined) {
						val = model[prop];
						if(o.$watch !== watch) {
							o.$watch.call(o.host, prop, model[prop], model, {type: 'bind-' + index});
						}
					} else if (val !== undefined && val !== '') {
						model[prop] = val;
					}
				}
			});
			if (watch && model[prop] !== undefined) {
				watch.call(host, prop, model[prop], model, {type: 'bind'});
			}

			val = model[prop];
			Object.defineProperty(model, prop, {
				get: function() {
					return val;
				},
				set: function(value) {
					var propagate = (val !== value);
					val = value;
					if (!states[prop]) {
						states[prop] = true;
						chains.forEach(function(o, index) {
							if (o.$watch) {
								if (propagate && o.$watch !== watch) {
									o.$watch.call(o.host, prop, value, model, {type: 'model-' + index});
								}
							}
							o.host[o.chain] = value;
						});
						if (watch && propagate) {
							watch.call(host, prop, value, model, {type: 'model'});
						}
						delete states[prop];
					}
				}, enumerable: true
			});
		}

		return promise = {
			watch: watch,
			bind: function($prop, chain, host, $watch) {
				if ($prop === prop) {
					chains.push({chain: chain, host: host, $watch: $watch || watch});
					chain = chains;
				}
				return ig.bind(model, $prop, chain, host, $watch || watch);
			},
			trigger: function(events) {
				chains.forEach(function(o) {
					o.host.$el.on(events, function(e) {
						states[prop] = true;
						model[prop] = o.host[o.chain];
						chains.forEach(function(o) {
							o.host[o.chain] = model[prop];
						});
						if (o.$watch) {
							o.$watch.call(o.host, prop, model[prop], model, e);
						}
						delete states[prop];
					});
				});
				return promise;
			}
		};
	};
})(window.top);