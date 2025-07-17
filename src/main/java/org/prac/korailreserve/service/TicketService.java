package org.prac.korailreserve;

import java.time.LocalTime;
import java.util.List;
import java.time.Duration;
import java.util.NoSuchElementException;

import org.prac.korailreserve.util.SmsSender;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;

import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TicketService {
//    @Autowired
//    private SmsSender smsSender;

    public String reserveTicket(String phoneNumber, String txtMember, String txtPwd, String txtGoStart, String txtGoEnd, String selMonth,
                                String selDay, Integer startHour, Integer startMin, Integer endHour, Integer endMin) {
        WebDriver driver = null;
        StringBuilder result = new StringBuilder();

        LocalTime startTime = LocalTime.of(startHour, startMin);
        LocalTime endTime = LocalTime.of(endHour, endMin);

        try {
            System.out.println(txtMember + " : start !");

            driver = initializeWebDriver();
            loginUser(driver, txtMember, txtPwd);
            navigateToReservationPage(driver);
            boolean ticketReserved = checkAndReserveTicket(driver, txtGoStart, txtGoEnd, selMonth, selDay, startHour, startTime,
                    endTime);

            if (ticketReserved) {
                System.out.println(txtMember + " Ticket Reserved !");
                result.append("Ticket Reserved !");
//                smsSender.sendSms(phoneNumber, "[코레일 예약 성공]\n" + "회원 번호 : " + txtMember + "\n" + "코레일 앱에서 결제");
            } else {
                result.append("No more pages. Ticket not found.");
            }
        } catch (Exception e) {
            System.out.println(txtMember + " / error : " + e.getMessage());
            result.append("Error fetching data: ").append(e.getMessage());

            try {
                Thread.sleep(1000);
            } catch (InterruptedException err1) {
                System.out.println("InterruptedException occurred while sleeping: " + err1.getMessage());
            }

            if (driver != null)  {
                driver.quit();
            }
        } finally {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException err2) {
                System.out.println("InterruptedException occurred while sleeping: " + err2.getMessage());
            }

            if (driver != null) {
                driver.quit();
            }
        }

        return result.toString();
    }

    // Web driver initialize
    private WebDriver initializeWebDriver() {
        // Chrome option : headless
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");

        WebDriver driver = new ChromeDriver(options);

        // Safari Driver
//        System.setProperty("webdriver.safari.driver", "/System/Cryptexes/App/usr/bin/safaridriver");
//        WebDriver driver = new SafariDriver();

//        driver.manage().window().setSize(new Dimension(1600, 1200));
        return driver;
    }

    // Login process
    private void loginUser(WebDriver driver, String txtMember, String txtPwd) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        driver.get("https://www.korail.com/ticket/login");

        handleKorailPopup(driver);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("id"))).sendKeys(txtMember);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("password"))).sendKeys(txtPwd);
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btn_bn-depblue"))).click();
    }

    // To reserve Page
    private void navigateToReservationPage(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        handleKorailPopup(driver);
        try {
            WebElement reservationLink = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("a[href='/ticket/search/general']"))
            );

            ((org.openqa.selenium.JavascriptExecutor) driver)
                    .executeScript("arguments[0].scrollIntoView({block: 'center'});", reservationLink);

            Thread.sleep(100);

            ((org.openqa.selenium.JavascriptExecutor) driver)
                    .executeScript("arguments[0].click();", reservationLink);

        } catch (Exception e) {
            throw new RuntimeException("예약 페이지 진입 실패", e);
        }
    }

    private void handleKorailPopup(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            WebElement popup = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector(".layerWrap.emer_pop")));

            WebElement closeButton = popup.findElement(By.cssSelector(".btn_by-blue.btn_pop-close"));
            if (closeButton.isDisplayed()) {
                closeButton.click();
            }
        } catch (Exception e) {
            System.out.println("팝업 없음 또는 이미 닫힘");
        }
    }

    // Ticket Reserve process
    private boolean checkAndReserveTicket(WebDriver driver, String txtGoStart, String txtGoEnd, String selMonth,
                                          String selDay, Integer startHour, LocalTime startTime, LocalTime endTime) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        int retryCount = 0;
        int maxRetries = 50; // 필요한 경우 늘리거나 제거

        boolean isTicketFound = false;

        setTicketSearchCriteria(driver, wait, txtGoStart, txtGoEnd, startHour, selMonth, selDay);
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btn_lookup"))).click();

        while (!isTicketFound && retryCount < maxRetries) {
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".tckList.clear")));

                isTicketFound = findAndReserveAvailableTicket(driver, wait, startTime, endTime);

                if (!isTicketFound) {
                    Thread.sleep(3000);
                    driver.navigate().refresh();

                    wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(".tckList.clear")));

                    retryCount++;
                }

            } catch (Exception e) {
                System.out.println("새로고침 중 예외 발생: " + e.getMessage());
                retryCount++;
            }
        }

        if (!isTicketFound) {
            System.out.println("조건에 맞는 열차를 찾지 못했습니다.");
        }

        return isTicketFound;
    }

    // Ticket search criteria setting
    private void setTicketSearchCriteria(WebDriver driver, WebDriverWait wait, String txtGoStart, String txtGoEnd,
                                         Integer startHour, String selMonth, String selDay) {
        selectStation(driver, wait, txtGoStart, true);
        selectStation(driver, wait, txtGoEnd, false);

        setDate(driver, wait, selMonth, selDay, startHour);
    }

    private void selectStation(WebDriver driver, WebDriverWait wait, String station, boolean isDeparture) {
        String btnSelector = isDeparture ? ".start .btn_pop" : ".end .btn_pop";

        try {
            WebElement openPopupButton = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(btnSelector)));

            ((org.openqa.selenium.JavascriptExecutor) driver)
                    .executeScript("arguments[0].scrollIntoView({block: 'center'});", openPopupButton);

            Thread.sleep(300);

            ((org.openqa.selenium.JavascriptExecutor) driver)
                    .executeScript("arguments[0].click();", openPopupButton);

        } catch (Exception e) {
            throw new RuntimeException("출발/도착역 팝업 열기 실패: " + e.getMessage(), e);
        }

        List<WebElement> stationLinks = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector(".list_wrap .ch_tag a")));

        boolean found = false;
        for (WebElement link : stationLinks) {
            if (link.getText().trim().equals(station)) {
                link.click();
                found = true;
                break;
            }
        }

        if (!found) {
            throw new RuntimeException("역 이름 '" + station + "' 을(를) 리스트에서 찾지 못했습니다.");
        }

        String inputSelector = isDeparture ? "input[name='txtGoStart']" : "input[name='txtGoEnd']";
        wait.until(ExpectedConditions.attributeToBeNotEmpty(driver.findElement(By.cssSelector(inputSelector)), "value"));
        wait.until(ExpectedConditions.attributeContains(By.cssSelector(inputSelector), "value", station));
    }

    private void setDate(WebDriver driver, WebDriverWait wait, String selMonth, String selDay, int startHour) {
        WebElement openPopupButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btn_pop.btn_d-day")));
        openPopupButton.click();

        List<WebElement> days = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector("table .day")));

        for (WebElement day : days) {
            String text = day.getText().trim();
            WebElement parent = day.findElement(By.xpath("./ancestor::a"));
            String disabled = parent.getAttribute("aria-disabled");

            if (text.equals(selDay.replaceFirst("^0+(?!$)", "")) && "false".equals(disabled)) {
                parent.click();
                break;
            }
        }
        setHour(driver, wait, selMonth, selDay, startHour);

        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btn_wrap .btn_bn-blue"))).click();
    }

    // 시간 선택만 ,,
    private void setHour(WebDriver driver, WebDriverWait wait, String selMonth, String selDay, int startHour) {
        String hourString = String.format("%02d시", startHour);

        By hourSelector = By.cssSelector(".timeSelect li a");

        int maxScrollTry = 10;
        boolean hourFound = false;

        for (int i = 0; i < maxScrollTry; i++) {
            List<WebElement> hourElements = driver.findElements(hourSelector);

            for (WebElement hour : hourElements) {
                String text = hour.getText().trim();
                String isDisabled = hour.getAttribute("aria-disabled");

                if (text.equals(hourString) && "false".equals(isDisabled)) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", hour);
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", hour);
                    System.out.println("시간 선택 완료: " + text);
                    hourFound = true;
                    break;
                }
            }

            if (hourFound) break;

            try {
                WebElement nextBtn = driver.findElement(By.cssSelector(".timeSelect .slick-next"));
                if (nextBtn.isDisplayed() && nextBtn.isEnabled()) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextBtn);
                    Thread.sleep(300);
                } else {
                    break;
                }
            } catch (NoSuchElementException e) {
                System.out.println("슬라이드 버튼 없음");
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (!hourFound) {
            System.out.println("시간(" + hourString + ")을 찾지 못함. 선택되지 않음");
        }
    }

    private boolean clickKtxAndReserve(WebDriver driver, WebDriverWait wait, WebElement train) {
        try {
            WebElement trainTypeSpan = train.findElement(By.cssSelector(".flag_wrap span.train_ktx_ticket"));
            if (trainTypeSpan == null || !trainTypeSpan.isDisplayed()) return false;

            WebElement generalBox = train.findElement(By.cssSelector(".price_box.gen"));
            WebElement openDetailBtn = generalBox.findElement(By.tagName("a"));

            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", openDetailBtn);
            openDetailBtn.click();

            Thread.sleep(500);

            WebElement reserveBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector(".ticket_reserv_wrap .btn_bn-blue02.reservbtn")
            ));

            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", reserveBtn);
            Thread.sleep(300);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", reserveBtn);

            handleNoticeModal(driver, wait);

            return true;

        } catch (NoSuchElementException e) {
            System.out.println(e.getMessage());
            return false;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            return false;
        }
    }

    // Check ticket available for reservation
    private boolean findAndReserveAvailableTicket(WebDriver driver, WebDriverWait wait,
                                                  LocalTime startTime, LocalTime endTime) {
        List<WebElement> trainList = driver.findElements(By.cssSelector(".tckList.clear"));

        for (WebElement train : trainList) {
            try {
                WebElement timeSpan = train.findElement(By.cssSelector(".data_box h3 span:last-child"));
                String rawTime = timeSpan.getText().trim();
                String cleanedTime = rawTime.replaceAll("[()\\s]", "");
                String departTimeStr = cleanedTime.split("~")[0];

                LocalTime departTime = LocalTime.parse(departTimeStr);

                if (departTime.isBefore(startTime) || departTime.isAfter(endTime)) {
                    continue;
                }

                if (clickKtxAndReserve(driver, wait, train)) {
                    return true;
                }

            } catch (NoSuchElementException e) {
                System.out.println(e.getMessage());
                continue;
            } catch (Exception e) {
                System.out.println( e.getMessage());
                continue;
            }
        }

        return false;
    }

    // Handling in case of reservation modal
    private void handleNoticeModal(WebDriver driver, WebDriverWait wait) {
        try {
            WebElement overlay = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector(".ReactModalPortal .ReactModal__Overlay"))
            );

            if (overlay.isDisplayed()) {
                WebElement confirmButton = overlay.findElement(By.cssSelector(".btn_bn-blue.btn_pop-close"));
                if (confirmButton.isDisplayed() && confirmButton.isEnabled()) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", confirmButton);
                    Thread.sleep(300);
                } else {
                    System.out.println("확인 버튼 비활성화 상태");
                }
            } else {
                System.out.println("React 모달이 표시 안됨");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}