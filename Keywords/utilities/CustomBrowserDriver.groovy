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
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.ProfilesIni;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.WebElement as WebElement
import java.nio.file.Paths
import java.nio.file.Path
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
	 * This method is used to instantiate a new driver based 
	 * on the user input from Profile
	 * @return
	 */
	@Keyword
	public static WebDriver createWebDriver() {

		WebDriver drv

		switch (getExecutedBrowser()) {

			case 'CHROME_DRIVER':
				System.setProperty("webdriver.chrome.driver", DriverFactory.getChromeDriverPath())
				ChromeOptions options = new ChromeOptions()
				Map<String, Object> chromePrefs = new HashMap<String, Object>()
				chromePrefs.put("download.prompt_for_download", false)
				options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.5060.134 Safari/18.1.1");
				options.setExperimentalOption("useAutomationExtension", false);
				options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
				drv  = new ChromeDriver(options)
				DriverFactory.changeWebDriver(drv)
				System.out.println("This is the value of dr from createwebdriver: "+drv)
				break;

			case 'HEADLESS_DRIVER':  //This is for headless Chrome driver
				System.setProperty("webdriver.chrome.driver", DriverFactory.getChromeDriverPath())
				ChromeOptions options = new ChromeOptions();
				options.addArguments("--headless");
				options.addArguments("--disable-dev-shm-usage");
				options.addArguments("--no-sandbox");
				options.addArguments("--disable-gpu");
				options.addArguments("--lang=en-US")
				options.addArguments("--timezone=EDT")
				options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.6778.85 Safari/18.1.1");
				DesiredCapabilities dc = new DesiredCapabilities();
				dc.setCapability(ChromeOptions.CAPABILITY, options);
				Map<String, Object> chromePrefs = new HashMap<String, Object>()
				chromePrefs.put("download.prompt_for_download", false)
				options.setExperimentalOption("prefs", chromePrefs)
				options.addArguments("--force-time-zone=America/Chicago");
				options.merge(dc);
				drv  = new ChromeDriver(options)
				DriverFactory.changeWebDriver(drv)
				System.out.println("This is the value of dr from createwebdriver: "+drv)
				break;

			case 'FIREFOX_DRIVER':
				System.setProperty("webdriver.gecko.driver", DriverFactory.getGeckoDriverPath())
				FirefoxProfile profile = new FirefoxProfile();
				FirefoxOptions opt = new FirefoxOptions();
				opt.setProfile(profile);
				drv  =  new FirefoxDriver(opt);
				DriverFactory.changeWebDriver(drv)
				System.out.println("This is the value of dr from createwebdriver: "+drv)
				break;

			case 'FIREFOX_HEADLESS_DRIVER':
				System.setProperty("webdriver.gecko.driver", DriverFactory.getGeckoDriverPath())
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
				String edgeDriverPath = DriverFactory.getEdgeDriverPath()
				WebUI.comment(">>> edgeDriverPath=${edgeDriverPath}")
				System.setProperty("webdriver.edge.driver", edgeDriverPath)
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
