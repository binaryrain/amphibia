'use strict';

define([
], function() {

	return function(locale, localeKey) {
		var _,
			defaultFormat,
			setLocaleString = function(locale) {
				var d = new Date(0, 0, 0), //Sun Dec 31 1899 00:00:00 GMT-0800 (Pacific Standard Time)
					days = [],
					months = [],
					months_abr = [];

				for (var i = 0; i < 7; i++) {
					d.setDate(d.getDate() + i);
					days.push(d.toLocaleString('en-us', { weekday: "long" }));
				}

				for (i = 0; i < 12; i++) {
					d.setMonth(i);
					/* istanbul ignore next */
					if (Date.toLocaleString) {
						months.push(d.toLocaleString('en-us', {month: 'long'}));
						months_abr.push(d.toLocaleString('en-us', {month: 'short'}));
					}
				}

				locale.days = locale.days || days;
				locale.months = locale.months || months;
				locale.months_abr = locale.months_abr || months_abr;
			};

		locale = locale || {
			dateFormat: 'M/d/yy',
			timeFormat: 'h:mm a',
			timeLongFormat: 'hh:mm a',
			dateTimeFormat: 'M/d/yy h:mm a', // 6/2/17 3:02 PM - new SimpleDateFormat().format(new Date())
			dateTimeLongFormat: 'MM/dd/yyyy hh:mm a',
			time_pm: 'PM',
			time_am: 'AM'
		};

		setLocaleString(locale);

		defaultFormat = locale[localeKey] || locale.dateTimeFormat;

		return _ = {
			locale: locale,

			defaultFormat: defaultFormat,

			parseDateRegExp: /(\d{4})-(\d{2})-(\d{2})\s+(\d{2}):(\d{2}):(\d{2})/, /* yyyy-MM-dd HH:mm:ss */

			parse: function(date, regExp) {
				if (parseInt(date) === Number(date)) { //parseInt("2016-01-01") !== Number("2016-01-01")
					date = new Date(parseInt(date));
				} else if (typeof date === 'string') {
					var match = date.match(regExp || _.parseDateRegExp);
					if (match) {
						date = new Date(match[1], match[2] - 1, match[3] || 1, match[4] || 0, match[5] || 0, match[6] || 0);
					}
				}
				return date instanceof Date ? date : null;
			},

			format: function(date, localeKeyOrFormat) {
				var values = [], hours = (date.getHours() + 11) % 12 + 1,
					format = locale[localeKeyOrFormat] || localeKeyOrFormat || defaultFormat,
					replace = function(regExp, val) {
						format = format.replace(regExp, '{' + values.length + '}');
						values.push(val);
					};

				replace(/E{4}/i, locale.days[date.getDay()]); //Tuesday - new SimpleDateFormat("EEEE").format(new Date())
				replace(/E{1,3}/i, (locale.days[date.getDay()] || '').substring(0, 3)); //Tue - new SimpleDateFormat("EEE").format(new Date())
				replace(/d{2}/i, _.pad(date.getDate())); //02 - new SimpleDateFormat("dd").format(new Date())
				replace(/d/i, date.getDate()); //2 - new SimpleDateFormat("d").format(new Date())
				replace(/M{4}/, locale.months[date.getMonth()]); //January - new SimpleDateFormat("MMMM").format(new Date())
				replace(/M{3}/, locale.months_abr[date.getMonth()]); //Jan - new SimpleDateFormat("MMM").format(new Date());
				replace(/M{2}/, _.pad(date.getMonth() + 1)); //01 - new SimpleDateFormat("MM").format(new Date())
				replace('M', date.getMonth() + 1); //1 - new SimpleDateFormat("M").format(new Date())
				replace(/y{4}/i, date.getFullYear()); //2016 - new SimpleDateFormat("yyyy").format(new Date())
				replace(/y{3}/i, date.getFullYear()); //2016 - new SimpleDateFormat("yyy").format(new Date())
				replace(/y{2}/i, String(date.getFullYear()).substring(2)); //16 - new SimpleDateFormat("yy").format(new Date())
				replace(/y{1}/i, date.getFullYear()); //2016 - new SimpleDateFormat("y").format(new Date())

				replace(/h{2}/, _.pad(hours)); //02 (14:00)- new SimpleDateFormat("hh").format(new Date())
				replace('h', hours); //2 (14:00) - new SimpleDateFormat("h").format(new Date())
				replace(/H{2}/, _.pad(date.getHours())); //02 - (02:00), 14 - (14:00) - new SimpleDateFormat("HH").format(new Date())
				replace(/H/, date.getHours()); //2 - (02:00), 14 - (14:00) - new SimpleDateFormat("HH").format(new Date())
				replace(/m{2}/, _.pad(date.getMinutes())); //08 - (02:08) - new SimpleDateFormat("mm").format(new Date())
				replace('m', date.getMinutes()); //8 - (02:08) - new SimpleDateFormat("m").format(new Date())
				replace(/s{2}/i, _.pad(date.getSeconds())); //05 - (02:08:05) - new SimpleDateFormat("ss").format(new Date())
				replace(/s/i, date.getSeconds()); //5 - (02:08:05) - new SimpleDateFormat("ss").format(new Date())
				replace(/a/i, date.getHours() >= 12 ? locale.time_pm : locale.time_am); //new SimpleDateFormat("a").format(new Date())

				values.forEach(function(val, index) {
					format = format.replace('{' + index + '}', val);
				});

				return format;
			},

			pad: function(val) {
				return val > 9 ? val : '0' + val;
			}
		};
	};
});