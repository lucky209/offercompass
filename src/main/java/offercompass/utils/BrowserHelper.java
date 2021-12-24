package offercompass.utils;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class BrowserHelper {

    public WebDriver openBrowser(boolean isMaximize) {
        return this.openChromeBrowser(isMaximize);
    }

    private WebDriver openChromeBrowser(boolean isMaximize) {
        ChromeOptions chromeOptions = new ChromeOptions();
        if (isMaximize)
            chromeOptions.addArguments("start-maximized");//window-size=1358,727
        chromeOptions.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        chromeOptions.setExperimentalOption("useAutomationExtension", false);
        Map<String, Object> prefs = new HashMap<String, Object>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        chromeOptions.setExperimentalOption("prefs", prefs);
        chromeOptions.addArguments("--disable-blink-features");
        chromeOptions.setHeadless(true);
        WebDriverManager.chromedriver().setup();
        return new ChromeDriver(chromeOptions);
    }
}
