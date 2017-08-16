package com.equinix.builder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.Charsets;

import com.equinix.runner.AbstractScript;
import com.google.common.io.Resources;

import groovy.json.JsonSlurper;
import net.sf.json.JSONObject;

public abstract class ProjectAbstract {

	protected CommandLine cmd;
	protected Map<?, ?> inputJsonProject;

	protected String inputFilePath;
	protected String outputDirPath;

	private final ClassLoader classLoader = getClass().getClassLoader();

	public ProjectAbstract(CommandLine cmd) throws Exception {
		this.cmd = cmd;
		inputFilePath = cmd.getOptionValue(Builder.INPUT);
		outputDirPath = new File(AbstractScript.PROJECT_DIR).getAbsolutePath();

		File outputDir = new File(outputDirPath);
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}
		init();
	}

	protected void init() throws Exception {
		readInputData();
		parseInputProjectFile();
		saveFile();
		printEnd();
	}
	
	protected String tabs(String source, String tabs) {
		return source.replaceAll("^", tabs).replaceAll("\n", "\n" + tabs);
	}
	
	protected String prettyJson(Object value) throws Exception {
		return prettyJson(JSONObject.fromObject(value).toString());
	}

	protected String prettyJson(String value) throws Exception {
		return prettyJson(value, "\t");
	}

	protected String prettyJson(String value, String tabs) throws Exception {
		ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("JavaScript");
		scriptEngine.put("jsonString", value);
		scriptEngine.eval("result = JSON.stringify(JSON.parse(jsonString), null, 4)");
		return ((String)scriptEngine.get("result")).replaceAll(" {4}", tabs);
	}
	
	protected Object getJSON(String path) throws IOException {
		return getJSON(new File(path));
	}

	protected Object getJSON(File file) throws IOException {
		JsonSlurper json = new JsonSlurper();
		return json.parse(new InputStreamReader(new FileInputStream(file)));
	}

	protected String getFileContent(String file) throws IOException {
		return getFileContent(new File(file).toURI());
	}

	protected String getFileContent(URI uri) throws IOException {
		return Resources.toString(uri.toURL(), Charsets.UTF_8);
	}

	protected URI getTemplateFile(String path) throws Exception {
		URL url = classLoader.getResource("com/equinix/templates/" + path);
		if (url != null) {
			return url.toURI();
		} else {
			File file = new File("builder/templates/" + path);
			if (!file.exists()) {
				throw new FileNotFoundException("The source XML file not found: " + file.getAbsolutePath());
			}
			return file.toURI();
		}
	}

	protected String replace(String source, Object target, Object replacement) {
		return source.replace(String.valueOf(target), String.valueOf(replacement));
	}

	protected void readInputData() throws Exception {
		File inputFile = new File(inputFilePath);
		if (!inputFile.exists()) {
			throw new FileNotFoundException("The JSON input project file not found: " + inputFile.getAbsolutePath());
		}
		inputJsonProject = (Map<?, ?>) getJSON(inputFile);
	}

	protected void parseInputProjectFile() throws Exception {
		for (Object key : inputJsonProject.keySet()) {
			Object value = inputJsonProject.get(key);
			if ("name".equals(key)) {
				buildProject((String) value);
			} else if ("globals".equals(key)) {
				buildGlobalParameters((List<?>) value);
			} else if ("interfaces".equals(key)) {
				buildInterfaces((List<?>) value);
			} else if ("properties".equals(key)) {
				buildProperties((List<?>) value);
			} else if ("testsuites".equals(key)) {
				buildTestSuites((List<?>) value);
			} else {
				System.err.println("Unexpected project key: " + key);
			}
		}
	}

	protected void buildProject(String name) throws Exception {
	}

	protected void buildGlobalParameters(List<?> globals) throws Exception {
	}

	protected void buildInterfaces(List<?> interfaces) throws Exception {
	}

	protected void buildProperties(List<?> properties) throws Exception {
	}

	protected void buildTestSuites(List<?> testsuites) throws Exception {
	}

	protected void saveFile() throws Exception {
	}
	
	protected void printEnd() throws Exception {
	}
}
