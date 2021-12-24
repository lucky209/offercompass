package offercompass.model;

import lombok.Data;

@Data
public class PriceHistoryData {
    private String phUrl;
    private String siteUrl;
    private String price;
    private String highestPrice;
    private String lowestPrice;
    private String dropChances;
    private String productName;
    private String sitePrice;
    private boolean isAvailable;
    private boolean isGoodOffer;
}
