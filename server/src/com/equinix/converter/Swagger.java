package com.equinix.converter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableMap;

import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;

public class Swagger {

	protected CommandLine cmd;
	protected JSONObject doc;
	protected JSONObject output;
	protected JSONObject swaggerProperties;
	protected File outputDir;
	protected File outputFile;
	protected String interfaceName;
	protected Runner runner;
	
	private static final Logger LOGGER = Logger.getLogger(Swagger.class.getName());

	public static final String ASSERTS_OUTPUT_DIR = "asserts";
	public static final Map<String, Map<String, Object>> asserts = new TreeMap<String, Map<String, Object>>();
	public static final JSONNull NULL = JSONNull.getInstance();

	public Swagger(CommandLine cmd, InputStream is, File outputDir) throws Exception {
		this.cmd = cmd;
		this.doc = getContent(is);
		this.output = new JSONObject();
		this.outputDir = outputDir;
		this.runner = new Runner(this);
		parse();
		saveFile();
	}

	protected void saveFile() throws Exception {
		outputFile.delete();
		for (String httpCode : asserts.keySet()) {
			Schema.save(this, JSONArray.fromObject(new Object[] {asserts.get(httpCode)}).toString(), httpCode, ASSERTS_OUTPUT_DIR);
		}
		
		this.runner.createRunner();

		PrintWriter writer = new PrintWriter(new FileOutputStream(outputFile, true));
		writer.println(getJson(output.toString()));
		writer.close();
		LOGGER.debug("The test file saved successfully.\n" + outputFile);
		Converter.addFile(Converter.ADD_PROJECT, outputFile);
	}
	
	public String getJson(List<?> value) throws Exception {
		return getJson(JSONArray.fromObject(value).toString());
	}

	public String getJson(Map<?, ?> value) throws Exception {
		return getJson(JSONObject.fromObject(value).toString());
	}

	public String getJson(String strJson) throws Exception {
		ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("JavaScript");
		scriptEngine.put("jsonString", strJson);
		scriptEngine.eval("result = JSON.stringify(JSON.parse(jsonString), null, 4)");
		return ((String)scriptEngine.get("result")).replaceAll(" {4}", "\t");
	}

	protected void parse() throws Exception {
		String name = cmd.getOptionValue(Converter.NAME);
		if (name == null) {
			JSONObject info = doc.getJSONObject("info");
			if (!info.isNullObject()) {
				name = (String) info.getString("title").replaceAll(" ", "");
			}
		}
		output.accumulate("name", name);
		outputFile = new File(outputDir, name + ".json");

		String param = cmd.getOptionValue(Converter.PROPERTIES);
		if (param != null) {
			swaggerProperties = getContent(new FileInputStream(new File(param).getAbsolutePath()));
		}

		JSONArray globals = new JSONArray();
		JSONArray schemes = doc.getJSONArray("schemes");
		globals.add(ImmutableMap.<String, Object>builder()
			.put("name", "RestEndPoint")
			.put("value", schemes.get(0) + "://" + doc.getString("host")).build());

		if (swaggerProperties != null) {
			JSONObject propertyGlobals = swaggerProperties.getJSONObject("globals");
			for (Object key : propertyGlobals.keySet()) {
				globals.add(ImmutableMap.<String, Object>builder()
						.put("name", key)
						.put("value", propertyGlobals.get(key)).build());
			}
		}

		output.accumulate("globals", globals);

		
		JSONArray interfaces = new JSONArray();
		interfaceName = doc.getString("basePath");

		JSONArray resources = new JSONArray();
		if (swaggerProperties != null) {
			JSONObject propertyHeaders = swaggerProperties.getJSONObject("headers");
			for (Object key : propertyHeaders.keySet()) {
				resources.add(ImmutableMap.<String, Object>builder()
						.put("name", key)
						.put("value", propertyHeaders.get(key)).build());
			}
		} else {
			resources.add(ImmutableMap.<String, Object>builder()
					.put("name", "CONTENT-TYPE")
					.put("value", "application/json").build());
		}

		interfaces.add(ImmutableMap.<String, Object>builder()
				.put("name", interfaceName)
				.put("type", "rest")
				.put("resources", resources).build());
		output.accumulate("interfaces", interfaces);

		JSONArray properties = new JSONArray();
		if (swaggerProperties != null) {
			JSONObject propertyValues = swaggerProperties.getJSONObject("properties");
			this.runner.addProperties(propertyValues);
			for (Object key : propertyValues.keySet()) {
				properties.add(ImmutableMap.<String, Object>builder()
						.put("name", key)
						.put("value", propertyValues.get(key)).build());
			}
		}
		output.accumulate("properties", properties);
		
		JSONArray testsuites = new JSONArray();
		JSONArray testcases = new JSONArray();

		addTestCases(testcases);

		testsuites.add(ImmutableMap.<String, Object>builder()
				.put("name", doc.getString("basePath"))
				.put("properties", new JSONArray())
				.put("testcases", testcases).build());
		output.accumulate("testsuites", testsuites);
	}

	protected void addTestCases(JSONArray testcases) throws Exception {
		JSONObject paths = doc.getJSONObject("paths");
		Map<String, List<Object[]>> testCaseMap = new TreeMap<String, List<Object[]>>();
		for (Object path : paths.keySet()) {
			JSONObject apis =  paths.getJSONObject(path.toString());
			for (Object methodName : apis.keySet()) {
				if ("parameters".equals(methodName)) {
					for (Object item : apis.getJSONArray("parameters")) {
						JSONObject param = (JSONObject) item;
						String in = param.getString("in");
						if ("path".equals(in)) {
							path = path.toString().replaceAll("\\{" + param.getString("name") + "\\}", Definition.getEnumOrDefault(param));
						}
					}
					continue;
				}
				JSONObject api = apis.getJSONObject(methodName.toString());
				String tagName = (String) api.getJSONArray("tags").get(0);
				List<Object[]> apiList = testCaseMap.get(tagName);
				if (apiList == null) {
					apiList = new ArrayList<Object[]>();
					testCaseMap.put(tagName, apiList);
				}
				apiList.add(new Object[] {path, methodName, api});
			}
		}

		for (String tagName : testCaseMap.keySet()) {
			JSONArray teststeps = new JSONArray();
			addTestSteps(teststeps, testCaseMap.get(tagName));
			testcases.add(ImmutableMap.<String, Object>builder()
					.put("name", tagName)
					.put("properties", new JSONArray())
					.put("teststeps", teststeps).build());
		}
		
		this.runner.addTestCases(interfaceName, testCaseMap);
	}

	protected void addTestSteps(JSONArray teststeps, List<Object[]> apiList) throws Exception {
		for (Object[] item : apiList) {
			String path = item[0].toString();
			String methodName = item[1].toString().toUpperCase();
			JSONObject api = (JSONObject) item[2];
			teststeps.add(ImmutableMap.<String, Object>builder()
					.put("type", "restrequest")
					.put("name", getTestCaseName(methodName, api.getString("summary")))
					.put("config", getConfig(path, methodName, api)).build());
		}
	}

	protected JSONObject getConfig(String path, String methodName, JSONObject api) throws Exception {
		JSONObject config = new JSONObject();

		JSONArray assertions = new JSONArray();
		JSONObject responses = api.getJSONObject("responses");
		for (Object httpCode : responses.keySet()) {
			assertions.add(ImmutableMap.<String, Object>builder()
					.put("type", "ValidHTTPStatusCodes")
					.put("replace", ImmutableMap.<String, Object>builder()
					.put("value", Integer.parseInt(httpCode.toString())).build()).build());
			break;
		}
		config.accumulate("assertions", assertions);

		JSONArray statuses = null;
		if (swaggerProperties != null) {
			statuses = swaggerProperties.getJSONArray("asserts");
		}
		for (Object httpCode : responses.keySet()) {
			int code = Integer.parseInt(httpCode.toString());
			JSONObject response = responses.getJSONObject(httpCode.toString());
			Map<String, Object> item = new TreeMap<String, Object>();
			item.put("statusCode", code);
			if (statuses != null) {
				for (Object obj : statuses) {
					JSONObject jsonObj = (JSONObject) obj;
					JSONArray range = jsonObj.getJSONArray("range");
					if (code >= (int)range.get(0) && code <= (int)range.get(1)) {
						item.put("status", jsonObj.getString("status"));
						break;
					}
				}
			}
			if (!"false".equals(Converter.cmd.getOptionValue(Converter.SCHEMA))) {
				asserts.put(httpCode.toString(), item);
			}

			if (code >= 200 && code < 300) {
				if (response.containsKey("schema") && response.getJSONObject("schema").containsKey("$ref")) {
					new Schema(this, response.getJSONObject("schema").getString("$ref"), "responses");
				}
			}
		}

		Definition definition = new Definition(doc);
		parseDefinition(definition, api);
		JSONObject body = api.getJSONObject("example");
		if (body.isNullObject()) {
			body = definition.getExample();
		}

		for (String name : definition.getParameters().keySet()) {
			path = path.replaceAll("\\{" + name + "\\}", definition.getParameters().get(name));
		}

		config.accumulate("replace", ImmutableMap.<String, Object>builder()
				.put("endpoint", NULL)
				.put("method", methodName)
				.put("interface", interfaceName)
				.put("mediaType", "application/json")
				.put("path", interfaceName.substring(1) + path + definition.getQueries())
				.put("body", body == null ? NULL : body).build());
		return config;
	}

	protected void parseDefinition(Definition definition, JSONObject api) throws Exception {
		if (api.containsKey("parameters")) {
			for (Object item : api.getJSONArray("parameters")) {
				JSONObject param = (JSONObject) item;
				String in = param.getString("in");
				if ("body".equals(in)) {
					JSONObject schema = param.getJSONObject("schema");
					if (schema.containsKey("$ref")) {
						definition.getRef(schema.getString("$ref"));
						new Schema(this, schema.getString("$ref"), "requests");
					}
				} else if ("query".equals(in) && (param.containsKey("default") || param.containsKey("enum"))) {
					definition.addQueryParam(param.getString("name"), Definition.getEnumOrDefault(param));
				} else if ("path".equals(in)) {
					definition.addParameter(param.getString("name"), Definition.getEnumOrDefault(param));
				}
			}
		}
	}
	
	protected JSONObject getContent(InputStream is) throws IOException {
		return JSONObject.fromObject(IOUtils.toString(is));
	}

	public String getPath(File path) {
		return path.getPath().replaceAll("\\\\", "/");
	}
	
	public String getTestCaseName(String methodName, String summary) {
		return  methodName.toUpperCase() + " - " + summary;
	}

	public String getDefinitionName(String ref) {
		return ref.split("/")[2];
	}

	public File getOutputDir() {
		return outputDir;
	}

	public JSONObject getDoc() {
		return doc;
	}
}