'use strict';

const indigo = global.__indigo;

module.exports = function(router) {

	router.get('/:locale/index', (req, res) => {
		indigo.render(req, res, '/index');
	});

	router.post('/save/step1', (req, res) => {
		console.log(req.body);
		res.json({file: 'ads'});
	});

	router.post('/file/upload', function(req, res) {
		res.json({id: Date.now()});
	});
};