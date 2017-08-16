package com.equinix.builder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;

public class SoapUI extends ProjectAbstract {

	public static final String VERSION = "5.3.0";
	public static final String DEFAULT_ENDPOINT = "${#Global#RestEndPoint}";

	protected String xmlContent;
	protected File outputFile;
	protected Map<String, String> resourceMap;
	protected List<?> globals;
	
	public SoapUI(CommandLine cmd) throws Exception {
		super(cmd);
	}

	@Override
	protected void init() throws Exception {
		resourceMap = new HashMap<String, String>();
		resourceMap.put("name", "PATH");
		resourceMap.put("value", "PATH");
		resourceMap.put("style", "TEMPLATE");
		super.init();
	}
	
	@Override
	protected void readInputData() throws Exception {
		super.readInputData();
		xmlContent = this.getFileContent(getTemplateFile("soapui/soapui.xml"));
	}
	
	@Override
	protected void buildProject(String name) throws Exception {
		super.buildProject(name);
		outputFile = new File(outputDirPath, name + "-soap-" + VERSION + ".xml");
		xmlContent = replace(xmlContent, "<% VERSION %>", VERSION);
		xmlContent = replace(xmlContent, "<% PROJECT_NAME %>", name);
		File dir = new File(outputDirPath, "src/scripts");
		if (!dir.exists()) {
			dir.mkdirs(); //WARN  [SoapUIProGroovyScriptEngineFactory] Missing scripts folder [soapui\projects\src]
		}
	}
	
	@Override
	protected void saveFile() throws Exception {
		super.saveFile();
		outputFile.delete();
		PrintWriter writer = new PrintWriter(new FileOutputStream(outputFile, true));
		writer.println(xmlContent);
		writer.close();
	}

	@Override
	protected void printEnd() {
		System.out.println("The project file saved successfully.\n" + outputFile);
		if (globals != null && globals.size() > 0) {
			System.out.println("\nNOTE:\n\nTo add or update Global Properties, go to File -> Preferences -> Global Properties");
			System.out.println("__________________________________________________________________________________\n");
			for (Object item : globals) {
				Map<?, ?> globalItem = (Map<?, ?>)item;
				System.out.println(globalItem.get("name") + "\t\t\t" + globalItem.get("value"));
			}
		}
	}
	
	@Override
	protected void buildGlobalParameters(List<?> globals) throws Exception {
		this.globals = globals;
	}
	
	@Override
	protected void buildInterfaces(List<?> interfaces) throws Exception {
		super.buildInterfaces(interfaces);
		List<String> interfaceList = new ArrayList<String>();
		for (Object item : interfaces) {
			Map<?, ?> interfaceItem = (Map<?, ?>)item;
			List<String> resourceList = new ArrayList<String>();
			String interfaceXML = this.getFileContent(getTemplateFile("soapui/interface.xml"));
			xmlContent = replace(xmlContent, "<% INTERFACE_NAME %>", interfaceItem.get("name"));
			interfaceXML = replace(interfaceXML, "<% INTERFACE_NAME %>", interfaceItem.get("name"));
			interfaceXML = replace(interfaceXML, "<% INTERFACE_TYPE %>", interfaceItem.get("type"));
			interfaceXML = replace(interfaceXML, "<% ENDPOINT %>", DEFAULT_ENDPOINT);

			List<?> resources = new ArrayList<Map<?, ?>>();
			if (interfaceItem.get("resources") != null) {
				Object obj = interfaceItem.get("resources");
				if (obj instanceof String) {
					resources = (List<?>)getJSON((String)interfaceItem.get("resources"));
				} else {
					resources = (List<?>) obj;
				}
			}
			buildResources(resourceList, resources);

			interfaceXML = replace(interfaceXML, "<% RESOURCES %>", String.join("\n", resourceList));
			interfaceList.add(interfaceXML);
		}
		xmlContent = replace(xmlContent, "<% INTERFACES %>", String.join("\n", interfaceList));
	}

	protected void buildResources(List<String> resourceList, List<?> resources) throws Exception {
		String resourcesXML = this.getFileContent(getTemplateFile("soapui/resource.xml"));
		List<String> parameterList = new ArrayList<String>();
		buildResourceParameters(parameterList, resourceMap);
		for (Object item : resources) {
			buildResourceParameters(parameterList, (Map<?, ?>)(Map<?, ?>)item);
		}
		resourcesXML = replace(resourcesXML, "<% PARAMETERS %>", String.join("\n", parameterList));
		resourceList.add(resourcesXML);
	}

	protected void buildResourceParameters(List<String> parameterList, Map<?, ?> parameters) throws Exception {
		String parameterXML = this.getFileContent(getTemplateFile("soapui/resource_parameters.xml"));
		String style = (String) parameters.get("style");
		parameterXML = replace(parameterXML, "<% STYLE %>", style == null ? "HEADER" : style);
		parameterXML = replace(parameterXML, "<% NAME %>", parameters.get("name"));
		parameterXML = replace(parameterXML, "<% VALUE %>", parameters.get("value"));
		parameterList.add(parameterXML);
	}
	
	@Override
	protected void buildProperties(List<?> properties) throws Exception {
		super.buildProperties(properties);
		List<String> propertyList = new ArrayList<String>();
		for (Object item : properties) {
			Map<?, ?> propertyItem = (Map<?, ?>)item;
			String propertyXML = this.getFileContent(getTemplateFile("soapui/property.xml"));
			propertyXML = replace(propertyXML, "<% NAME %>", propertyItem.get("name"));
			propertyXML = replace(propertyXML, "<% VALUE %>", propertyItem.get("value"));
			propertyList.add(tabs(propertyXML, "\t\t"));
		}
		xmlContent = replace(xmlContent, "<% PROPERTIES %>", addProperties(propertyList, "\t"));
	}

	@Override
	protected void buildTestSuites(List<?> testsuites) throws Exception {
		super.buildTestSuites(testsuites);
		List<String> testSuiteList = new ArrayList<String>();
		for (Object item : testsuites) {
			Map<?, ?> testSuiteItem = (Map<?, ?>)item;
			List<String> testCaseList = new ArrayList<String>();
			String testSuiteXML = this.getFileContent(getTemplateFile("soapui/testSuite.xml"));
			testSuiteXML = replace(testSuiteXML, "<% TESTSUITE_NAME %>", testSuiteItem.get("name"));
			buildTestCases(testCaseList, (List<?>)testSuiteItem.get("testcases"));
			testSuiteXML = replace(testSuiteXML, "<% TESTCASES %>", String.join("\n", testCaseList));
			List<?> properties = (List<?>)testSuiteItem.get("properties");
			if (properties != null) {
				List<String> propertyList = new ArrayList<String>();
				for (Object property : properties) {
					Map<?, ?> propertyItem = (Map<?, ?>)property;
					String propertyXML = this.getFileContent(getTemplateFile("soapui/property.xml"));
					propertyXML = replace(propertyXML, "<% NAME %>", propertyItem.get("name"));
					propertyXML = replace(propertyXML, "<% VALUE %>", propertyItem.get("value"));
					propertyList.add(tabs(propertyXML, "\t\t\t"));
				}
				testSuiteXML = replace(testSuiteXML, "<% PROPERTIES %>", addProperties(propertyList, "\t\t"));
			}
			testSuiteList.add(testSuiteXML);
		}
		xmlContent = replace(xmlContent, "<% TESTSUITES %>", String.join("\n", testSuiteList));
	}
	
	protected void buildTestCases(List<String> testCaseList, List<?> testcases) throws Exception {
		for (Object item : testcases) {
			Map<?, ?> testCaseItem = (Map<?, ?>)item;
			List<String> testStepList = new ArrayList<String>();
			String testCaseXML = this.getFileContent(getTemplateFile("soapui/testCase.xml"));
			testCaseXML = replace(testCaseXML, "<% TESTCASE_NAME %>", testCaseItem.get("name"));
			buildTestSteps(testStepList, (List<?>)testCaseItem.get("teststeps"));
			testCaseXML = replace(testCaseXML, "<% TEST_STEPS %>", String.join("\n", testStepList));
			List<?> properties = (List<?>)testCaseItem.get("properties");
			if (properties != null) {
				List<String> propertyList = new ArrayList<String>();
				for (Object property : properties) {
					Map<?, ?> propertyItem = (Map<?, ?>)property;
					String propertyXML = this.getFileContent(getTemplateFile("soapui/property.xml"));
					propertyXML = replace(propertyXML, "<% NAME %>", propertyItem.get("name"));
					propertyXML = replace(propertyXML, "<% VALUE %>", propertyItem.get("value"));
					propertyList.add(tabs(propertyXML, "\t\t\t\t"));
				}
				testCaseXML = replace(testCaseXML, "<% PROPERTIES %>", addProperties(propertyList, "\t\t\t"));
			}
			testCaseList.add(testCaseXML);
		}
	}
	
	protected void buildTestSteps(List<String> testStepList, List<?> teststeps) throws Exception {
		Map<?, ?> replace = null;
		for (Object item : teststeps) {
			Map<?, ?> testStepItem = (Map<?, ?>)item;
			String type = (String)testStepItem.get("type");
			String testStepXML = this.getFileContent(getTemplateFile("soapui/teststeps/" + type + ".xml"));
			testStepXML = replace(testStepXML, "<% TESTSTEP_NAME %>", testStepItem.get("name"));
			if (testStepItem.get("config") != null) {
				Object obj = testStepItem.get("config");
				Map<?, ?> config = null;
				if (obj instanceof String) {
					config = (Map<?, ?>)getJSON((String)obj);
				} else {
					config = (Map<?, ?>)obj;
				}
				replace = (Map<?, ?>)config.get("replace");
				if (replace != null) {
					testStepXML = replace(testStepXML, "<% ENDPOINT %>", replace.get("endpoint") != null ? replace.get("endpoint") : DEFAULT_ENDPOINT);
					for (Object key : replace.keySet()) {
						Object value = replace.get(key);
						testStepXML = replace(testStepXML, "<% " + key.toString().toUpperCase() + " %>", value instanceof String ? value : prettyJson(value));
					}
				}
				
			
				List<?> properties = (List<?>)config.get("properties");
				if (properties != null) {
					List<String> propertyList = new ArrayList<String>();
					for (Object property : properties) {
						Map<?, ?> propertyItem = (Map<?, ?>)property;
						String propertyXML = this.getFileContent(getTemplateFile("soapui/property.xml"));
						propertyXML = replace(propertyXML, "<% NAME %>", propertyItem.get("name"));
						propertyXML = replace(propertyXML, "<% VALUE %>", propertyItem.get("value"));
						propertyList.add(tabs(propertyXML, "\t\t\t\t\t\t"));
					}
					testStepXML = replace(testStepXML, "<% PROPERTIES %>", addProperties(propertyList, "\t\t\t\t\t"));
				}
				
				List<?> assertions = (List<?>)config.get("assertions");
				if (assertions != null) {
					List<String> assertionList = new ArrayList<String>();
					for (Object assertion : assertions) {
						Map<?, ?> assertionItem = (Map<?, ?>)assertion;
						String assertionXML = this.getFileContent(getTemplateFile("soapui/assertions/" + assertionItem.get("type") + ".xml"));
						replace = (Map<?, ?>)assertionItem.get("replace");
						for (Object key : replace.keySet()) {
							assertionXML = replace(assertionXML, "<% " + key.toString().toUpperCase() + " %>", replace.get(key).toString());
						}
						assertionList.add(assertionXML);
					}
					testStepXML = replace(testStepXML, "<% ASSERTIONS %>", String.join("\n", assertionList));
				}
			}
			testStepList.add(testStepXML);
		}
	}
	
	protected void buildAssertions(List<String> assertionList, List<?> assertions) throws Exception {
		if (assertions != null) {
			for (Object item : assertions) {
				Map<?, ?> assertionItem = (Map<?, ?>)item;
				String type = (String)assertionItem.get("type");
				String assertionXML = this.getFileContent(getTemplateFile("soapui/assertions/" + type + ".xml"));
				Map<?, ?> properties = (Map<?, ?>)assertionItem.get("properties");
				if (properties != null) {
					for (Object key : properties.keySet()) {
						assertionXML = replace(assertionXML, "<% " + key.toString().toUpperCase() + " %>", properties.get(key));
					}
					assertionList.add(assertionXML);
				}
			}
		}
	}
	
	private String addProperties(List<String> propertyList, String tabs) {
		if (propertyList.size() == 0) {
			return tabs + "<con:properties/>";
		} else {
			return tabs + "<con:properties>\n" + String.join("\n", propertyList) + "\n" + tabs + "</con:properties>";
		}
	}
}