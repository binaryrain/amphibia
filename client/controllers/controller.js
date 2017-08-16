'use strict';

const indigo = global.__indigo,
	rest = indigo.libs('rest'),
	path = require('path'),
	exec = require('child_process').exec,
	formidable = require('formidable'),
	buildDir = path.join(__appDir, '/build'),
	url = require('url'),
	fs = require('fs');

module.exports = function(router) {

	if (!fs.existsSync(buildDir)) {
		fs.mkdirSync(buildDir);
	}

	router.get('/:locale/index', (req, res) => {
		indigo.render(req, res, '/index');
	});

	router.post('/save/step1', (req, res) => {
		console.log(req.body);
		res.json({file: 'ads'});
	});

	router.post('/preview', function(req, res) {
		if (req.body.path.startsWith('http')) {
			let reqData = url.parse(req.body.path),
				config = {
					host: reqData.hostname,
					port: reqData.port || 80,
					secure: reqData.protocol === 'https'
				};
				rest().init(config).get(function(err, data) {
					if (err) {
						res.status(400).json({error: err});
					} else {
						res.json(data);
					}
				}, reqData.path);
		} else {
			fs.readFile(path.join(buildDir, req.body.path), function read(err, data) {
				if (err) {
					res.status(400).json({error: err});
				} else {
					res.send(data);
				}
			});
		}
	});

	router.post('/file/upload', function(req, res) {
		let fileName, form = new formidable.IncomingForm();
		form.multiples = true;
		form.buildDir = buildDir;
		form.on('file', function(field, file) {
			fileName = new Date().toISOString().substring(0, 10) + '_' + file.name;
			fs.rename(file.path, path.join(buildDir, fileName));
		});
		form.on('error', function(err) {
			res.status(400).json({error: err});
		});
		form.on('end', function() {
			res.json({file: fileName});
		});
		form.parse(req);
	});

	router.post('/compile', function(req, res) {
		let args = ['-l=true'],
			cmd;
		for (var key in req.body) {
			args.push(`${key}=${req.body[key]}`);
		}
		cmd = `java -cp ../../server/ext/amphibia.jar com.equinix.converter.Converter ${args.join(' ')}`;
		console.log(cmd);
		exec(cmd, {cwd: buildDir}, (error, stdout, stderr) => {
			if(error !== null) {
				console.error('exec error: ' + error);
				res.status(400).send(stderr);
			} else {
				console.log('stdout: \n' + stdout);
				res.send(stdout);
			}
		});
	});

	router.post('/export', function(req, res) {
		let args = [],
			cmd;
		for (var key in req.body) {
			args.push(`${key}=${req.body[key]}`);
		}
		cmd = `java -cp ../../server/ext/amphibia.jar com.equinix.builder.Builder ${args.join(' ')}`;
		console.log(cmd);
		exec(cmd, {cwd: buildDir}, (error, stdout, stderr) => {
			if(error !== null) {
				console.log('stdout: \n' + stdout);
				console.error('exec error: ' + error);
				res.status(400).send(stderr);
			} else {
				console.log('stdout: \n' + stdout);
				res.send(stdout);
			}
		});
	});
};