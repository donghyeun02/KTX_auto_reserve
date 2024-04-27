package org.prac.korailreserve;

import java.time.LocalTime;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.NoSuchElementException;

@Service
public class TicketService {
    public String reserveTicket(String txtMember, String txtPwd, String txtGoStart, String txtGoEnd, String selMonth,
                                String selDay, Integer startHour, Integer startMin, Integer endHour, Integer endMin) {
        WebDriver driver = null;
        StringBuilder result = new StringBuilder();

        LocalTime startTime = LocalTime.of(startHour, startMin);
        LocalTime endTime = LocalTime.of(endHour, endMin);

        try {
            driver = initializeWebDriver();
            loginUser(driver, txtMember, txtPwd);
            navigateToReservationPage(driver);
            boolean ticketReserved = checkAndReserveTicket(driver, txtGoStart, txtGoEnd, selMonth, selDay, startHour, startTime,
                    endTime);

            if (ticketReserved) {
                result.append("Ticket Reserved !");
            } else {
                result.append("No more pages. Ticket not found.");
            }
        } catch (Exception e) {
            result.append("Error fetching data: ").append(e.getMessage());
        }
        return result.toString();
    }

    // Web driver initialize
    private WebDriver initializeWebDriver() {
        // Chrome Driver
//        WebDriverManager.chromedriver().setup();

        // Chrome option : headless
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");

        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver");

        WebDriver driver = new ChromeDriver(options);

        // Safari Drvier
        // System.setProperty("webdriver.safari.driver", "/System/Cryptexes/App/usr/bin/safaridriver");
        // WebDriver driver = new SafariDriver();
        driver.manage().window().setSize(new Dimension(1600, 1200));
        return driver;
    }

    // Login process
    private void loginUser(WebDriver driver, String txtMember, String txtPwd) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        driver.get("https://www.letskorail.com/korail/com/login.do");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("txtMember"))).sendKeys(txtMember);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("txtPwd"))).sendKeys(txtPwd);
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btn_login a"))).click();

        wait.until(ExpectedConditions.urlContains("https://www.letskorail.com/index.jsp"));
    }

    // To reserve Page
    private void navigateToReservationPage(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".first a"))).click();
    }

    // Ticket Reserve process
    private boolean checkAndReserveTicket(WebDriver driver, String txtGoStart, String txtGoEnd, String selMonth,
                                          String selDay, Integer startHour, LocalTime startTime, LocalTime endTime) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        boolean isTicketFound = false;

        setTicketSearchCriteria(driver, wait, txtGoStart, txtGoEnd, startHour, selMonth, selDay);
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btn_inq a"))).click();

        while (!isTicketFound) {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("tableResult")));

            isTicketFound = findAndReserveAvailableTicket(driver, wait, startTime, endTime);

            if (!isTicketFound) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btn_inq a"))).click();
            } else {
                isTicketFound = true;
            }
        }

        return isTicketFound;
    }

    // Ticket search criteria setting
    private void setTicketSearchCriteria(WebDriver driver, WebDriverWait wait, String txtGoStart, String txtGoEnd,
                                         Integer startHour, String selMonth, String selDay) {
        WebElement startElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("start")));
        startElement.clear();
        startElement.sendKeys(txtGoStart);

        WebElement endElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("get")));
        endElement.clear();
        endElement.sendKeys(txtGoEnd);
        new Select(driver.findElement(By.name("selGoMonth"))).selectByValue(selMonth);
        new Select(driver.findElement(By.name("selGoDay"))).selectByValue(selDay);
        new Select(driver.findElement(By.name("selGoHour"))).selectByValue(String.format("%02d", startHour));
    }

    // Check ticket available for reservation
    private boolean findAndReserveAvailableTicket(WebDriver driver, WebDriverWait wait, LocalTime startTime,
                                                  LocalTime endTime) {
        WebElement tbody = driver.findElement(By.xpath("//*[@id='tableResult']/tbody"));
        List<WebElement> rows = tbody.findElements(By.tagName("tr"));

        for (int i = 1; i < rows.size(); i++) {
            WebElement startTimeText = driver.findElement(
                    By.xpath("//*[@id='tableResult']/tbody/tr[" + i + "]/td[3]"));
            String startTimeTextContent = startTimeText.getText().trim();
            String timeString = startTimeTextContent.split("\\s+")[1];
            LocalTime startTimeTextLocal = LocalTime.parse(timeString);

            LocalTime currentTime = LocalTime.now();

            if (startTimeTextLocal.isAfter(startTime) && startTimeTextLocal.isBefore(endTime)) {
                long minutesDifference = Math.abs(Duration.between(currentTime, startTimeTextLocal).toMinutes());

                if (minutesDifference > 20) {
                    WebElement reservationButton = findReservationButton(driver, i);
                    if (reservationButton != null) {
                        reservationButton.click();
                        handleReservationModal(driver, wait);

                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Handling in case of reservation modal
    private void handleReservationModal(WebDriver driver, WebDriverWait wait) {
        WebElement modal = driver.findElement(By.id("korail-modal-traininfo"));
        if (modal.isDisplayed()) {
            WebElement iframe = modal.findElement(By.tagName("iframe"));
            driver.switchTo().frame(iframe);
            WebElement continueButton = wait.until(
                    ExpectedConditions.elementToBeClickable(By.cssSelector(".cont p.btn_c a")));
            continueButton.click();
            driver.switchTo().defaultContent();
        }
    }

    // Find btn to reserve
    private WebElement findReservationButton(WebDriver driver, int index) {
        try {
            WebElement regularSeatElement = driver.findElement(
                    By.xpath("//*[@id='tableResult']/tbody/tr[" + index + "]/td[6]"));
            WebElement specialSeatElement = driver.findElement(
                    By.xpath("//*[@id='tableResult']/tbody/tr[" + index + "]/td[5]"));

            WebElement regularSeatImg = regularSeatElement.findElement(By.tagName("img"));
            WebElement specialSeatImg = specialSeatElement.findElement(By.tagName("img"));

            String altRegular = regularSeatImg.getAttribute("alt");
            String altSpecial = specialSeatImg.getAttribute("alt");

            if (altRegular.equals("예약하기") && altSpecial.equals("예약하기")) {
                return regularSeatElement.findElement(By.tagName("a"));
            } else if (altRegular.equals("예약하기")) {
                return regularSeatElement.findElement(By.tagName("a"));
            } else if (altSpecial.equals("예약하기")) {
                return specialSeatElement.findElement(By.tagName("a"));
            } else {
                return null;
            }
        } catch (NoSuchElementException e) {
            return null;
        }
    }
}