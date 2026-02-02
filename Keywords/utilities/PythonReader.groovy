package utilities;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.poi.xssf.usermodel.XSSFCell
import com.kms.katalon.core.annotation.Keyword;
import com.kms.katalon.core.configuration.RunConfiguration;
import com.kms.katalon.core.util.KeywordUtil;
import internal.GlobalVariable

public class PythonReader {


	public static String appKey = GlobalVariable.AppKey;
	static ProcessBuilder processBuilder;

	/**
	 * This function reads and executes a Python file.
	 * @param pythonFileName The name of the Python file to execute.
	 */
	@Keyword
	public static void readFile(String pythonFileName) {

		String binPath = Utils.getPythonExecutablePath();
		String queryFilePath = GlobalVariable.InputExcel;
		String outputFilePath = Paths.get(RunConfiguration.getProjectDir(), "OutputFiles")
		String metaDataFilespath = Utils.getMetadataFilesPath()
		String pyPath = Utils.getPythonFilePath(pythonFileName);

		try {

			if(appKey.equals("INS") && pythonFileName.equals("DataSets.py")) {
				Utils.RESULT_TAB_NAME = "DatasetsTab"
				metaDataFilespath = metaDataFilespath + "/DataSetsTab";
			}
			// Create a process builder
			processBuilder = new ProcessBuilder(binPath, pyPath, queryFilePath, outputFilePath, metaDataFilespath, Utils.RESULT_TAB_NAME);

			processBuilder.redirectErrorStream(true);
			KeywordUtil.logInfo("Executing Python file: " + pythonFileName);

			// Start the process
			Process process = processBuilder.start();

			// Capture the output
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder output = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line).append("\n");
			}

			// Wait for the process to finish and get the exit code
			int exitCode = process.waitFor();

			// Print the output and handle the exit code
			if (exitCode == 0) {
				KeywordUtil.logInfo("Python script executed successfully.");
				KeywordUtil.logInfo("Script output: " + output.toString());
			} else {
				KeywordUtil.markFailed("Script execution failed with exit code: " + exitCode);
				KeywordUtil.markFailed("Script output: " + output.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			KeywordUtil.markFailed("An error occurred while executing the Python script: " + e.getMessage());
		}
	}

	/**
	 * This function reads and executes a Python file (Used for QuickRuns only.
	 * @param pythonFileName The name of the Python file to execute.
	 */
	@Keyword
	public static void readFileQuickRun(String pythonFileName, String tabName) {

		String binPath = Utils.getPythonExecutablePath();
		String queryFilePath = GlobalVariable.InputExcel;
		String outputFilePath = Paths.get(RunConfiguration.getProjectDir(), "OutputFiles")
		String metaDataFilespath = Utils.getMetadataFilesPath()
		String pyPath = Utils.getPythonFilePath(pythonFileName);

		try {

			if(pythonFileName.equals("statbar.py")) {
				KeywordUtil.logInfo("Starting process builder for statbar.py for QuickRun: " + pythonFileName);
				processBuilder = new ProcessBuilder(binPath, pyPath, queryFilePath, outputFilePath, metaDataFilespath);
			}else {
				Utils.RESULT_TAB_NAME = tabName;
				// Create a process builder
				processBuilder = new ProcessBuilder(binPath, pyPath, queryFilePath, outputFilePath, metaDataFilespath, Utils.RESULT_TAB_NAME);
			}

			processBuilder.redirectErrorStream(true);
			KeywordUtil.logInfo("Executing Python file: " + pythonFileName);

			// Start the process
			Process process = processBuilder.start();

			// Capture the output
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder output = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line).append("\n");
			}

			// Wait for the process to finish and get the exit code
			int exitCode = process.waitFor();

			// Print the output and handle the exit code
			if (exitCode == 0) {
				KeywordUtil.logInfo("Python script executed successfully.");
				KeywordUtil.logInfo("Script output: " + output.toString());
			} else {
				KeywordUtil.markFailed("Script execution failed with exit code: " + exitCode);
				KeywordUtil.markFailed("Script output: " + output.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			KeywordUtil.markFailed("An error occurred while executing the Python script: " + e.getMessage());
		}
	}
}
