'use strict';

const superagent = require('superagent'),
		assert = require('assert');

const globals = {
<% GLOBALS %>
};

const headers = {
<% HEADERS %>
};

let properties = {
<% PROPERTIES %>
};

<% TESTS %>