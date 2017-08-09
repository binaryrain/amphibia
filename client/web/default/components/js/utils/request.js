'use strict';

define([], function(win_top) {

	/* istanbul ignore next */ 
	var top = win_top || window.top,
		defOps = {
		contentType: 'application/json',
		overlay: function() {},
		alert: function() {}
	};

	var _ = function(contextPath, options) {
		options = options || {};
		options.contextPath = contextPath;

		for (var key in defOps) {
			if (options[key] === undefined) {
				options[key] = defOps[key];
			}
		}

		var request = function(callback, path, data, type, skipOverlay, contentType) {
			options.overlay(_.OVERLAY_SHOW, skipOverlay);

			return top.$.ajax({
				type: type || 'GET',
				url: options.contextPath + path,
				data: data ? contentType === undefined ? JSON.stringify(data) : data : null,
				contentType: contentType === undefined ? options.contentType : contentType,
				async: true,
				cache: false,
				processData: false,
				success: function(data, status, jqXHR) {
					options.overlay(_.OVERLAY_HIDE, skipOverlay);
					callback(null, data, status, jqXHR);
				},
				error: function(jqXHR) {
					options.overlay(_.OVERLAY_HIDE, skipOverlay);
					if (jqXHR.status === 302) {
						top.location.reload(true);
					} else if (jqXHR.status === 0 || jqXHR.status === 404) { //abort
						options.alert(jqXHR.responseJSON || jqXHR.responseText);
					} else {
						callback(jqXHR.responseJSON || jqXHR.responseText, null, jqXHR);
					}
				}
			});
		};

		return {
			options: options,

			request: request,

			get: function(callback, url, skipOverlay) {
				return request(callback, url, null, 'GET', skipOverlay);
			},

			post: function(callback, url, data, skipOverlay) {
				return request(callback, url, data, 'POST', skipOverlay);
			},

			put: function(callback, url, data, skipOverlay) {
				return request(callback, url, data, 'PUT', skipOverlay);
			},

			patch: function(callback, url, data, skipOverlay) {
				return request(callback, url, data, 'PATCH', skipOverlay);
			},

			delete: function(callback, url, data, skipOverlay) {
				return request(callback, url, data, 'DELETE', skipOverlay);
			},

			file: function(callback, url, data, skipOverlay) {
				return request(callback, url, data, 'POST', skipOverlay, false);
			}
		};
	};
	_.OVERLAY_HIDE = 0;
	_.OVERLAY_SHOW = 1;

	return _;
});