package de.tu_bs.cs.isf.cbc.tool.helper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.emf.common.util.URI;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.testng.ITestContext;  
import org.testng.ITestListener;  
import org.testng.ITestResult;

import de.tu_bs.cs.isf.cbc.tool.exceptions.DiagnosticsException;
import de.tu_bs.cs.isf.cbc.util.Console;
import de.tu_bs.cs.isf.cbc.util.FileUtil;
import diagnostics.DataCollector;
import diagnostics.DataType;  

/**
 * Listener for checking the execution of test cases generated by the test generator.
 * @author Fynn
 */
public class TestAndAssertionListener implements ITestListener {
	  public final URI projectPath;
	  private final List<TestCaseData> inputDataTupels;
	  private final List<String> globalVars;
	  private static final String INSERTED_SYMBOL = "{=";
	  private static final String INSERTED_CLOSED_SYMBOL = "}";
	  public static final Color red = new Color(new RGB(150, 10, 10));
	  public static final Color green = new Color(new RGB(10, 150, 10));
	  public static final Color blue = new Color(new RGB(10, 10, 200));
	  public static final Color black = new Color(new RGB(0, 0, 0));
	  private List<String> executedBranches = new ArrayList<String>();
	  private List<String> allBranches;
	  private final DataCollector dataCollector;
	  private long testStartTime;
	  
	  public TestAndAssertionListener(final URI projectPath, final String diagramName, final List<String> globalVars, final List<TestCaseData> inputDataTupels) throws DiagnosticsException {
		  this.projectPath = projectPath;
		  this.inputDataTupels = inputDataTupels;
		  this.globalVars = globalVars;
		  this.dataCollector = new DataCollector(projectPath, DataType.TESTCASE, diagramName); 
	  }
	  
	  public static String printTestMethodWithValues(String code, ITestContext context, String testNr) {
		  var varsWithValues = new HashMap<String, String>();
		  int offset = 0;
		  int prevLen;
		  
		  String output = code.substring(0, code.indexOf("\n\n") + 2);
		  code = code.substring(code.indexOf("\n\n") + 2, code.length());
		  
		  for (var contextVar : context.getAttributeNames()) {
			  if (contextVar.startsWith(testNr)) {
				  var varWithoutId = contextVar.substring(contextVar.indexOf(testNr) + testNr.length(), contextVar.length());
				  if (context.getAttribute(contextVar) == null) {
					  Console.println("TestAndAssertionListenerWarning: Couldn't get value for variable '" + varWithoutId + "'.");
					  continue;
				  }
				  varsWithValues.put(varWithoutId, context.getAttribute(contextVar).toString());
			  }
		  }
		  		
		  for (var v : varsWithValues.keySet()) {
			  if (varsWithValues.get(v).contains("[")) {
				  // arrays can become quite big and that would clutter the error display
				  continue;
			  }
			  offset = 0;
			  prevLen = code.length();
			  Pattern p = Pattern.compile("\\W" + v + "[^\\w\\" + INSERTED_SYMBOL.charAt(0) + "]");
			  Matcher m = p.matcher(code);
			  
			  while (m.find()) {
				  code = code.substring(0, m.start() + offset + 1) + v + INSERTED_SYMBOL 
						  + varsWithValues.get(v) 
						  + INSERTED_CLOSED_SYMBOL + code.substring(m.end() + offset - 1, code.length());
				  offset += code.length() - prevLen;
				  prevLen = code.length();
			  }
		  }
		  output = output + code;
		  return output;
	  }
	  
	  private String getClassCode(String className) {
			var dir = new File(FileUtil.getProjectLocation(this.projectPath) + "\\tests");	
			if (!dir.exists()) {
				return "";
			}
			var javaFile = new File(FileUtil.getProjectLocation(this.projectPath) + "\\tests\\" + className + ".java");
			if (!javaFile.exists()){
				return "";
		    }
			try {
				var code = Files.readString(Paths.get(javaFile.getPath()));
				return code;
			} catch (IOException e) {}
			return "";
	  }
	  
	  protected int getErrorLineNr(final StackTraceElement[] stackTrace, final String className, final String methodName, final String methodCode) {
		  int errorLineNr = -1;
		  for (var trace : stackTrace) {
			  if (trace.getMethodName().equals(methodName)) {
				  errorLineNr = trace.getLineNumber();
				  break;
			  }
		  }
		  var classCode = getClassCode(className);
		  var lines = classCode.split("\\n");
		  var line = lines[errorLineNr-1].trim();
		  var methodLines = methodCode.split("\\n");
		  for (int i = 0; i < methodLines.length; i++) {
			  if (methodLines[i].trim().equals(line)) {
				  return i;
			  }
		  }
		  return -1;
	  }
		
	  public String getLine(final String code, int lineNr) {
		 var lines = code.split("\\n");
		 return lines[lineNr].trim();
	  }
	  
	  public String removeHelperLines(String testCode) {
		  Pattern p = Pattern.compile("\\t\\tcontext\\.");
		  Matcher m = p.matcher(testCode);
		  int end;
		  int prev = testCode.length();
		  int offset = 0;
		  String helper;
		  
		  while (m.find()) {
			  helper = testCode.substring(m.start() + offset, testCode.length());
			  end = helper.indexOf("\n");
			  testCode = testCode.substring(0, m.start() + offset + 2) + testCode.substring(m.start() + offset + end + 1, testCode.length());
			  offset += testCode.length() - prev;
			  prev = testCode.length();
		  }	  
		  
		  testCode = CodeHandler.removeTabs(testCode);
		  testCode = CodeHandler.insertTabs(testCode, 1);
		  // finally remove all precondition checks
		  if (testCode.contains(CodeHandler.PRECHECKS_START)) {
			 testCode = testCode.substring(0, testCode.indexOf(CodeHandler.PRECHECKS_START)).stripTrailing() + "\n"
					 + testCode.substring(testCode.indexOf(CodeHandler.PRECHECKS_END) + CodeHandler.PRECHECKS_END.length(), testCode.length());
		  }
		  return testCode;
	  }
	    
	  public String printMethodFromFile(final String methodName, final String path) {
			String code = "";
		  	try {
				var javaFile = new File(path);
				code = Files.readString(javaFile.toPath());

				String helper = code.substring(code.indexOf(methodName), code.length());
				while (helper.indexOf('\n') < helper.indexOf('{') ||
						helper.substring(0, helper.indexOf('{')).chars().filter(c -> c == ')').count() > 1) 
				{
					if (helper.indexOf(methodName) == -1) {
						// method doesn't exist in this file
						return "";
					}
					helper = helper.substring(helper.indexOf('\n'), helper.length());
					helper = helper.substring(helper.indexOf(methodName), helper.length());
				}
				int startIndex = code.length() - helper.length() + helper.indexOf('{');
				int closingBracketIndex = CodeHandler.findClosingBracketIndex(code, startIndex, '{');
				code = code.substring(code.length() - helper.length(), closingBracketIndex + 1);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return "\t" + code;
	  }
	  
	  private String getMethodFromTestFile(final String methodName, final String path) {
			String code = "";
		  	try {
				var javaFile = new File(path);
				code = Files.readString(javaFile.toPath());
				code = code.substring(code.indexOf(methodName), code.length());
				String tmp = code.substring(code.indexOf('\n'), code.length());
				int indexOfNextTest =  tmp.indexOf("@Test");
				if (indexOfNextTest == -1) {
					tmp = "";
					code = code.trim();
					code = code.substring(0, code.lastIndexOf('}'));
				} else {
					tmp = tmp.substring(0, tmp.indexOf("@Test"));
					code = "\t" + code.substring(0, tmp.lastIndexOf('\n') + code.indexOf('\n'));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return code;
	  }
	  
	  private String getVarName(String fullVarName) {
		  var splitter = fullVarName.split("\\s");
		  return splitter[splitter.length - 1];
	  }
	  
	  private String getLastExecutedBranch(final ITestResult result, final String curTestNr) {
		  final var allExecutedBranches = getAllExecutedBranches(result, curTestNr);
		  final var keys = allExecutedBranches.keySet().stream().toList();
		  int max = Integer.MIN_VALUE;
		  
		  
		  for (int i = 0; i < allExecutedBranches.keySet().size(); i++) {
			  var branchNr = Integer.parseInt(keys.get(i));
			  if (branchNr > max) {
				  max = branchNr;
			  }
		  }
		  return allExecutedBranches.get(max + "");
	  }
	  
	  private HashMap<String, String> getAllExecutedBranches(final ITestResult result, final String curTestNr) {
		  final var branches = new HashMap<String, String>();
		  final var testContext = result.getTestContext();
		  final var attrNames = testContext.getAttributeNames().stream().toList();
		  String attr;
		  String[] branchParts;
		  String testNr;
		  String branchNr;
		  
		  
		  for (int i = 0; i < attrNames.size(); i++) {
			  if (testContext.getAttribute(attrNames.get(i)) == null) {
				  continue;
			  }
			  attr = testContext.getAttribute(attrNames.get(i)).toString();
			  branchParts = attrNames.get(i).split("branch");
			  if (branchParts.length == 2) {
				  testNr = branchParts[0];
				  branchNr = branchParts[1];
				  if (curTestNr.equals(testNr)) {
					  branches.put(branchNr, attr);
				  }
			  }
		  }
		  return branches;
	  }
	  
	  private String loadCode(final String className) {	  
			var dir = new File(FileUtil.getProjectLocation(this.projectPath) + "\\tests");	
			if (!dir.exists()) {
				return "";
			}
			var javaFile = new File(FileUtil.getProjectLocation(this.projectPath) + "\\tests\\" + className + ".java");
			if (!javaFile.exists()){
				return "";
		    }
			try {
				var code = Files.readString(Paths.get(javaFile.getPath()));
				return code;
			} catch (IOException e) {}
			return "";
	  }
	  
	  private HashMap<String, String> getAllBranches(String className, String testName) {
		  var branches = new HashMap<String, String>();
		  final var code = loadCode(className);
		  final var methodCode = MethodHandler.getBySignature(code, MethodHandler.getMethodSignature(code, testName));
		  final Pattern p = Pattern.compile("\\w+branch\\w+");
		  final Matcher m = p.matcher(methodCode);
		  String[] branchParts;
		  String helper;
		  String branch;
		  
		  while (m.find()) {
			  branchParts = m.group(0).split("branch");
			  helper = methodCode.substring(m.start(), methodCode.length());
			  branch = helper.substring(helper.indexOf(", \"") + 3, helper.indexOf("\")"));
			  branches.put(branchParts[1], branch);
		  }
		  return branches;
	  }
	  	    
	  private boolean isAssignment(String methodCode, int position) {
		  if (methodCode.charAt(position) == '=') {
			  if (Arrays.asList('+', '-', '*', '/').contains(methodCode.charAt(position - 1))) {
				  return true;
			  }
			  if (Character.isLetter(methodCode.charAt(position - 1)) || methodCode.charAt(position - 1) == ' ') {
				  if (Character.isLetter(methodCode.charAt(position + 1)) || methodCode.charAt(position + 1) == ' ') {
					  return true;
				  }
			  }
		  }
		  return false;
	  }
	  
	  private String printMethodWithValues(String methodCode, InputDataTupel tupel) {
		  var varsWithValues = new HashMap<String, String>();
		  var extractedGlobalValues = tupel.getGlobalVarsValues();
		  var extractedParameterValues = tupel.getParametersValues();
		  var extractedParameterNames = tupel.getParameterNames();
		  int i = 0;
		  int offset = 0;
		  
		  //boolean addPrevVal = false;
		  //int offset2 = 0;
		  
		  
		  for (var gVar : this.globalVars) {
			  varsWithValues.put(getVarName(gVar), extractedGlobalValues.get(i));
			  i++;
		  }
		  i = 0;
		  for (var varName : extractedParameterNames) {
			  varsWithValues.put(varName, extractedParameterValues.get(i));
			  i++;
		  }
		  
		  for (var v : varsWithValues.keySet()) {
			  if (varsWithValues.get(v) == null) {
				  continue;
			  }
			  if (varsWithValues.get(v).contains(",")) {
				  // arrays can become quite big and that would clutter the error display
				  methodCode = "\t(" + v + " = " + varsWithValues.get(v) + ")\n" + methodCode;
				  continue;
			  }
			  
			  Pattern p = Pattern.compile(v + "[^" + Pattern.quote(""+INSERTED_SYMBOL.charAt(0)) + Pattern.quote(""+INSERTED_CLOSED_SYMBOL.charAt(0)) + "]");
			  Matcher m = p.matcher(methodCode);
			  
			  while (m.find()) {
				  int start = m.start();
				  offset = 0;
				  if (!matchesVariable(methodCode, m.start(), m.end(), v)) {
					  continue;
				  }
				  if (isAssignment(methodCode, m.end()-1) || isAssignment(methodCode, m.end())) {
					  // break when the variable gets assigned because it could have any value afterwards.
					  break;
				  }
				  if (methodCode.charAt(m.end()-1) == '(') {
					  continue;
				  }
				  if (methodCode.charAt(m.start() - 1) == '.') {
					  start -= 1;
					  offset += 1;
					  for (int j = m.start() - 2; j >= 0; j--) {
						  if (!Character.isAlphabetic(methodCode.charAt(j))) {
							  break;  
						  }
						  start--;
						  offset++;
					  }
				  }
				  methodCode = methodCode.substring(0, start) + v + INSERTED_SYMBOL + varsWithValues.get(v) 
				  + INSERTED_CLOSED_SYMBOL + methodCode.substring(start + v.length() + offset, methodCode.length());
				  m = p.matcher(methodCode);
			  }
		  }
		  return methodCode;
	  }
	  
	  private boolean matchesVariable(String code, int start, int end, String expected) {
		  if (start != 0 && Character.isJavaIdentifierPart(code.charAt(start - 1))) {
			  return false;
		  }
		  if (end != code.length() && Character.isJavaIdentifierPart(code.charAt(end))) {
			  return false;
		  }
		  return true;
	  }
	  
	  private String printConstructorCallWithNames(final InputDataTupel tupel, String className) {
		  String output = className + "(";
		  var names = tupel.getGlobalVarNames();
		  int counter = 0;
		  for (var name : names) {
			  output += tupel.getGlobalVarsValues().get(counter) + INSERTED_SYMBOL + name + INSERTED_CLOSED_SYMBOL + ", ";
			  counter++;
		  }
		  if (this.globalVars.size() > 0) {
			  output = output.substring(0, output.length() - 2);
		  }
		  output += ')';
		  return output;
	  }
	  
	  private void printWithInsertionsHighlighted (String toPrint) {
		  int nextIndex = toPrint.indexOf(INSERTED_SYMBOL);
		  if (nextIndex == -1) {
			  Console.println(toPrint);
			  return;
		  }
		  String curNoneHighlighted = toPrint.substring(0, toPrint.indexOf(INSERTED_SYMBOL));
		  String curHighlighted = toPrint.substring(nextIndex, toPrint.length());
		  curHighlighted = curHighlighted.substring(0, curHighlighted.indexOf(INSERTED_CLOSED_SYMBOL) + 1);
		  
		  
		  while (nextIndex != -1) {
			  Console.print(curNoneHighlighted, black);
			  Console.print(curHighlighted, blue);
			  toPrint = toPrint.substring(nextIndex + curHighlighted.indexOf(INSERTED_CLOSED_SYMBOL) + 1, toPrint.length());
			  nextIndex = toPrint.indexOf(INSERTED_SYMBOL);
			  if (nextIndex == -1) {
				  Console.print(toPrint + "\n", black);
				  break;
			  }
			  curNoneHighlighted = toPrint.substring(0, toPrint.indexOf(INSERTED_SYMBOL));
			  curHighlighted = toPrint.substring(nextIndex, toPrint.length());
			  curHighlighted = curHighlighted.substring(0, curHighlighted.indexOf(INSERTED_CLOSED_SYMBOL) + 1);
		  }
	  }
	  
	  private String addExecutedBranches(ITestResult result, String testeeName, String testName, String testNr) {
		  var allExecutedBranches = getAllExecutedBranches(result, testNr);
		  var allBranches = getAllBranches(testeeName, testName);
		  var lastBranch = getLastExecutedBranch(result, testNr);	  
		  this.allBranches = allBranches.values().stream().toList();	  
		  var newExecutedBranches = allExecutedBranches.values().stream().toList();
		  for (int i = 0; i < newExecutedBranches.size(); i++) {
			  if (!this.executedBranches.contains(newExecutedBranches.get(i))) {
				  this.executedBranches.add(newExecutedBranches.get(i));
			  }
		  }
		  return lastBranch;
	  }
	  
	  private int getLongestLineLen(final String text) {
		  final var lines = text.split("\\n");
		  int max = Integer.MIN_VALUE;
		  
		  for (var line : lines) {
			  if (line.length() > max) {
				  max = line.length();
			  }
		  }
		  return max;
	  }
	  
	  /**
	   * Invoked each time a test succeeds.
	   *
	   * @param result <code>ITestResult</code> containing information about the run test
	   * @see ITestResult#SUCCESS
	   */
	  @Override
	  public void onTestSuccess(ITestResult result) {
		  final var testMethodName = result.getName();	  
		  float testTime = (System.nanoTime() - testStartTime / 1000000);
		  dataCollector.addData(testMethodName, testTime); 
		  if (result.getTestContext().getAttribute("skip") != null) {
			  var consoleStr = "+ " + testMethodName + " +";
			  var bar = "+";
			  for (int i = 0; i < consoleStr.length() - 2; i++) bar += "=";
			  bar += "+";
			  Console.println(bar);
			  Console.println(consoleStr);
			  Console.println(bar);
			  Console.println(" > Generated data doesn't satisfy the precondition [" + result.getTestContext().getAttribute("skip") + "].", red);
			  Console.println();
			  result.setStatus(ITestResult.SKIP);
			  result.getTestContext().getSkippedTests().addResult(result);
			  result.getTestContext().removeAttribute("skip");
			  return;
		  } 
		  final var testNr = testMethodName.substring(testMethodName.lastIndexOf("Test") + 4, testMethodName.length());
		  addExecutedBranches(result, result.getTestClass().getName(), testMethodName, testNr);
	  }
	
	  /**
	   * Invoked each time a test fails.
	   *
	   * @param result <code>ITestResult</code> containing information about the run test
	   * @see ITestResult#FAILURE
	   */
	  @Override
	  public void onTestFailure(ITestResult result) {	 
		  var className =  result.getTestClass().getName();
		  var testMethodName = result.getName();
		  float testTime = (System.nanoTime() - testStartTime / 1000000);
		  dataCollector.addData(testMethodName, testTime); 
		  var methodName = testMethodName.substring(0, testMethodName.indexOf("Test"));
		  var testNr = testMethodName.substring(testMethodName.lastIndexOf("Test") + 4, testMethodName.length());
		  		  
		  String branch = addExecutedBranches(result, className, testMethodName, testNr);
		  InputDataTupel tupel = null;
		    	 	
		  var consoleStr = "+ " + testMethodName + " +";
		  var bar = "+";
		  for (int i = 0; i < consoleStr.length() - 2; i++) bar += "=";
		  bar += "+";
		  Console.println(bar);
		  Console.println(consoleStr);
		  Console.println(bar);
		  if (result.getTestContext().getAttribute("skip") != null) {
			  Console.println(" > Generated data doesn't satisfy the precondition [" + result.getTestContext().getAttribute("skip") + "].", red);
			  Console.println();
			  result.setStatus(ITestResult.SKIP);
			  result.getTestContext().getSkippedTests().addResult(result);
			  result.getTestContext().removeAttribute("skip");
			  return;
		  }
		  
		  // remove all attributes from previous tests
		  final var testContext = result.getTestContext();
		  final var attrCopy = new HashSet<String>();
		  String attrNr;
		  attrCopy.addAll(testContext.getAttributeNames());
		  for (var attr : attrCopy) {
			  attrNr = "";
			  for (int i = 0; i < attr.length(); i++) {
				  if (Character.isDigit(attr.charAt(i))) {
					  attrNr += attr.charAt(i);
				  } else {
					  break;
				  }
			  }
			  if (!attrNr.equals(testNr)) {
				  testContext.removeAttribute(attr);
			  }
		  }
		  String testMethodCode = getMethodFromTestFile(testMethodName,  FileUtil.getProjectLocation(this.projectPath) + "/tests/" + className + ".java");
		  // remove all testng helper lines
		  testMethodCode = removeHelperLines(testMethodCode);
		  Console.println(" > Failed with the error message:", red);
		  int errorLineNr = getErrorLineNr(result.getThrowable().getStackTrace(), className, testMethodName, testMethodCode);
		  String errorLine = getLine(testMethodCode, errorLineNr);
		  Console.println("\t" + "line " + (errorLineNr+1) + ": '" + errorLine + "':");
		  Console.println("\t\t" + result.getThrowable().getMessage());
		  Console.println();

		  for (var curMethod: this.inputDataTupels) {
			  if (curMethod.getName().equals(testMethodName)) {
				  tupel = curMethod.getInputDataTupel();
				  break;
			  }
		  }
		  Console.println(" > Failed test with variable names inserted in '" + INSERTED_SYMBOL + "..." + INSERTED_CLOSED_SYMBOL + "':", red);
		  className = className.substring(0, className.indexOf("Test"));
		  var constructorCallWithNames = printConstructorCallWithNames(tupel, className);
		  testMethodCode = testMethodCode.replace(className + tupel.getGlobalVarsRep(), constructorCallWithNames);
		  // insert values for variables
		  testMethodCode = printTestMethodWithValues(testMethodCode, result.getTestContext(), testNr);  
		  printWithInsertionsHighlighted("\t" + testMethodCode.trim());
		  Console.println();  
		  
		  String methodCode = printMethodFromFile(methodName, FileUtil.getProjectLocation(this.projectPath) + 
				  "/tests/" + className.substring(className.lastIndexOf('.') + 1,
						  className.length()).split("Test")[0] + ".java");
		  Console.println(" > Code of method " + methodName + " with values from test " + testMethodName + " inserted in '" + INSERTED_SYMBOL + "..." + INSERTED_CLOSED_SYMBOL + "':", red);
		  String methodCodeWithValues = printMethodWithValues(methodCode, tupel);
		  printWithInsertionsHighlighted(methodCodeWithValues);
		  Console.println();
		 // Console.println(LINE);  
	  }
	
	  /**
	   * Invoked after all the test methods belonging to the classes inside the &lt;test&gt; tag have run
	   * and all their Configuration methods have been called.
	   *
	   * @param context The test context
	   */
	  @Override
	  public void onFinish(ITestContext context) {
		  dataCollector.finish();
		  int passedTests = context.getPassedTests().size();
		  int failedTests = context.getFailedTests().size();
		  int skippedTests = context.getSkippedTests().size();
		  
		  if (failedTests == 0) {
			  Console.println(" > " + context.getName(), green);
		  } else {
			  Console.println(" > " + context.getName(), red);
		  }
		  
	      StringBuilder bufLog = new StringBuilder("+ Total tests run: ");
	      bufLog
	          .append(passedTests + failedTests)
	          .append(", Passes: ")
	          .append(passedTests)
	          .append(", Failures: ")
	          .append(failedTests )
	          .append(", Skips: ")
	          .append(skippedTests)
	      	  .append(" +");
	      int len = getLongestLineLen(bufLog.toString());
	      String line = "+";
	      for (int i = 0; i < len - 2; i++) {
	    	  line += "=";
	      }
	      line += "+";
	      Console.println(line);
	      Console.println(bufLog.toString());
	      Console.println(line);
	      
	      // print predicate coverage
	      int totalNumBranches = -1;
	      if (this.allBranches == null) {
	    	  totalNumBranches = 0;
	      } else {
	    	  totalNumBranches = this.allBranches.size();
	      }
	      int executedNumBranches = this.executedBranches.size();
	      double percentage = ((double)executedNumBranches / (double)totalNumBranches) * 100.0;

	      
	      if (failedTests == 0) {
	    	  Console.println(" > Predicate Coverage (Post Condition)", green);
	      } else {
	    	  Console.println(" > Predicate Coverage (Post Condition)", red);
	      }
	      if (totalNumBranches == 0) {
	    	  Console.println("\tCoverage: 1/1 = 100%");
	      } else {
		      Console.println("\tCoverage: " + executedNumBranches + "/" + totalNumBranches + " = " + percentage + "%");
		      Console.println();
		      Console.println(" > Branches that were executed at least once are highlighted in green:");
		      for (var branch : this.allBranches) {
		    	  if (this.executedBranches.contains(branch)) {
		    		  Console.println("\t" + branch, green);
		    	  } else {
		    		  Console.println("\t" + branch);
		    	  }
		      }
	      }
	      Console.println();
	  }

	@Override
	public void onTestSkipped(ITestResult result) {
		  float testTime = (System.nanoTime() - testStartTime / 1000000);
		  dataCollector.addData(result.getName(), testTime); 
	}
	
	@Override
	public void onStart(ITestContext context) {

	}
	  
	@Override
	public void onTestStart(ITestResult context) {
		this.testStartTime = System.nanoTime();
	}
	  
}
