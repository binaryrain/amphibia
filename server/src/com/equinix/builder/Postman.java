package com.equinix.builder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;

public class Postman extends ProjectAbstract {

	public static final String DEFAULT_ENDPOINT = "{{RestEndPoint}}";
	
	protected String jsonContent;
	protected File outputFile;
	protected int folderIndex;
	protected Map<String, List<String>> interfaceMap;
	protected List<String> folderList;
	protected List<String> requestList;

	public Postman(CommandLine cmd) throws Exception {
		super(cmd);
	}
	
	@Override
	protected void readInputData() throws Exception {
		super.readInputData();
		jsonContent = this.getFileContent(getTemplateFile("postman/postman.json"));
	}
	
	@Override
	protected void buildProject(String name) throws Exception {
		super.buildProject(name);
		outputFile = new File(outputDirPath, name + "-postman.json");
		jsonContent = replace(jsonContent, "<% PROJECT_NAME %>", name);
	}
	
	@Override
	protected void saveFile() throws Exception {
		super.saveFile();
		outputFile.delete();
		PrintWriter writer = new PrintWriter(new FileOutputStream(outputFile, true));
		writer.println(jsonContent);
		writer.close();
	}
	
	@Override
	protected void buildGlobalParameters(List<?> globals) throws Exception {
		super.buildGlobalParameters(globals);
		List<String> globalList = new ArrayList<String>();
		for (Object item : globals) {
			Map<?, ?> globalItem = (Map<?, ?>)item;
			String parameterJSON = this.getFileContent(getTemplateFile("postman/property.json"));
			parameterJSON = replace(parameterJSON, "<% NAME %>", globalItem.get("name"));
			parameterJSON = replace(parameterJSON, "<% VALUE %>", globalItem.get("value"));
			globalList.add(tabs(parameterJSON, "\t\t"));
		}
		jsonContent = replace(jsonContent, "<% GLOBALS %>", String.join(",\n", globalList));
	}
	
	@Override
	protected void buildInterfaces(List<?> interfaces) throws Exception {
		super.buildInterfaces(interfaces);
		List<String> resourceList = new ArrayList<String>();
		interfaceMap = new HashMap<String, List<String>>();
		for (Object item : interfaces) {
			Map<?, ?> interfaceItem = (Map<?, ?>)item;
			String headerJSON = this.getFileContent(getTemplateFile("postman/header.json"));
			headerJSON = replace(headerJSON, "<% ID %>", "00000000-0000-" + String.format("%04d", (resourceList.size() + 1)) + "-0000-000000000000");
			headerJSON = replace(headerJSON, "<% NAME %>", interfaceItem.get("name"));
			headerJSON = replace(headerJSON, "<% TIMESTAMP %>", String.valueOf(new Date().getTime()));

			List<?> resources = new ArrayList<Map<?, ?>>();
			if (interfaceItem.get("resources") != null) {
				Object obj = interfaceItem.get("resources");
				if (obj instanceof String) {
					resources = (List<?>)getJSON((String)interfaceItem.get("resources"));
				} else {
					resources = (List<?>) obj;
				}
			}
			List<String> headersList = new ArrayList<String>();
			headerJSON = buildResources(headersList, headerJSON, resources);

			interfaceMap.put(interfaceItem.get("name").toString(), headersList);
			resourceList.add(headerJSON);
		}
		jsonContent = replace(jsonContent, "<% HEADERS %>", String.join(",\n", resourceList));
	}
	
	protected String buildResources(List<String> headersList, String headerJSON, List<?> resources) throws Exception {
		List<String> parameterList = new ArrayList<String>();
		for (Object item : resources) {
			buildResourceParameters(headersList, parameterList, (Map<?, ?>)item);
		}
		return replace(headerJSON, "<% PARAMETERS %>", String.join(",\n", parameterList));
	}
	
	protected void buildResourceParameters(List<String> headersList, List<String> parameterList, Map<?, ?> parameter) throws Exception {
		String parameterJSON = this.getFileContent(getTemplateFile("postman/property.json"));
		parameterJSON = replace(parameterJSON, "<% NAME %>", parameter.get("name"));
		parameterJSON = replace(parameterJSON, "<% VALUE %>", parameter.get("value"));
		headersList.add(parameter.get("name") + ": " + parameter.get("value"));
		parameterList.add(tabs(parameterJSON, "\t\t\t\t"));
	}

	@Override
	protected void buildProperties(List<?> properties) throws Exception {
		super.buildProperties(properties);
		List<String> propertytList = new ArrayList<String>();
		for (Object item : properties) {
			Map<?, ?> propertyItem = (Map<?, ?>)item;
			String propertyJSON = this.getFileContent(getTemplateFile("postman/property.json"));
			propertyJSON = replace(propertyJSON, "<% NAME %>", propertyItem.get("name"));
			propertyJSON = replace(propertyJSON, "<% VALUE %>", propertyItem.get("value"));
			propertytList.add(tabs(propertyJSON, "\t\t\t\t"));
		}
		jsonContent = replace(jsonContent, "<% PARAMETERS %>", String.join(",\n", propertytList));
	}
	
	@Override
	protected void buildTestSuites(List<?> testsuites) throws Exception {
		super.buildTestSuites(testsuites);
		folderIndex = 1;
		folderList = new ArrayList<String>();
		requestList = new ArrayList<String>();

		List<String> testSuiteIds = new ArrayList<String>();
		for (Object item : testsuites) {
			Map<?, ?> testSuiteItem = (Map<?, ?>)item;
			int index = folderList.size();
			String testSuiteJSON = this.getFileContent(getTemplateFile("postman/folder.json"));
			testSuiteJSON = replace(testSuiteJSON, "<% NAME %>", testSuiteItem.get("name"));
			testSuiteJSON = replace(testSuiteJSON, "<% ID %>", getId(folderList, testSuiteIds));
			testSuiteJSON = replace(testSuiteJSON, "<% REQUEST_IDS %>", "");

			List<String> testCaseIds = new ArrayList<String>();
			buildTestCases(folderList, testCaseIds, (List<?>)testSuiteItem.get("testcases"));

			testSuiteJSON = replace(testSuiteJSON, "<% FOLDER_IDS %>", String.join(",\n", testCaseIds));
			folderList.add(index, testSuiteJSON);
		}
		jsonContent = replace(jsonContent, "<% TESTSUITE_IDS %>", String.join(",\n", testSuiteIds));
		jsonContent = replace(jsonContent, "<% FOLDERS %>", String.join(",\n", folderList));
		jsonContent = replace(jsonContent, "<% REQUESTS %>", String.join(",\n", requestList));
	}
	
	protected void buildTestCases(List<String> folderList, List<String> testCaseIds, List<?> testcases) throws Exception {
		for (Object item : testcases) {
			Map<?, ?> testCaseItem = (Map<?, ?>)item;
			int index = folderList.size();
			String testCaseJSON = this.getFileContent(getTemplateFile("postman/folder.json"));
			testCaseJSON = replace(testCaseJSON, "<% NAME %>", testCaseItem.get("name"));
			testCaseJSON = replace(testCaseJSON, "<% ID %>", getId(folderList, testCaseIds));
			testCaseJSON = replace(testCaseJSON, "<% FOLDER_IDS %>", "");

			List<String> testStepIds = new ArrayList<String>();
			buildTestSteps(testStepIds, (List<?>)testCaseItem.get("teststeps"));

			testCaseJSON = replace(testCaseJSON, "<% REQUEST_IDS %>", String.join(",\n", testStepIds));
			folderList.add(index, testCaseJSON);
		}
	}
	
	protected void buildTestSteps(List<String> testStepIds, List<?> teststeps) throws Exception {
		for (Object item : teststeps) {
			Map<?, ?> testStepItem = (Map<?, ?>)item;
			if ("restrequest".equals(testStepItem.get("type"))) {
				String testStepJSON = this.getFileContent(getTemplateFile("postman/request.json"));
				testStepJSON = replace(testStepJSON, "<% NAME %>", testStepItem.get("name"));
				testStepJSON = replace(testStepJSON, "<% ID %>", getId(folderList, testStepIds));

				if (testStepItem.get("config") != null) {
					Object obj = testStepItem.get("config");
					Map<?, ?> config = null;
					if (obj instanceof String) {
						config = (Map<?, ?>)getJSON((String)obj);
					} else {
						config = (Map<?, ?>)obj;
					}
					Map<?, ?> replace = (Map<?, ?>)config.get("replace");
					if (replace != null) {
						testStepJSON = replace(testStepJSON, "<% HEADERS %>", String.join("\\n", interfaceMap.get(replace.get("interface"))));
						testStepJSON = replace(testStepJSON, "<% ENDPOINT %>", replace.get("endpoint") != null ? replace.get("endpoint") : DEFAULT_ENDPOINT);
	
						Object body = replace.get("body");
						if (body != null) {
							body = prettyJson(body).replaceAll("\\n", "\\\\n").replaceAll("\\t", "\\\\t").replaceAll("\\\"", "\\\\\"");
							body = body.toString().replaceAll("\\$\\{#Project#(.*?)\\}", "{{$1}}");
							testStepJSON = replace(testStepJSON, "<% BODY %>", body);
						}
	
						for (Object key : replace.keySet()) {
							Object value = replace.get(key);
							testStepJSON = replace(testStepJSON, "<% " + key.toString().toUpperCase() + " %>", value instanceof String ? value : prettyJson(value));
						}
					}
				}
				requestList.add(testStepJSON);
			}
		}
	}
	
	private String getId(List<String> folderList, List<String> collection) {
		String id = "00000000-" + String.format("%04d", folderIndex++) + "-0000-0000-000000000000";
		collection.add(tabs('"' + id + '"', "\t\t\t\t\t\t"));
		return id;
	}
	
	protected void printEnd() throws Exception {
		System.out.println("The project file saved successfully.");
		System.out.println("\nNOTE:\n\n1) Open SETTINGS window\n2) Click Data tab\n3) Click the Choose Files button and navigate to: " + outputFile);
		System.out.println("\n\nSelect a new Environment");
	}
}
