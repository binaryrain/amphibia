package com.equinix.builder;

import java.io.File;

import org.apache.commons.cli.CommandLine;

public class ReadyAPI extends SoapUI {

	public static final String VERSION = "6.0.0";

	public ReadyAPI(CommandLine cmd) throws Exception {
		super(cmd);
	}

	@Override
	protected void buildProject(String name) throws Exception {
		super.buildProject(name);
		outputFile = new File(outputDirPath, name + "-ready-" + VERSION + ".xml");
		xmlContent = replace(xmlContent, "<% VERSION %>", VERSION);
		xmlContent = replace(xmlContent, "<% PROJECT_NAME %>", name);
	}
}