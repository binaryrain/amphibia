package com.equinix.builder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;

public class Mocha extends ProjectAbstract {
	
	public static final String DEFAULT_ENDPOINT = "${globals.RestEndPoint}";
	
	protected File outputFile;
	protected String packageJSON;
	protected String testsJS;

	public Mocha(CommandLine cmd) throws Exception {
		super(cmd);
	}
	
	@Override
	protected void readInputData() throws Exception {
		super.readInputData();
		packageJSON = this.getFileContent(getTemplateFile("mocha/package.json"));
		testsJS = this.getFileContent(getTemplateFile("mocha/tests.js"));
	}
	
	@Override
	protected void buildProject(String name) throws Exception {
		super.buildProject(name);
		outputFile = new File(outputDirPath, name + "-tests.js");
		packageJSON = replace(packageJSON, "<% PROJECT_NAME %>", name);
	}
	
	@Override
	protected void saveFile() throws Exception {
		super.saveFile();
		File packageFile = new File(outputDirPath, "package.json");
		packageFile.delete();
		PrintWriter writer = new PrintWriter(new FileOutputStream(packageFile, true));
		writer.println(packageJSON);
		writer.close();
		System.out.println("The package.json file saved successfully.\n" + packageFile);

		outputFile.delete();
		writer = new PrintWriter(new FileOutputStream(outputFile, true));
		writer.println(testsJS);
		writer.close();
		System.out.println("The test file saved successfully.\n" + outputFile);
	}
	
	protected void printEnd() throws Exception {
		System.out.println("\nNOTE:\n\nOpen command prompt and CD to: " + outputDirPath);
		System.out.println("\nRun following commands: \nnpm install\nnpm start");
	}
	
	@Override
	protected void buildGlobalParameters(List<?> globals) throws Exception {
		super.buildGlobalParameters(globals);
		List<String> globalConfig = new ArrayList<String>();
		List<String> globalConst = new ArrayList<String>();
		for (Object item : globals) {
			Map<?, ?> globalItem = (Map<?, ?>)item;
			String parameterJSON = "\t\t\"<% NAME %>\": \"<% VALUE %>\"";
			parameterJSON = replace(parameterJSON, "<% NAME %>", globalItem.get("name"));
			parameterJSON = replace(parameterJSON, "<% VALUE %>", globalItem.get("value"));
			globalConfig.add(parameterJSON);

			parameterJSON = "\t'<% NAME %>': process.env['npm_package_config_<% NAME %>']";
			parameterJSON = replace(parameterJSON, "<% NAME %>", globalItem.get("name"));
			globalConst.add(parameterJSON);
		}
		packageJSON = replace(packageJSON, "<% GLOBALS %>", String.join(",\n", globalConfig));
		testsJS = replace(testsJS, "<% GLOBALS %>", String.join(",\n", globalConst));
	}

	@Override
	protected void buildInterfaces(List<?> interfaces) throws Exception {
		super.buildInterfaces(interfaces);
		List<String> resourceList = new ArrayList<String>();
		for (Object item : interfaces) {
			Map<?, ?> interfaceItem = (Map<?, ?>)item;
			String headerJSON = "\t'" + interfaceItem.get("name") + "': {\n<% PARAMETERS %>\n\t}";

			List<?> resources = new ArrayList<Map<?, ?>>();
			if (interfaceItem.get("resources") != null) {
				Object obj = interfaceItem.get("resources");
				if (obj instanceof String) {
					resources = (List<?>)getJSON((String)interfaceItem.get("resources"));
				} else {
					resources = (List<?>) obj;
				}
			}
			headerJSON = buildResources(headerJSON, resources);

			resourceList.add(headerJSON);
		}
		testsJS = replace(testsJS, "<% HEADERS %>", String.join(",\n", resourceList));
	}
	
	protected String buildResources(String headerJSON,List<?> resources) throws Exception {
		List<String> parameterList = new ArrayList<String>();
		for (Object item : resources) {
			Map<?, ?> resourceItem = (Map<?, ?>)item;
			String parameterJSON = "\t\t'<% NAME %>': '<% VALUE %>'";
			parameterJSON = replace(parameterJSON, "<% NAME %>", resourceItem.get("name"));
			parameterJSON = replace(parameterJSON, "<% VALUE %>", resourceItem.get("value"));
			parameterList.add(parameterJSON);
		}
		return replace(headerJSON, "<% PARAMETERS %>", String.join(",\n", parameterList));
	}

	@Override
	protected void buildProperties(List<?> properties) throws Exception {
		super.buildProperties(properties);
		List<String> propertytList = new ArrayList<String>();
		for (Object item : properties) {
			Map<?, ?> propertyItem = (Map<?, ?>)item;
			String propertyJSON = "\t'<% NAME %>': '<% VALUE %>'";
			propertyJSON = replace(propertyJSON, "<% NAME %>", propertyItem.get("name"));
			propertyJSON = replace(propertyJSON, "<% VALUE %>", propertyItem.get("value"));
			propertytList.add(propertyJSON);
		}
		testsJS = replace(testsJS, "<% PROPERTIES %>", String.join(",\n", propertytList));
	}
	
	@Override
	protected void buildTestSuites(List<?> testsuites) throws Exception {
		super.buildTestSuites(testsuites);

		List<String> testLists = new ArrayList<String>();
		for (Object testsuite : testsuites) {
			Map<?, ?> testSuiteItem = (Map<?, ?>)testsuite;
			List<?> testcases = (List<?>)testSuiteItem.get("testcases");
			for (Object testcase : testcases) {
				String testJSON = this.getFileContent(getTemplateFile("mocha/test.js"));
				testJSON = replace(testJSON, "<% TESTSUITE_NAME %>", testSuiteItem.get("name"));
				Map<?, ?> testCaseItem = (Map<?, ?>)testcase;
				testJSON = replace(testJSON, "<% TESTCASE_NAME %>", testCaseItem.get("name"));

				List<String> testStepLists = new ArrayList<String>();
				buildTestSteps(testStepLists, (List<?>)testCaseItem.get("teststeps"));

				testJSON = replace(testJSON, "<% TESTSTEPS %>", String.join("\n\n", testStepLists));
				testLists.add(testJSON);
			}
		}
		testsJS = replace(testsJS, "<% TESTS %>", String.join("\n\n", testLists));
	}
	
	protected void buildTestSteps(List<String> testStepLists, List<?> teststeps) throws Exception {
		for (Object teststep : teststeps) {
			Map<?, ?> testStepItem = (Map<?, ?>)teststep;
			if ("restrequest".equals(testStepItem.get("type"))) {
				String testStepJSON = this.getFileContent(getTemplateFile("mocha/teststep.js"));
				testStepJSON = replace(testStepJSON, "<% TESTSTEP %>", testStepItem.get("name"));
				
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
						testStepJSON = replace(testStepJSON, "<% ENDPOINT %>", replace.get("endpoint") != null ? replace.get("endpoint") : DEFAULT_ENDPOINT);
						testStepJSON = replace(testStepJSON, "<% HEADER %>", "headers['" + replace.get("interface") + "']");
						testStepJSON = replace(testStepJSON, "<% METHOD %>", replace.get("method").toString().toLowerCase());
	
						Object body = replace.get("body");
						if (body != null) {
							body = prettyJson(body).toString().replaceAll("\\$\\{#Project#(.*?)\\}", "\\${properties.$1}");
						} else {
							body = "{}";
						}
						testStepJSON = replace(testStepJSON, "<% BODY %>", tabs(body.toString(), "\t\t"));
	
						for (Object key : replace.keySet()) {
							Object value = replace.get(key);
							testStepJSON = replace(testStepJSON, "<% " + key.toString().toUpperCase() + " %>", value instanceof String ? value : prettyJson(value));
						}
					}
					
					List<?> assertions = (List<?>)config.get("assertions");
					if (assertions != null) {
						List<String> assertionList = new ArrayList<String>();
						for (Object item : assertions) {
							Map<?, ?> assertionItem = (Map<?, ?>)item;
							String type = assertionItem.get("type").toString();
							String assertion = "";
							replace = (Map<?, ?>)assertionItem.get("replace");
							
							switch (type) {
								case "HTTPHeaderEquals":
									assertion = "assert.equal(res.header['<% NAME %>'], '<% VALUE %>');";
									break;
								case "InvalidHTTPStatusCodes":
									assertion = "assert.ok(res.statusCode !== <% VALUE %>);";
									break;
								case "ValidHTTPStatusCodes":
									assertion = "assert.ok(res.statusCode === <% VALUE %>);";
									break;
							}
							for (Object key : replace.keySet()) {
								assertion = replace(assertion, "<% " + key.toString().toUpperCase() + " %>", replace.get(key).toString());
							}
							assertionList.add(assertion);
						}
	
						testStepJSON = replace(testStepJSON, "<% ASSERTIONS %>", String.join(",\n", assertionList));
					}
				}

				testStepLists.add(testStepJSON);
			}
		}
	}
}
