package automator;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;

/**
 * Created by alex on 27/09/15.
 */
public class DemirbasAutomator extends Automator {

    public static void main(String[] args) {
        DemirbasAutomator automator = new DemirbasAutomator();

        automator.driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

        for (int i = 0; i < 50; i++) {
            System.out.println(i + 1);
            long t = System.nanoTime();
            automator.startPage();
            automator.timers.add(System.nanoTime() - t);
            t = System.nanoTime();
            automator.login();
            automator.timers.add(System.nanoTime() - t);
            t = System.nanoTime();
            automator.startPage();
            automator.timers.add(System.nanoTime() - t);
            t = System.nanoTime();
            automator.logout();
            automator.timers.add(System.nanoTime() - t);
            t = System.nanoTime();
            automator.startPage();
            automator.timers.add(System.nanoTime() - t);
        }

        for (Long l : automator.timers) {
            System.out.println(new DecimalFormat("#.00").format((double)l/1000000));
        }

        automator.destroy();
    }

    private void startPage() {
        driver.navigate().to("https://fw-pretschner1.informatik.tu-muenchen.de:4046/Demirbas/");
    }

    private void login() {
        // get login button image and then its enclosing <a> tag with href
        String loginAddress = driver.findElement(By.xpath("//img[contains(@src,'fb-login.png')]")).findElement(By.xpath("parent::*")).getAttribute("href");
        driver.navigate().to(loginAddress);

//        driver.findElement(By.xpath("//img[contains(@src,'fb-login.png')]")).click();
//        (new WebDriverWait(driver, 30)).until((Predicate<WebDriver>) d -> driver.findElement(By.xpath("//img[contains(@src,'fb-logout.gif')]")) != null);
    }

    private void logout() {
        String logoutAddress = driver.findElement(By.xpath("//img[contains(@src,'fb-logout.gif')]")).findElement(By.xpath("parent::*")).getAttribute("href");
        driver.navigate().to(logoutAddress);
//        driver.findElement(By.xpath("//img[contains(@src,'fb-logout.gif')]")).click();
//        (new WebDriverWait(driver, 30)).until((Predicate<WebDriver>) d -> driver.findElement(By.xpath("//img[contains(@src,'fb-login.png')]")) != null);
    }
}
