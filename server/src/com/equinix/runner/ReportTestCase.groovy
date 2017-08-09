package com.equinix.runner;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

public class ReportTestCase {
	
	public static enum State {
		SUCCESS,
		ERROR,
		FAIL,
		SKIPPED
	};

	private String className;
	private String testCaseName;
	private State state;
	private Throwable t;
	private long time;

	public ReportTestCase(String className, String testCaseName) {
		this.className = className;
		this.testCaseName = testCaseName;
		this.state = State.SKIPPED;
	}
 
	public void addException(State state, Throwable t) {
		this.state = state;
		this.t = t;
	}
	
	public void success() {
		this.state = State.SUCCESS;
	}
	
	public State getState() {
		return this.state;
	}
	
	public String getType() {
		return t.getClass().getName();
	}
	
	public String getMessage() {
		return StringEscapeUtils.escapeHtml(t.getMessage());
	}
	
	public String getStackTrace() {
		return StringUtils.join(t.getStackTrace(), "\n\t");
	}
	
	public String getClassName() {
		return className;
	}
	
	public String getTestCaseName() {
		return testCaseName;
	}
	
	public String getTime() {
		return String.valueOf(time / 1000);
	}

	public void setTime(long time) {
		this.time = time;
	}
	
	@Override
	public String toString() {
		return "[" + className + "] " + testCaseName + " (" + state + ")";
	}
}