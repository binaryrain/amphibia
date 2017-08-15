package com.equinix.builder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Map;

import com.google.common.base.Splitter;

import org.apache.commons.cli.CommandLine;

public class Swagger extends ProjectAbstract {

	protected File outputFile;
	protected String swaggerJSON;
	protected List<String> parameterList;
	protected List<String> definitionList;
	protected Map<Object, Object> propertyMap;
	protected Map<String, List<String>> pathMap;
	protected Map<String, List<String>> interfaceMap;
	protected Map<?, ?> globalItem;
	
	private static final Pattern projectRegExp = Pattern.compile("\\$\\{#Project#(.*?)\\}");

	public Swagger(CommandLine cmd) throws Exception {
		super(cmd);
	}
	
	@Override
	protected void init() throws Exception {
		parameterList = new ArrayList<String>();
		definitionList = new ArrayList<String>();
		propertyMap = new TreeMap<Object, Object>();
		pathMap = new TreeMap<String, List<String>>();
		interfaceMap = new TreeMap<String, List<String>>();
		super.init();
	}
	
	@Override
	protected void readInputData() throws Exception {
		super.readInputData();
		swaggerJSON = this.getFileContent(getTemplateFile("swagger/swagger.json"));
	}
	
	@Override
	protected void saveFile() throws Exception {
		super.saveFile();
		outputFile.delete();
		List<String> paths = new ArrayList<String>();
		for (String path : pathMap.keySet()) {
			paths.add("\t\t\"" + (path.charAt(0) != '/' ? '/' + path : path) + "\": {\n" +String.join(",\n", pathMap.get(path)) + "\n\t\t}");
		}
		swaggerJSON = replace(swaggerJSON, "<% PATHS %>", String.join(",\n", paths));
		swaggerJSON = replace(swaggerJSON, "<% PARAMETERS %>", String.join(",\n", parameterList));
		swaggerJSON = replace(swaggerJSON, "<% DEFINITIONS %>", String.join(",\n", definitionList));

		PrintWriter writer = new PrintWriter(new FileOutputStream(outputFile, true));
		writer.println(swaggerJSON);
		writer.close();
	}
	
	protected void printEnd() throws Exception {
		System.out.println("The test file saved successfully.");
		System.out.println("\nNOTE:\n\nOpen http://editor2.swagger.io in browser and copy & paste JSON from: " + outputFile);
	}

	@Override
	protected void buildProject(String name) throws Exception {
		super.buildProject(name);
		outputFile = new File(outputDirPath, name + "-swagger.json");
		swaggerJSON = replace(swaggerJSON, "<% PROJECT_NAME %>", name);
	}
	
	@Override
	protected void buildGlobalParameters(List<?> globals) throws Exception {
		super.buildGlobalParameters(globals);
		for (Object item : globals) {
			globalItem = (Map<?, ?>)item;
			if ("RestEndPoint".equals(globalItem.get("name"))) {
				String[] pair = globalItem.get("value").toString().split(":");
				swaggerJSON = replace(swaggerJSON, "<% SCHEME %>", pair[0]);
				swaggerJSON = replace(swaggerJSON, "<% ENDPOINT %>", pair[1].substring(2));
			}
		}
	}
	
	@Override
	protected void buildProperties(List<?> properties) throws Exception {
		super.buildProperties(properties);
		for (Object item : properties) {
			Map<?, ?> propertyItem = (Map<?, ?>)item;
			propertyMap.put(propertyItem.get("name"), propertyItem.get("value"));
		}
	}

	@Override
	protected void buildInterfaces(List<?> interfaces) throws Exception {
		super.buildInterfaces(interfaces);
		for (Object item : interfaces) {
			Map<?, ?> interfaceItem = (Map<?, ?>)item;

			List<?> resources;
			Object obj = interfaceItem.get("resources");
			if (obj instanceof String) {
				resources = (List<?>)getJSON((String)interfaceItem.get("resources"));
			} else {
				resources = (List<?>) obj;
			}
			List<String> resourceList = new ArrayList<String>();
			buildResources(resourceList, resources);

			interfaceMap.put(interfaceItem.get("name").toString(), resourceList);
		}
	}
	
	protected void buildResources(List<String> resourceList, List<?> resources) throws Exception {
		for (Object item : resources) {
			Map<?, ?> resourceItem = (Map<?, ?>)item;
			String parameterJSON = this.getFileContent(getTemplateFile("swagger/parameter.json"));
			String style = (String) resourceItem.get("style");
			String param = "param" + (parameterList.size() + 1);
			parameterJSON = replace(parameterJSON, "<% PARAM %>", '"' + param + "\": ");
			parameterJSON = replace(parameterJSON, "<% IN %>", style == null ? "header" : style.toLowerCase());
			parameterJSON = replace(parameterJSON, "<% NAME %>", resourceItem.get("name"));
			parameterJSON = replace(parameterJSON, "<% VALUE %>", resourceItem.get("value"));
			resourceList.add(this.getFileContent(getTemplateFile("swagger/ref.json")).replace("<% PARAM %>", param));
			parameterList.add(parameterJSON);
		}
	}
	
	@Override
	protected void buildTestSuites(List<?> testsuites) throws Exception {
		super.buildTestSuites(testsuites);
		for (Object testsuite : testsuites) {
			Map<?, ?> testSuiteItem = (Map<?, ?>)testsuite;
			for (Object testcase : (List<?>)testSuiteItem.get("testcases")) {
				Map<?, ?> testCaseItem = (Map<?, ?>)testcase;
				for (Object teststep : (List<?>)testCaseItem.get("teststeps")) {
					Map<?, ?> testStepItem = (Map<?, ?>)teststep;
					if ("restrequest".equals(testStepItem.get("type"))) {
						String testStepJSON = this.getFileContent(getTemplateFile("swagger/path.json"));
						Map<?, ?> config;
						Object obj = testStepItem.get("config");
						if (obj instanceof String) {
							config = (Map<?, ?>)getJSON((String) obj);
						} else {
							config = (Map<?, ?>) obj;
						}

						List<String> assertionList = new ArrayList<String>();
						List<?> assertions = (List<?>)config.get("assertions");
						for (Object item : assertions) {
							Map<?, ?> assertionItem = (Map<?, ?>)item;
							String assertionJSON = this.getFileContent(getTemplateFile("swagger/assertion.json"));
							assertionJSON = replace(assertionJSON, "<% NAME %>", assertionItem.get("type"));
							assertionJSON = replace(assertionJSON, "<% VALUE %>", ((Map<?, ?>)assertionItem.get("replace")).get("value"));
							assertionList.add(assertionJSON);
						}
						testStepJSON = replace(testStepJSON, "<% ASSERTIONS %>", String.join(",\n", assertionList));


						Map<?, ?> replace = (Map<?, ?>)config.get("replace");
						if (replace != null) {
							URI uri = new URI(replace.get("path").toString());
							List<String> resourceList = new ArrayList<String>();
							resourceList.addAll(interfaceMap.get(replace.get("interface")));
							if (uri.getQuery() != null) {
								Map<String, String> queries = Splitter.on('&').trimResults().withKeyValueSeparator("=").split(uri.getQuery());
								for (String name : queries.keySet()) {
									String parameterJSON = this.getFileContent(getTemplateFile("swagger/parameter.json"));
									parameterJSON = replace(parameterJSON, "<% PARAM %>", "");
									parameterJSON = replace(parameterJSON, "<% IN %>", "query");
									parameterJSON = replace(parameterJSON, "<% NAME %>", name);
									parameterJSON = replace(parameterJSON, "<% VALUE %>", queries.get(name));
									resourceList.add(tabs(parameterJSON, "\t\t\t"));
								}
							}

							Object body = replace.get("body");
							if (body != null) {
								String defName = "def" + (definitionList.size() + 1);
								String bodyJSON = this.getFileContent(getTemplateFile("swagger/body.json"));
								bodyJSON = replace(bodyJSON, "<% DEFINITION %>", defName);
								resourceList.add(bodyJSON);

								StringBuffer jsonBody = new StringBuffer();
								Matcher matcher = projectRegExp.matcher(prettyJson(body));
								while (matcher.find()) {
									String value = (String) propertyMap.get(matcher.group(1));
									matcher.appendReplacement(jsonBody, value != null ? value : "");
								}
								matcher.appendTail(jsonBody);

								String definitionJSON = this.getFileContent(getTemplateFile("swagger/definition.json"));
								definitionJSON = replace(definitionJSON, "<% NAME %>", defName);
								definitionJSON = replace(definitionJSON, "<% EXAMPLE %>", jsonBody.toString());
								definitionList.add(definitionJSON);
							}

							testStepJSON = replace(testStepJSON, "<% METHOD %>", replace.get("method").toString().toLowerCase());
							testStepJSON = replace(testStepJSON, "<% DESCRIPTION %>", testSuiteItem.get("name") + "." + testCaseItem.get("name") + ", " + testStepItem.get("name"));
							testStepJSON = replace(testStepJSON, "<% TAG %>", testCaseItem.get("name"));
							testStepJSON = replace(testStepJSON, "<% MEDIATYPE %>", replace.get("mediaType"));
							testStepJSON = replace(testStepJSON, "<% PARAMETERS %>", String.join(",\n", resourceList));

							List<String> items = pathMap.get(uri.getPath());
							if (items == null) {
								items = new ArrayList<String>();
								items.add(testStepJSON);
							}
							pathMap.put(uri.getPath(), items);
						}
					}
				}
			}
		}
	}
}
