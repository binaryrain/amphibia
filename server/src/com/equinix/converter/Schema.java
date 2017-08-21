package com.equinix.converter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import java.util.LinkedHashMap;

import com.google.common.collect.ImmutableMap;

import net.sf.json.JSONObject;

public class Schema {

	protected Swagger swagger;
	protected JSONObject docDefinitions;
	protected Map<Object, Object> fields;
	protected Map<Object, Object> definitions;
	protected Map<Object, Object> schema;

	public static final String OUTPUT_DIR = "schemas";
	public static final Map<String, String> schemas = new LinkedHashMap<String, String>();

	private static final Logger LOGGER = Logger.getLogger(Schema.class.getName());

	@SuppressWarnings("unchecked")
	public Schema(Swagger swagger, String ref, String childDir) throws Exception {
		this.swagger = swagger;
		this.schema = ImmutableMap.<Object, Object>builder()
							.put("type", "schema")
							.put("extends", new ArrayList<String>())
							.put("fields", new LinkedHashMap<Object, Object>())
							.put("definitions", new LinkedHashMap<Object, Object>()).build();
		if ("false".equals(Converter.cmd.getOptionValue(Converter.SCHEMA))) {
			return;
		}

		this.docDefinitions = swagger.getDoc().getJSONObject("definitions");
		if (!this.docDefinitions.isNullObject()) {
			fields = (Map<Object, Object>)this.schema.get("fields");
			definitions = (Map<Object, Object>)this.schema.get("definitions");
			String definitionName = this.parse(fields, ref, new ArrayList<String>());
			if (!schemas.containsKey(ref)) {
				String path = save(swagger, JSONObject.fromObject(schema).toString(), definitionName, childDir);
				schemas.put(ref, path);
			}
		}
	}
	
	protected String parse(Map<Object, Object> parent, String ref, List<String> paths) {
		String definitionName = swagger.getDefinitionName(ref);
		JSONObject definition = this.docDefinitions.getJSONObject(definitionName);
		if (!definition.isNullObject()) {
			parseProperties(parent, definition.getJSONObject("properties"), paths);
		}
		return definitionName;
	}
	
	protected void parseProperties(Map<Object, Object> parent, JSONObject properties, List<String> paths) {
		for (Object name : properties.keySet()) {
			JSONObject props = properties.getJSONObject(name.toString());
			if (props.containsKey("properties")) {
				paths.add(name.toString());
				parseProperties(parent, props.getJSONObject("properties"), paths);
			} else {
				Map<String, Object> details = new LinkedHashMap<String, Object>();
				parent.put(name, details);
				String path = String.join(".",  paths) + (paths.size() > 0 ? "." : "") + name;
				if (props.containsKey("type")) {
					details.put("$type", props.getString("type"));
					details.put("$path", path);
					JSONObject items = props.getJSONObject("items");
					if (!items.isNullObject()) {
						if (items.containsKey("$ref")) {
							addDefinition(details, items.getString("$ref"), paths);
						} else if (items.containsKey("properties")) {
							Map <Object, Object> definition = new LinkedHashMap<Object, Object>();
							details.put("$ref", name);
							definitions.put(name, definition);
							parseProperties(definition, items.getJSONObject("properties"), paths);
						}
					}
				} else if (props.containsKey("$ref")) {
					details.put("$type", "object");
					details.put("$path", path);
					if (props.containsKey("$ref")) {
						addDefinition(details, props.getString("$ref"), paths);
					}
				}
			}
		}
	}
	
	protected void addDefinition(Map<String, Object> details, String ref, List<String> paths) {
		String definitionName = swagger.getDefinitionName(ref);
		details.put("$ref", definitionName);
		Map <Object, Object> definition = new LinkedHashMap<Object, Object>();
		definitions.put(definitionName, definition);
		parse(definition, ref, paths);
	}
	
	public static String save(Swagger swagger, String json, String fileName, String childDir) throws Exception {
		if ("false".equals(Converter.cmd.getOptionValue(Converter.SCHEMA))) {
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
		File outputFile = new File(outputDir, fileName + ".json");
		PrintWriter writer = new PrintWriter(new FileOutputStream(outputFile, false));
		writer.println(swagger.getJson(json));
		writer.close();
		LOGGER.debug("The schena file saved successfully.\n" + outputFile);
		Converter.addFile(Converter.ADD_SCHEMA, outputFile);
		return swagger.getPath(path) + "/" + fileName + ".json";
	}
}
