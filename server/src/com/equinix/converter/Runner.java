package com.equinix.converter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableMap;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class Runner {

	protected Swagger swagger;
	protected JSONObject definitions;
	protected final Map<Object, Object> runner;
	
	public static final String OUTPUT_DIR = "tests";
	private static final Logger LOGGER = Logger.getLogger(Runner.class.getName());
	
	public Runner(Swagger swagger) throws Exception {
		this.swagger = swagger;
		this.definitions = swagger.getDoc().getJSONObject("definitions");

		this.runner = ImmutableMap.<Object, Object>builder()
							.put("options", ImmutableMap.<Object, Object>builder()
									.put("appendLogs", false)
									.put("updateTransfersProperty", false)
									.put("continueOnError", true)
									.put("testCaseTimeout", 5000).build())
							.put("properties", new LinkedHashMap<Object, Object>())
							.put("testcases", new ArrayList<Object>()).build();
	}
	
	public void createRunner() throws Exception {
		save(swagger.getJson(runner), "runner.json", null);
	}
	
	protected String save(String json, String fileName, String childDir) throws Exception {
		if ("false".equals(Converter.cmd.getOptionValue(Converter.TESTS))) {
			return null;
		}
		File path = new File(OUTPUT_DIR);
		if (childDir != null) {
			path = new File(path, childDir);
		}
		File outputDir = new File(swagger.getOutputDir(), path.getPath());
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}
		File outputFile = new File(outputDir, fileName);
		PrintWriter writer = new PrintWriter(new FileOutputStream(outputFile, false));
		writer.println(swagger.getJson(json));
		writer.close();
		LOGGER.debug("The schena file saved successfully.\n" + outputFile);
		return swagger.getPath(path) + "/" + fileName;
	}

	@SuppressWarnings("unchecked")
	public void addProperties(JSONObject propertyValues) {
		Map<Object, Object> properties = (Map<Object, Object>)runner.get("properties");
		for (Object key : propertyValues.keySet()) {
			properties.put(key, propertyValues.get(key));
		}
	}
	
	@SuppressWarnings("unchecked")
	public void addTestCases(String interfaceName, Map<String, List<Object[]>> testCaseMap) throws Exception {
		List<Object> testcases = (ArrayList<Object>)runner.get("testcases");
		for (String testcaseName : testCaseMap.keySet()) {
			String childDir = testcaseName.toLowerCase();
			List<Object[]> testcaseTests = testCaseMap.get(testcaseName);
			List<String> tests = new ArrayList<String>();
			Map<String, Object> testcase = new LinkedHashMap<String, Object>();
			testcase.put("name", testcaseName);
			testcase.put("testsuite", interfaceName);
			testcase.put("disabled", false);
			testcase.put("tests", tests);

			for (Object[] items : testcaseTests) {
				String testFile = items[1] + items[0].toString().replaceAll("/", "_") + ".json";
				tests.add(OUTPUT_DIR + "/" + childDir + "/" + testFile);

				addTestSteps(items[1].toString(), items[2], testFile, childDir);
			}
			testcases.add(testcase);
		}
	}
	
	protected void addTestSteps(String methodName, Object json, String fileName, String childDir) throws Exception {
		JSONObject api = (JSONObject) json;
		List<Object> teststeps = new ArrayList<Object>();
		List<Object> steps = new ArrayList<Object>();
		Map<Object, Object> properties = new LinkedHashMap<Object, Object>();
		Map<Object, Object> body = new LinkedHashMap<Object, Object>();

		Map<String, Object> teststep = new LinkedHashMap<String, Object>();
		teststep.put("name", swagger.getTestCaseName(methodName, api.getString("summary")));
		teststep.put("disabled", false);
		teststep.put("properties", addTestStepProperties(api, properties, body));
		teststep.put("steps", steps);
		steps.add(addStep(fileName, api, body));
		teststeps.add(teststep);
		save(swagger.getJson(teststeps), fileName, childDir);
	}
	
	protected Map<Object, Object> addTestStepProperties(JSONObject api, Map<Object, Object> properties, Map<Object, Object> body) {
		if (api.containsKey("parameters")) {
			JSONArray parameters = api.getJSONArray("parameters");
			for (Object obj : parameters) {
				JSONObject param = (JSONObject) obj;
				if ("body".equals(param.getString("in"))) {
					if (param.containsKey("schema")) {
						JSONObject schema = param.getJSONObject("schema");
						if (schema.containsKey("$ref")) {
							addBodyAndProperties(schema.getString("$ref"), properties, body, "");
						}
					}
					break;
				}
			}
		}
		return properties;
	}
	
	protected void addBodyAndProperties(String ref, Map<Object, Object> properties, Map<Object, Object> body, String ids) {
		String definitionName = swagger.getDefinitionName(ref);
		JSONObject definition = definitions.getJSONObject(definitionName);
		if (!definition.isNullObject() && definition.containsKey("properties")) {
			JSONObject props = definition.getJSONObject("properties");
			addBodyAndProperty(props, properties, body, ids);
		}
	}
	
	protected void addBodyAndProperty(JSONObject props, Map<Object, Object> properties, Map<Object, Object> body, String ids) {
		for (Object key : props.keySet()) {
			String id = ids + (ids.length() == 0 ? "" : ".") + key;
			String param = "${" + id + "}";

			JSONObject val = (JSONObject)props.get(key);
			if (val.containsKey("default")) {
				properties.put(id, val.get("default"));
				body.put(key, param);
			} else if (val.containsKey("example")) {
				properties.put(id, val.get("example"));
				body.put(key, param);
			} else if (val.containsKey("enum")) {
				properties.put(id, val.getJSONArray("enum").get(0));
				body.put(key, param);
			} else {
				Map<Object, Object> child = new LinkedHashMap<Object, Object>();
				if (val.containsKey("type") && "array".equals(val.getString("type"))) {
					body.put(key, new Object[] {child});
				} else {
					body.put(key, child);
				}
				if (val.containsKey("$ref")) {	
					addBodyAndProperties(val.getString("$ref"), properties, child, id);
				} else if (val.containsKey("items")) {
					JSONObject items = val.getJSONObject("items");
					if (items.containsKey("$ref")) {
						addBodyAndProperties(items.getString("$ref"), properties, child, id);
					} else if (items.containsKey("properties")) {
						addBodyAndProperty(items.getJSONObject("properties"), properties, child, id);
					}
				} else if (val.containsKey("properties")) {
					addBodyAndProperty(val.getJSONObject("properties"), properties, child, id);
				} else {
					body.put(key, null);
				}
			}
		}
	}
	
	protected Map<String, Object> addStep(String fileName, JSONObject api, Map<Object, Object> body) throws Exception {
		Map<String, Object> step = new LinkedHashMap<String, Object>();

		Object requestBody = Swagger.NULL;
		Object requestSchema = Swagger.NULL;
		if (api.containsKey("parameters")) {
			JSONArray parameters = api.getJSONArray("parameters");
			for (Object obj : parameters) {
				JSONObject param = (JSONObject) obj;
				if ("body".equals(param.getString("in"))) {
					if (param.containsKey("schema")) {
						JSONObject schema = param.getJSONObject("schema");
						if (schema.containsKey("$ref")) {
							String ref = schema.getString("$ref");
							if (Schema.schemas.get(ref) != null) {
								requestSchema = Schema.schemas.get(ref);
								break;
							}
						}
					}
				}
			}
		}
		
		if (!body.isEmpty()) {
			requestBody = save(swagger.getJson(body), fileName, "requests");
		}
		
		Map<Object, Object> responseProperties = new LinkedHashMap<Object, Object>();
		Object responseBody = Swagger.NULL;
		Object responseSchema = Swagger.NULL;
		Object responseAsserts = new ArrayList<Object>();
		if (api.containsKey("responses")) {
			JSONObject responses = api.getJSONObject("responses");
			for (Object httpCode : responses.keySet()) {
				JSONObject response = responses.getJSONObject(httpCode.toString());
				if (response.containsKey("schema")) {
					JSONObject schema = response.getJSONObject("schema");
					if (schema.containsKey("$ref")) {
						String ref = schema.getString("$ref");
						if (Schema.schemas.get(ref) != null) {
							responseSchema = Schema.schemas.get(ref);
						}
						Map<Object, Object> resBody = new LinkedHashMap<Object, Object>();
						addBodyAndProperties(schema.getString("$ref"), responseProperties, resBody, "");
						if (!resBody.isEmpty()) {
							responseBody = save(swagger.getJson(resBody), fileName, "responses");
						}
					}
				}

				if (Swagger.asserts.get(httpCode) != null) {
					responseAsserts = swagger.getPath(new File(Schema.OUTPUT_DIR, Swagger.ASSERTS_OUTPUT_DIR)) + "/" + httpCode + ".json";
					break;
				}
			}
		}

		step.put("tag", api.getString("operationId"));
		step.put("request", ImmutableMap.<String, Object>builder()
				.put("properties", new LinkedHashMap<Object, Object>())
				.put("body", requestBody)
				.put("schema", requestSchema).build());
		step.put("response", ImmutableMap.<String, Object>builder()
				.put("properties", responseProperties)
				.put("body", responseBody)
				.put("schema", responseSchema)
				.put("asserts", responseAsserts).build());
		return step;
	}
}