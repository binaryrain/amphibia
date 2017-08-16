package com.equinix.builder;

import org.apache.commons.cli.GnuParser;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Builder {

	public static final String FORMAT = "format";
	public static final String INPUT = "input";

	public static void main(String[] args) throws Exception {
		Options options = new Options();
		options.addOption(new Option("f", FORMAT, true, "Export format: SOAP, READY, POSTMAN, MOCHA, SWAGGER. Default: SOAP"));

		Option input = new Option("i", INPUT, true, "JSON input project file");
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

		String inputFormat = cmd.getOptionValue(FORMAT);
		 if ("READY".equalsIgnoreCase(inputFormat)) {
			new ReadyAPI(cmd);
		} else if ("POSTMAN".equalsIgnoreCase(inputFormat)) {
			new Postman(cmd);
		} else if ("MOCHA".equalsIgnoreCase(inputFormat)) {
			new Mocha(cmd);
		} else if ("SWAGGER".equalsIgnoreCase(inputFormat)) {
			new Swagger(cmd);
		} else {
			new SoapUI(cmd);
		}
	}

	public static void printHelp(HelpFormatter formatter, Options options) {
		formatter.printHelp("Builder", options);
	}
}
