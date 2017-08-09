package com.equinix.runner;

import static ReportTestCase.State;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.model.testsuite.TestCaseRunner;

public class JUnitReports {

	protected File xmlFile;
	protected ReportTestSuite testsute;
	
	protected static Map<String, ReportTestSuite> testsuites;

	public JUnitReports() {
		testsuites = new TreeMap<String, ReportTestSuite>();
	}

	public void createNewReport(String outputFolder, String name) {
		xmlFile = new File(outputFolder, "TEST-" + name + ".xml");
		testsute = new ReportTestSuite();
		testsuites.put(name, testsute);
	}

	public ReportTestCase addNewTestCase(String className, String testCaseName) {
		ReportTestCase testcase = new ReportTestCase(className, testCaseName);
		testsute.testcases.add(testcase);
		return testcase;
	}

	public void afterRun(ReportTestCase testcase, TestCaseRunner testRunner) {
		System.out.println(testcase.toString());
		testcase.setTime(testRunner.getTimeTaken());
		testsute.totalTime += testRunner.getTimeTaken();
	}

	public void save() throws IOException {
		int totalTime = 0;
		int totalTestSteps = 0;
		int totalErrors = 0;
		int totalFailed = 0;
		int totalSkipped = 0;

		PrintWriter pw = new PrintWriter(new FileOutputStream(xmlFile, true));
		Map<String, Boolean> testCases = new TreeMap<String, Boolean>();

		pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		for (String testSuiteName : testsuites.keySet()) {
			ReportTestSuite testsuite = testsuites.get(testSuiteName);
			StringBuffer sw = new StringBuffer();
			totalTime += testsuite.totalTime;
			int noofFailures, noofErrors, noofSkipped;
			noofFailures = noofErrors = noofSkipped = 0;

			for (ReportTestCase testcase : testsuite.testcases) {
				totalTestSteps++;
				testCases.put(testcase.getClassName(), true);
				switch (testcase.getState()) {
					case State.SUCCESS:
						sw.append("<testcase time=\"" + testcase.getTime() + "\" classname=\"" + testcase.getClassName() + "\" name=\"" + testcase.getTestCaseName() + "\" />");
						break;
					case State.ERROR:
						sw.append("<testcase time=\"" + testcase.getTime() + "\" classname=\"" + testcase.getClassName() + "\" name=\"" + testcase.getTestCaseName() + "\">" + 
								"<error message=\"" + testcase.getMessage() + "\" type=\"" + testcase.getType() + "\"><![CDATA[" + testcase.getStackTrace() + "]]></error></testcase>");
						noofErrors++;
						totalErrors++;
						break;
					case State.FAIL:
						sw.append("<testcase time=\"" + testcase.getTime() + "\" classname=\"" + testcase.getClassName() + "\" name=\"" + testcase.getTestCaseName() + "\">" + 
								"<failure message=\"" + testcase.getMessage() + "\" type=\"" + testcase.getType() + "\"><![CDATA[" + testcase.getStackTrace() + "]]></failure></testcase>");
						noofFailures++;
						totalFailed++;
						break;
					case State.SKIPPED:
						sw.append("<testcase time=\"" + testcase.getTime() + "\" classname=\"" + testcase.getClassName() + "\" name=\"" + testcase.getTestCaseName() + "\"><skipped message=\"\"/></testcase>");
						noofSkipped++;
						totalSkipped++;
						break;
				}
			}

			pw.println("<testsuite time=\"" + (testsuite.totalTime / 1000) + "\" failures=\"" + noofFailures + "\" errors=\""
				+ noofErrors + "\" " + "skipped=\"" + noofSkipped + "\" tests=\"" + testsuite.testcases.size()
				+ "\" " + "timestamp=\"" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(testsuite.timestamp) + "\" " + "name=\"" + testSuiteName + "\">");
			pw.println(sw.toString());
			pw.println("</testsuite>");
		}
		pw.close();

		System.out.println();
		System.out.println("SoapUI " + SoapUI.SOAPUI_VERSION + " TestCaseRunner Summary");
		System.out.println("-----------------------------");
		System.out.println("Time Taken: " + totalTime + "ms");
		System.out.println("Total TestSuites: " + testsuites.size());
		System.out.println("Total TestCases: " + testCases.size());
		System.out.println("Total TestSteps: " + totalTestSteps);
		System.out.println("Total Errors: " + totalErrors);
		System.out.println("Total Failed: " + totalFailed);
		System.out.println("Total Skipped: " + totalSkipped);
	}


	public class ReportTestSuite {
		public double totalTime = 0;
		public long timestamp = System.currentTimeMillis();
		public List<ReportTestCase> testcases = new ArrayList<ReportTestCase>();
	}
}
