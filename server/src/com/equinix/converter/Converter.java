package com.equinix.converter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;

import com.equinix.runner.AbstractScript;

public class Converter {

	public static final String NAME = "name";
	public static final String INPUT = "input";
	public static final String PROPERTIES = "properties";
	public static final String SCHEMA = "schema";
	public static final String TESTS = "tests";

	public static CommandLine cmd;

	public static void main(String[] args) throws Exception {
		Options options = new Options();
		options.addOption(new Option("n", NAME, true, "Project name"));
		options.addOption(new Option("p", PROPERTIES, true, "Properties file"));
		options.addOption(new Option("s", SCHEMA, true, "generate JSON schemas. Default: true"));
		options.addOption(new Option("t", TESTS, true, "generate JSON tests. Default: true"));

		Option input = new Option("i", INPUT, true, "Swagger file or URL");
		input.setRequired(true);
		options.addOption(input);

		CommandLineParser parser = new GnuParser();
		HelpFormatter formatter = new HelpFormatter();

		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			printHelp(formatter, options);
			System.exit(1);
			return;
		}

		File outputDir = new File(new File(AbstractScript.PROJECT_DIR).getAbsolutePath());
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}
		
		String inputFile = cmd.getOptionValue(INPUT);
		InputStream is = null;
		if (inputFile.startsWith("http")) {
			is = new URL(inputFile).openStream();
		} else {
			is = new FileInputStream(inputFile);
		}
		try {
			new Swagger(cmd, is, outputDir);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(is);
		}
	}

	public static void printHelp(HelpFormatter formatter, Options options) {
		formatter.printHelp("Converter", options);
	}
}
