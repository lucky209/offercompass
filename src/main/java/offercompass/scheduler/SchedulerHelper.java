package offercompass.scheduler;

import lombok.extern.slf4j.Slf4j;
import offercompass.model.PriceHistoryData;
import offercompass.utils.PriceHistoryConstants;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SchedulerHelper {

    @Value("${ph.drop.chances.threshold.value}")
    private int dropChancesThresholdVal;

    @Value("${to.mail.id}")
    private String[] toMailId;

    @Autowired
    private JavaMailSender javaMailSender;


    List<PriceHistoryData> getLatestProducts(WebDriver browser) {
        List<PriceHistoryData> priceHistoryDataList = new ArrayList<>();
            browser.get(PriceHistoryConstants.DEALS_URL);
            WebElement mainDiv = browser.findElement(By.id(PriceHistoryConstants.MAIN_PRODUCT_DIV_ID));
            //fetching product elements
            List<WebElement> productElements = mainDiv.findElements(By.cssSelector(
                    PriceHistoryConstants.SINGLE_PRODUCT_CSS_SELECTOR));
            productElements.forEach(element -> {
                PriceHistoryData priceHistoryData = new PriceHistoryData();
                priceHistoryData.setPhUrl(this.fetchPhUrl(element));
                priceHistoryData.setSiteUrl(this.fetchSiteUrl(element));
                priceHistoryData.setPrice(this.fetchPriceHistoryPrice(element));
                priceHistoryDataList.add(priceHistoryData);
            });
        log.info("Fetched {} LatestProducts", priceHistoryDataList.size());
        return priceHistoryDataList;
    }

    private String fetchPriceHistoryPrice(WebElement element) {
        return element
                .findElement(By.className(PriceHistoryConstants.PRICE_CLASS)).getText().trim();
    }

    private String fetchSiteUrl(WebElement element) {
        WebElement elementProductName = element.findElement(By.cssSelector(
                PriceHistoryConstants.PRODUCT_NAME_CSS_SELECTOR));
        return elementProductName.findElement(By.tagName(PriceHistoryConstants.TAG_ANCHOR))
                .getAttribute(PriceHistoryConstants.ATTRIBUTE_HREF);
    }

    private String fetchPhUrl(WebElement element) {
        return element != null ? element.findElement(By.className(
                PriceHistoryConstants.PRICE_HISTORY_URL_CLASS))
                .getAttribute(PriceHistoryConstants.ATTRIBUTE_HREF) : null;
    }

    void fetchDetailedPriceHistoryDetails(WebDriver browser,
                                         List<PriceHistoryData> priceHistoryDataList) {
        priceHistoryDataList.forEach(product -> {
            browser.get(product.getPhUrl());
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
                //ignore
            }
            product.setLowestPrice(this.fetchLowestPrice(browser));
            product.setHighestPrice(this.fetchHighestPrice(browser));
            product.setDropChances(this.fetchDropChances(browser));
        });
        log.info("Fetched PriceHistoryDetails successfully...");
    }

    private String fetchDropChances(WebDriver browser) {
        List<WebElement> elements = browser.findElements(By.id(PriceHistoryConstants.DROP_CHANCES_ID));
        if (!elements.isEmpty()) {
            return elements.get(0).getText().trim();
        }
        return null;
    }

    private String fetchHighestPrice(WebDriver browser) {
        List<WebElement> elements = browser.findElements(By.id(PriceHistoryConstants.HIGHEST_PRICE_ID));
        if (!elements.isEmpty()) {
            return elements.get(0).getText().trim();
        }
        log.info("Cannot fetch highest price for the url {}", browser.getCurrentUrl());
        return null;
    }

    private String fetchLowestPrice(WebDriver browser) {
        List<WebElement> elements = browser.findElements(By.id(PriceHistoryConstants.LOWEST_PRICE_ID));
        if (!elements.isEmpty()) {
            return elements.get(0).getText().trim();
        }
        log.info("Cannot fetch lowest price for the url {}", browser.getCurrentUrl());
        return null;
    }

    void fetchSiteDetails(WebDriver browser,
                                    List<PriceHistoryData> priceHistoryDataList) {
        priceHistoryDataList.forEach(product -> {
            if (product.getSiteUrl().contains(PriceHistoryConstants.FLIPKART_URL) ||
                    product.getSiteUrl().contains(PriceHistoryConstants.AMAZON_URL)) {
                browser.get(product.getSiteUrl());
                this.sleep();
                if (browser.getCurrentUrl().contains(PriceHistoryConstants.FLIPKART_URL)) {
                    product.setProductName(this.fetchFlipkartProductName(browser));
                    product.setSitePrice(this.fetchFlipkartPrice(browser));
                } else {
                    product.setProductName(this.fetchAmazonProductName(browser));
                    product.setSitePrice(this.fetchAmazonPrice(browser));
                }
                product.setAvailable(this.isAvailableToBuy(browser));
                product.setSiteUrl(browser.getCurrentUrl());
            }
        });
        log.info("Fetched SiteDetails successfully...");
        browser.quit();
    }

    private void sleep() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignored) {
            //ignore
        }
    }

    private String fetchFlipkartProductName(WebDriver browser) {
        List<WebElement> elements = browser.findElements(By.className(
                PriceHistoryConstants.FLIPKART_PRODUCT_NAME_CLASS));
        if (!elements.isEmpty()) {
            return elements.get(0).getText().trim();
        }
        log.info("Cannot fetch product name of the flipkart element for the url " + browser.getCurrentUrl());
        return null;
    }

    private String fetchAmazonProductName(WebDriver browser) {
        List<WebElement> elements = browser.findElements(By.id(PriceHistoryConstants.AMAZON_PRODUCT_NAME_ID));
        if (!elements.isEmpty()) {
            return elements.get(0).getText().trim();
        }
        log.info("Cannot fetch product name of the amazon element for the url {}", browser.getCurrentUrl());
        return null;
    }

    private String fetchFlipkartPrice(WebDriver browser) {
        List<WebElement> elements = browser.findElements(By.cssSelector(
                PriceHistoryConstants.FLIPKART_PRODUCT_PRICE_CSS_CLASS));
        if (!elements.isEmpty()) {
            return elements.get(0).getText().trim();
        }
        return null;
    }

    private String fetchAmazonPrice(WebDriver browser) {
        List<WebElement> elements = browser.findElements(By.id(PriceHistoryConstants.AMAZON_PRODUCT_PRICE_ID_1));
        if (!elements.isEmpty()) {
            return elements.get(0).getText().trim();
        } else {
            elements = browser.findElements(By.id(PriceHistoryConstants.AMAZON_PRODUCT_PRICE_ID_2));
            if (!elements.isEmpty()) {
                return elements.get(0).getText().trim();
            } else {
                elements = browser.findElements(By.id(PriceHistoryConstants.AMAZON_PRODUCT_PRICE_ID_3));
                if (!elements.isEmpty()) {
                    return elements.get(0).getText().trim();
                }
            }
        }
        return null;
    }

    void isGoodOffer(List<PriceHistoryData> priceHistoryDataList) {
        priceHistoryDataList.forEach(product -> {
            if (StringUtils.isNotBlank(product.getSitePrice())
                    && StringUtils.isNotBlank(product.getDropChances())
                    && product.isAvailable()) {
                Integer phPrice = this.convertStringRupeeToInteger(product.getPrice());
                Integer sitePrice = this.convertStringRupeeToInteger(product.getSitePrice());
                if (phPrice.equals(sitePrice)) {
                    int dropChances = Integer.parseInt(
                            product.getDropChances().trim().replace(
                                    PriceHistoryConstants.UTIL_PERCENTAGE, PriceHistoryConstants.UTIL_EMPTY_QUOTE));
                    if (dropChances <= dropChancesThresholdVal)
                        product.setGoodOffer(true);
                }
            }
        });
        log.info("Updated Good offer products successfully...");
    }

    private int convertStringRupeeToInteger(String rupee) {
        rupee = rupee
                .replace(PriceHistoryConstants.UTIL_RUPEE, PriceHistoryConstants.UTIL_EMPTY_QUOTE)
                .replaceAll(PriceHistoryConstants.UTIL_COMMA, PriceHistoryConstants.UTIL_EMPTY_QUOTE);
        if (rupee.contains(PriceHistoryConstants.UTIL_DOT)) {
            rupee = rupee.substring(0, rupee.indexOf(PriceHistoryConstants.UTIL_DOT)).trim();
        }
        if (rupee.contains(PriceHistoryConstants.UTIL_HYPHEN)) {
            rupee = rupee.substring(0, rupee.indexOf(PriceHistoryConstants.UTIL_HYPHEN)).trim();
        }
        return Integer.parseInt(rupee.trim());
    }

    private boolean isAvailableToBuy(WebDriver browser) {
        return !(browser.getPageSource().toLowerCase().contains("sold out") ||
                browser.getPageSource().toLowerCase().contains("currently unavailable"));
    }

    void sendEmail(List<PriceHistoryData> priceHistoryDataList) throws MessagingException {
        Map<String, String> emailMap = priceHistoryDataList.stream()
                .filter(PriceHistoryData::isGoodOffer)
                .collect(Collectors.toMap(
                        PriceHistoryData::getProductName,
                        PriceHistoryData::getSiteUrl,
                        (name1, name2) -> {
                            log.info("Duplicate key found...");
                            return name1;
                        }
                ));
        log.info("Mailing {} good product links", emailMap.size());
        if (!emailMap.isEmpty()) {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(toMailId);
            helper.setSubject(PriceHistoryConstants.MAIL_SUBJECT);
            helper.setText(this.constructMailBody(emailMap), true);
            javaMailSender.send(mimeMessage);
            log.info("Mail send successfully with {} product links", emailMap.size());
        } else {
            log.info("No good product found to mail at the moment");
        }
    }

    private String constructMailBody(Map<String, String> emailMap) {
        StringBuilder flipkartBody = new StringBuilder();
        StringBuilder amazonBody = new StringBuilder();
        StringBuilder body = new StringBuilder();
        AtomicInteger flipkartCount = new AtomicInteger(1);
        AtomicInteger amazonCount = new AtomicInteger(1);
        emailMap.forEach((productName, url)  -> {
            if (productName == null)
                productName = PriceHistoryConstants.MAIL_EMPTY_PRODUCT_NAME;
            if (url.contains("flipkart")) {
                flipkartBody.append(flipkartCount).append(PriceHistoryConstants.UTIL_DOT)
                        .append(PriceHistoryConstants.SINGLE_SPACE)
                        .append("<a href=")
                        .append(url).append(">")
                        .append(productName).append("</a><br/><br/>");
                flipkartCount.getAndIncrement();
            } else if (url.contains("amazon")) {
                amazonBody.append(amazonCount).append(PriceHistoryConstants.UTIL_DOT)
                        .append(PriceHistoryConstants.SINGLE_SPACE)
                        .append("<a href=")
                        .append(url).append(">")
                        .append(productName).append("</a><br/><br/>");
                amazonCount.getAndIncrement();
            }
        });
        log.info("Found {} flipkart products and {} amazon products", flipkartCount.decrementAndGet(), amazonCount.decrementAndGet());
        body.append("Flipkart Products : ").append("<br><br/>").append(flipkartBody)
                .append("<br><br/>").append("Amazon  Products : ").append(amazonBody);
        return PriceHistoryConstants.MAIL_HEADER + body;
    }
}
