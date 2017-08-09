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

public class Converter {

	public static final String NAME = "name";
	public static final String INPUT = "input";
	public static final String OUTPUT = "output";
	
	public static final String DEFAULT_OUTPUT = new File("projects/swagger.json").getAbsolutePath();

	public static void main(String[] args) throws Exception {
		Options options = new Options();
		options.addOption(new Option("n", NAME, true, "Project name"));
		options.addOption(new Option("o", OUTPUT, true, "output project file. Default: " + DEFAULT_OUTPUT));

		Option input = new Option("i", INPUT, true, "Swagger file or URL");
		input.setRequired(true);
		options.addOption(input);

		CommandLineParser parser = new GnuParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;

		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			printHelp(formatter, options);
			System.exit(1);
			return;
		}
		
		String outputFile = cmd.getOptionValue(OUTPUT);
		if (outputFile == null) {
			outputFile = DEFAULT_OUTPUT;
		}
		
		String inputFile = cmd.getOptionValue(INPUT);
		InputStream is = null;
		if (inputFile.startsWith("http")) {
			is = new URL(inputFile).openStream();
		} else {
			is = new FileInputStream(inputFile);
		}
		try {
			new Swagger(cmd, IOUtils.toString(is), outputFile);
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
