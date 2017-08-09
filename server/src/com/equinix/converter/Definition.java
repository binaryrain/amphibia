package com.equinix.converter;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONObject;

public class Definition {

	private JSONObject doc;
	private JSONObject example;
	private StringBuilder queries;
	private Map<String, String> parameters;
	
	public Definition(JSONObject doc) {
		this.doc = doc;
		queries = new StringBuilder();
		parameters = new HashMap<String, String>();
	}
	
	public JSONObject getRef(String id) {
		String[] paths = id.split("/");
		JSONObject node = doc;
		for (int i = 1; i < paths.length; i++) {
			node = node.getJSONObject(paths[i]);
			if (node.isNullObject()) {
				break;
			}
		}
		if (node.containsKey("example")) {
			example = node.getJSONObject("example");
		}
		return node;
	}
	
	public void addQueryParam(String key, String value) throws Exception {
		if (queries.length() > 0) {
			queries.append("&");
		}
		queries.append(String.format("%s=%s", URLEncoder.encode(key, "UTF-8"), URLEncoder.encode(value, "UTF-8")));
	}
	
	public void addParameter(String key, String value) {
		parameters.put(key, value);
	}
	
	public JSONObject getExample() {
		return example;
	}
	
	public JSONObject getDoc() {
		return doc;
	}
	
	public String getQueries() {
		return queries.length() == 0 ? "" : "?" + queries.toString();
	}
	
	public Map<String, String> getParameters() {
		return parameters;
	}
	
	public static String getEnumOrDefault(JSONObject value) {
		if (value.containsKey("default")) {
			return value.getString("default");
		} else {
			return value.getJSONArray("enum").get(0).toString();
		}
	}
}