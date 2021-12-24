package offercompass.scheduler;

import lombok.extern.slf4j.Slf4j;
import offercompass.model.PriceHistoryData;
import offercompass.utils.BrowserHelper;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@ConditionalOnProperty(name = "price.history.scheduler.enabled", havingValue = "true")
public class PriceHistoryScheduler {

    @Autowired
    private SchedulerHelper schedulerHelper;

    @Autowired
    private BrowserHelper browserHelper;

    @Scheduled(cron = "${price.history.scheduler.cron}")
    public void fetchProducts() {
        log.info("Price history scheduler fetching products...");
        WebDriver browser = browserHelper.openBrowser(true);
        try {
            //1. get list of latest products
            List<PriceHistoryData> priceHistoryDataList = schedulerHelper.getLatestProducts(browser);
            if (!priceHistoryDataList.isEmpty()) {

                //2. collect detailed product price history
                schedulerHelper.fetchDetailedPriceHistoryDetails(browser, priceHistoryDataList);

                //3. collect site details
                schedulerHelper.fetchSiteDetails(browser, priceHistoryDataList);

                //4. confirm good offer
                schedulerHelper.isGoodOffer(priceHistoryDataList);

                //5. email it
                schedulerHelper.sendEmail(priceHistoryDataList);
            }
        } catch (Exception ex) {
            log.info("Exception occurred. Exception is " + ex.getMessage());
        }
        browser.quit();
        log.info("Scheduler ran successfully....");
    }
}
