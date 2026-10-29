package org.exampleTests;

import com.microsoft.playwright.*;
import org.testng.Assert;
import org.testng.annotations.*;
import org.example.SecurityTable;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ReadOnlyTests {

    public static final String TARGET_VARIANCE = "target Variance";
    public static final String SECURITY = "Security";
    public static final String UNIT_PRICE = "unit price";
    public static final String OUTPUT_SHARES = "Output Number of shares";
    public static final String TARGET = "target%";
    public static final String CURRENT = "current%";

    private Playwright playwright;
    private Browser browser;
    private final int totalAssetValue = 100000;   //Assumption the total Asset value has been loaded in from larger account Page object.
    private SecurityTable securitiesTable;

    @BeforeClass
    public void setUp(){
        this.playwright = Playwright.create();
        this.browser = this.playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
        Page page = browser.newPage();

        page.navigate("https://www.testApplication.ie");
        page.getByText("ABC - Account Tab").click();

        Locator outputTable = page.locator("#OutputTable");
        this.securitiesTable = new SecurityTable(outputTable);
    }

    @AfterClass(alwaysRun = true)
    public void tearDown(){
        browser.close();
        playwright.close();
    }

    @Test(priority = 1)
    public void validateTargetAssetValues(){
        int value = this.securitiesTable.sumColumnValues(TARGET);
        Assert.assertEquals(value, 100, "Target Asset value should sum to 100%");
    }

    @Test(priority = 2)
    public void validateCurrentAssetValues(){
        int value = this.securitiesTable.sumColumnValues(CURRENT);
        Assert.assertEquals(value, 100, "Current Asset value should sum to 100%");
    }

    @Test(priority = 3)
    public void validateTargetVariance(){
        String[] securities = this.securitiesTable.getColumnValueForAllRows(this.securitiesTable.getColumnIndex(SECURITY));
        int varianceColumnIndex = this.securitiesTable.getColumnIndex(TARGET_VARIANCE);
        int targetColumnIndex = this.securitiesTable.getColumnIndex(TARGET);
        int currentColumnIndex = this.securitiesTable.getColumnIndex(CURRENT);
        for(String security:securities){
            Locator row  = this.securitiesTable.getRowBySecurity(security);
            BigDecimal targetNumber = new BigDecimal(row.nth(targetColumnIndex).innerText().strip());
            BigDecimal currentNumber = new BigDecimal(row.nth(currentColumnIndex).innerText().strip());
            BigDecimal varianceNumber = new BigDecimal(row.nth(varianceColumnIndex).innerText().strip());
            BigDecimal calculatedVariance = currentNumber.subtract(targetNumber);

            Assert.assertEquals(calculatedVariance.compareTo(varianceNumber), 0, "for "+security+
                    " the Target Variance did not match test. we expected "
                    +calculatedVariance+" and found "+varianceNumber);
        }
    }

    @Test(priority = 4)
    //I assume that fractional shares are the correct output as the system displayed is for reporting on the portfolio not modifying the holdings.
    public void validateFractionalShareValuation(){
        String[] securities = this.securitiesTable.getColumnValueForAllRows(this.securitiesTable.getColumnIndex(SECURITY));
        int varianceColumnIndex = this.securitiesTable.getColumnIndex(TARGET_VARIANCE);
        int unitPriceColumnIndex = this.securitiesTable.getColumnIndex(UNIT_PRICE);
        int sharesColumnIndex = this.securitiesTable.getColumnIndex(OUTPUT_SHARES);
        for(String security:securities){
            Locator row  = this.securitiesTable.getRowBySecurity(security);
            BigDecimal varianceNumber = new BigDecimal(row.nth(varianceColumnIndex).innerText().strip());
            BigDecimal unitPriceNumber = new BigDecimal(row.nth(unitPriceColumnIndex).innerText().strip());
            String outputNumber = row.nth(sharesColumnIndex).innerText().trim();

            //Calculate expected Shares to buy or sell.
            BigDecimal cashValueOfDifference = varianceNumber.abs().divide(BigDecimal.valueOf(100)).multiply(BigDecimal.valueOf(this.totalAssetValue));
            BigDecimal calculatedNumberOfShares = cashValueOfDifference.divide(unitPriceNumber, 2, RoundingMode.HALF_UP);
            BigDecimal shareCount = new BigDecimal(outputNumber.split("\\s+")[0]);

            Assert.assertEquals(calculatedNumberOfShares, shareCount, "for "+security+
                    " the number of shares to buy or sell did not match the test, we expected "
                    +calculatedNumberOfShares+" and found "+shareCount);
            if(0!=shareCount.signum() && 0>varianceNumber.signum()){
                Assert.assertEquals(outputNumber.split("\\s+")[1], "-ve",
                        "Buy signal not recorded in the Application output for the security " + security);
            }
            else if(0!=shareCount.signum() && 0<varianceNumber.signum()){
                Assert.assertEquals(outputNumber.split("\\s+")[1], "+ve",
                        "Sell signal not recorded in the Application output for the security " + security);
            }
            else if(0==shareCount.signum()){
                // no buy or sell instruction should be present if variance is 0 (no action required.)
                Assert.assertFalse(outputNumber.contains("ve"));
            }
        }
    }

}