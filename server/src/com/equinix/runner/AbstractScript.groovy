package com.equinix.runner;

import org.apache.log4j.Logger;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;

import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.model.testsuite.ProjectRunListener;
import com.eviware.soapui.model.testsuite.TestProperty;

import com.google.common.io.Files;
import com.google.common.base.Charsets;

import net.sf.json.JSON;
import net.sf.json.groovy.JsonSlurper;

@groovy.transform.TypeChecked
public abstract class AbstractScript {

	protected Logger log;
	protected String projectPath;
	protected ProjectRunListener[] listeners;
	protected WsdlProject project;
	protected File jsonDir;
	protected String environment;

	public AbstractScript() throws IOException {
		this.init();
	}

	protected void init() throws IOException {
		Main main = Main.getInstance();
		this.log = main.getLog();
		this.projectPath = main.getProjectPath();
		this.project = main.getProject();
		this.listeners = project.getProjectRunListeners();
		this.jsonDir = main.getJsonDir();
		this.environment = project.getActiveEnvironment().getName();
	}

	public JSON getSchema(String file) throws IOException {
		File uiDir = new File(projectPath, "/..");
		return new JsonSlurper().parse(new File(uiDir.getCanonicalPath(), file));
	}
	
	public JSON getJsonObject(String file) throws IOException {
		return new JsonSlurper().parse(new File(jsonDir, file));
	}

	public JSON getJsonObject(String file, Map<String, TestProperty> properties) throws IOException {
		String content = Files.toString(new File(jsonDir, file), Charsets.UTF_8);
		if (properties != null) {
			for (TestProperty property : properties.values()) {
				content = content.replaceAll("\\\$\\{" + property.getName() + "\\}", Matcher.quoteReplacement(String.valueOf(property.getValue())));
			}
		}
		log.info("getJsonObject: " + content);
		return new JsonSlurper().parseText(content);
	}

	public JSON parse(String body) throws IOException {
		return new JsonSlurper().parseText(body);
	}
}