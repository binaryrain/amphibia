package com.equinix.converter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.equinix.runner.AbstractScript;

import net.minidev.json.JSONObject;

public class Converter {

	public static final String NAME = "name";
	public static final String INPUT = "input";
	public static final String PROPERTIES = "properties";
	public static final String SCHEMA = "schema";
	public static final String TESTS = "tests";
	public static final String JSON = "json";

	public static CommandLine cmd;
	
	private static Map<String, List<String>> results;
	private static final String CURR_DIR = new File(".").getAbsolutePath();
	
	public static final String ADD_PROJECT = "projects";
	public static final String ADD_SCHEMA = "schemas";
	public static final String ADD_TEST = "tests";

	public static void main(String[] args) throws Exception {
		Options options = new Options();
		options.addOption(new Option("n", NAME, true, "Project name"));
		options.addOption(new Option("p", PROPERTIES, true, "Properties file"));
		options.addOption(new Option("s", SCHEMA, true, "Generate JSON schemas. Default: true"));
		options.addOption(new Option("t", TESTS, true, "Generate JSON tests. Default: true"));
		options.addOption(new Option("j", JSON, true, "JSON output. Default: false"));

		Option input = new Option("i", INPUT, true, "Swagger file or URL");
		input.setRequired(true);
		options.addOption(input);

		CommandLineParser parser = new GnuParser();
		HelpFormatter formatter = new HelpFormatter();

		try {
			cmd = parser.parse(options, args);
			for (Option o : cmd.getOptions()) {
				if(o.isRequired() && o.getValue().isEmpty()) {
					throw new Error(o.getDescription() + " is empty");
				}
			}
		} catch (ParseException e) {
			printHelp(formatter, options);
			System.exit(1);
			return;
		}

		File outputDir = new File(new File(AbstractScript.PROJECT_DIR).getAbsolutePath());
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}
		
		String list = cmd.getOptionValue(JSON);
		if ("true".equals(list)) {
			results = new TreeMap<String, List<String>>();
			results.put(ADD_PROJECT, new ArrayList<String>());
			results.put(ADD_SCHEMA, new ArrayList<String>());
			results.put(ADD_TEST, new ArrayList<String>());
			Logger.getRootLogger().setLevel(Level.ERROR);
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
			if (results != null) {
				System.out.println(String.join("\n", JSONObject.toJSONString(results)));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(is);
		}
	}
	
	public static void addFile(String addType, File file) {
		if (results != null) {
			results.get(addType).add(file.getAbsolutePath().substring(CURR_DIR.length() - 1).replaceAll("\\\\", "/"));
		}
	}

	public static void printHelp(HelpFormatter formatter, Options options) {
		formatter.printHelp("Converter", options);
	}
}
