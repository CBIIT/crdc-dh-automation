package utilities

import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import static com.kms.katalon.core.testobject.ObjectRepository.findWindowsObject
import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.checkpoint.Checkpoint
import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testcase.TestCase
import com.kms.katalon.core.testdata.TestData
import com.kms.katalon.core.testng.keyword.TestNGBuiltinKeywords as TestNGKW
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.windows.keyword.WindowsBuiltinKeywords as Windows
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions as ChromeOptions
import org.openqa.selenium.edge.EdgeDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.ie.InternetExplorerDriver
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.driver.chrome.ChromeDriverUtil
import com.kms.katalon.core.webui.driver.edge.EdgeDriverUtil
import com.kms.katalon.core.webui.driver.firefox.FirefoxDriverUtil
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.ProfilesIni;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.WebElement as WebElement
import java.nio.file.Paths
import java.nio.file.Path
import com.kms.katalon.core.configuration.RunConfiguration
import internal.GlobalVariable
import java.io.File;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;


public class CustomBrowserDriver {


	public static String getExecutedBrowser() {
		String exBrowser = DriverFactory.getExecutedBrowser().getName()
		GlobalVariable.execBrowser = exBrowser;
		System.out.println("This is the value of executed browser: "+GlobalVariable.execBrowser);
		return GlobalVariable.execBrowser;
	}

	/**
	 * Katalon 10 removed DriverFactory.getChromeDriverPath, getGeckoDriverPath, and getEdgeDriverPath.
	 * The replacement utils return a path when one is configured. When they do not, leave the
	 * property unset so Selenium Manager resolves a driver that matches the installed browser.
	 */
	private static void applyDriverPath(String propertyKey, String driverPath) {
		if (driverPath != null && !driverPath.trim().isEmpty()) {
			System.setProperty(propertyKey, driverPath)
			System.out.println("Driver path for " + propertyKey + ": " + driverPath)
		} else {
			System.clearProperty(propertyKey)
			System.out.println("No configured path for " + propertyKey + ". Selenium Manager will resolve the driver.")
		}
	}

	/**
	 * Instantiates a WebDriver from the execution profile (Chrome, headless Chrome, Firefox, etc.).
	 * Chrome prefs set the project Downloads folder, suppress the Save As prompt, and allow
	 * automatic downloads so repeated file downloads from the same origin (e.g. CLI package links) do not show Block/Allow.
	 * @return WebDriver instance registered with DriverFactory
	 */
	@Keyword
	public static WebDriver createWebDriver() {

		WebDriver drv

		switch (getExecutedBrowser()) {

			case 'CHROME_DRIVER':
				applyDriverPath("webdriver.chrome.driver", ChromeDriverUtil.getChromeDriverPath())
				ChromeOptions options = new ChromeOptions()
				File projectDownloadsDir = new File(RunConfiguration.getProjectDir(), "Downloads")
				projectDownloadsDir.mkdirs()
				Map<String, Object> chromePrefs = new HashMap<String, Object>()
				chromePrefs.put("download.prompt_for_download", false)
				chromePrefs.put("download.default_directory", projectDownloadsDir.getAbsolutePath())
				chromePrefs.put("profile.default_content_setting_values.automatic_downloads", 1)
				options.setExperimentalOption("prefs", chromePrefs)
				options.setExperimentalOption("useAutomationExtension", false);
				options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
				drv  = new ChromeDriver(options)
				DriverFactory.changeWebDriver(drv)
				System.out.println("This is the value of dr from createwebdriver: "+drv)
				break;

			case 'HEADLESS_DRIVER':  //This is for headless Chrome driver
				applyDriverPath("webdriver.chrome.driver", ChromeDriverUtil.getChromeDriverPath())
				ChromeOptions options = new ChromeOptions();
				File projectDownloadsDir = new File(RunConfiguration.getProjectDir(), "Downloads")
				projectDownloadsDir.mkdirs()
				options.addArguments("--headless");
				options.addArguments("--disable-dev-shm-usage");
				options.addArguments("--no-sandbox");
				options.addArguments("--disable-gpu");
				options.addArguments("--lang=en-US")
				options.addArguments("--timezone=EDT")
				DesiredCapabilities dc = new DesiredCapabilities();
				dc.setCapability(ChromeOptions.CAPABILITY, options);
				Map<String, Object> chromePrefs = new HashMap<String, Object>()
				chromePrefs.put("download.prompt_for_download", false)
				chromePrefs.put("download.default_directory", projectDownloadsDir.getAbsolutePath())
				chromePrefs.put("profile.default_content_setting_values.automatic_downloads", 1)
				options.setExperimentalOption("prefs", chromePrefs)
				options.addArguments("--force-time-zone=America/Chicago");
				options.merge(dc);
				drv  = new ChromeDriver(options)
				DriverFactory.changeWebDriver(drv)
				System.out.println("This is the value of dr from createwebdriver: "+drv)
				break;

			case 'FIREFOX_DRIVER':
				applyDriverPath("webdriver.gecko.driver", FirefoxDriverUtil.getDriverPath())
				FirefoxProfile profile = new FirefoxProfile();
				FirefoxOptions opt = new FirefoxOptions();
				opt.setProfile(profile);
				drv  =  new FirefoxDriver(opt);
				DriverFactory.changeWebDriver(drv)
				System.out.println("This is the value of dr from createwebdriver: "+drv)
				break;

			case 'FIREFOX_HEADLESS_DRIVER':
				applyDriverPath("webdriver.gecko.driver", FirefoxDriverUtil.getDriverPath())
				FirefoxOptions ffoptions = new FirefoxOptions();
				DesiredCapabilities desiredCap = DesiredCapabilities.firefox();
				desiredCap.setCapability("headless", true);
				ffoptions.addArguments("--headless");
				ffoptions.merge(desiredCap);
				drv = new FirefoxDriver(ffoptions);
				DriverFactory.changeWebDriver(drv)
				System.out.println("This is the value of dr from createwebdriver: "+drv)
				break;

			case 'SAFARI_DRIVER':
				SafariOptions options = new SafariOptions();
				options.setCapability("cleanSession", true);
				drv = new SafariDriver(options);
				DriverFactory.changeWebDriver(drv)
				System.out.println("This is the value of dr from createwebdriver: "+drv)
				break;

			case 'EDGE_DRIVER':
				String edgeDriverPath = EdgeDriverUtil.getEdgeChromiumDriverPath()
				WebUI.comment(">>> edgeDriverPath=${edgeDriverPath}")
				applyDriverPath("webdriver.edge.driver", edgeDriverPath)
				drv  = new EdgeDriver()
				DriverFactory.changeWebDriver(drv)
				System.out.println("This is the value of dr from createwebdriver : "+drv)
				break;

			default:
				throw new IllegalStateException("Unsupported browser type: ${executedBrowser}")
		}
		return drv
	}
}
