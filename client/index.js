'use strict';

const SOURCE_DIR = '../server',
	TARGET_DIR = 'ext',
	exec = require('child_process').exec,
	indigo = require('indigojs'),
	args = indigo.getArgs(),
	verbose = args['-v'] || args['-verbose'];

console.info('Maven dependencies...');
exec('mvn dependency:copy-dependencies', {cwd: SOURCE_DIR}, (error, stdout, stderr) => {
	if (verbose) {
		console.log('stdout: ' + stdout);
		console.log('stderr: ' + stderr);
	}
	if(error !== null) {
		console.error('exec error: ' + error);
	} else {
		let count = 0,
			callback = () => {
				count++;
				if (count === 2) {
					indigo.min_version('2.6.6')
						.start(__dirname + '/config/app.json');
				}
			};
		javaCompiler('com/equinix/converter', 'Converter', callback);
		javaCompiler('com/equinix/builder', 'Builder', callback);
	}
});

const javaCompiler = (pkg, className, callback) => {
	console.info(`Compiling ${className}...`);

	exec(`javac -d ${TARGET_DIR} -sourcepath src -classpath ${TARGET_DIR}/dependency/* src/${pkg}/${className}.java`,
			{cwd: SOURCE_DIR}, (error, stdout, stderr) => {
		if(error !== null) {
			console.error('exec error: ' + error);
		} else {
			exec(`jar cvfe ${className}.jar ${pkg.replace(/\//g, '.')}.${className} ${pkg}/*.class`,
					{cwd: `${SOURCE_DIR}/${TARGET_DIR}`}, (error, stdout, stderr) => {
				if (verbose) {
					console.log('stdout: ' + stdout);
					console.log('stderr: ' + stderr);
				}
				if(error !== null) {
					console.error('exec error: ' + error);
				} else {
					callback();
				}
			});
		}
	});
}