package com.equinix.converter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.cli.CommandLine;

import com.google.common.collect.ImmutableMap;

import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;

public class Swagger {

	protected CommandLine cmd;
	protected JSONObject doc;
	protected JSONObject output;
	protected File outputFile;
	protected String interfaceName;

	protected JSONNull NULL = JSONNull.getInstance();

	public Swagger(CommandLine cmd, String content, String outputFile) throws Exception {
		this.cmd = cmd;
		this.doc = JSONObject.fromObject(content);
		this.output = new JSONObject();
		this.outputFile = new File(outputFile);
		parse();
		saveFile();
	}

	protected void saveFile() throws Exception {
		outputFile.delete();

		ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("JavaScript");
		scriptEngine.put("jsonString", output.toString());
		scriptEngine.eval("result = JSON.stringify(JSON.parse(jsonString), null, 4)");
		String json = ((String)scriptEngine.get("result")).replaceAll(" {4}", "\t");

		PrintWriter writer = new PrintWriter(new FileOutputStream(outputFile, true));
		writer.println(json);
		writer.close();
		System.out.println("The test file saved successfully.\n" + outputFile);
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

		JSONArray globals = new JSONArray();
		JSONArray schemes = doc.getJSONArray("schemes");
		globals.add(ImmutableMap.<String, Object>builder()
			.put("name", "RestEndPoint")
			.put("value", schemes.get(0) + "://" + doc.getString("host")).build());
		output.accumulate("globals", globals);
		
		JSONArray interfaces = new JSONArray();
		interfaceName = doc.getString("basePath");

		JSONArray resources = new JSONArray();
		resources.add(ImmutableMap.<String, Object>builder()
				.put("name", "CONTENT-TYPE")
				.put("value", "application/json").build());

		interfaces.add(ImmutableMap.<String, Object>builder()
				.put("name", interfaceName)
				.put("type", "rest")
				.put("resources", resources).build());
		output.accumulate("interfaces", interfaces);

		output.accumulate("properties", new JSONArray());
		
		JSONArray testsuites = new JSONArray();
		JSONArray testcases = new JSONArray();

		addTestCases(testcases);

		testsuites.add(ImmutableMap.<String, Object>builder()
				.put("name", doc.getString("basePath").split("/")[1])
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
	}

	protected void addTestSteps(JSONArray teststeps, List<Object[]> apiList) throws Exception {
		for (Object[] item : apiList) {
			String path = item[0].toString();
			String methodName = item[1].toString().toUpperCase();
			JSONObject api = (JSONObject) item[2];
			teststeps.add(ImmutableMap.<String, Object>builder()
					.put("type", "restrequest")
					.put("name", methodName + " - " + api.getString("summary"))
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
				.put("path", path.substring(1) + definition.getQueries())
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
					}
				} else if ("query".equals(in) && (param.containsKey("default") || param.containsKey("enum"))) {
					definition.addQueryParam(param.getString("name"), Definition.getEnumOrDefault(param));
				} else if ("path".equals(in)) {
					definition.addParameter(param.getString("name"), Definition.getEnumOrDefault(param));
				}
			}
		}
	}
}