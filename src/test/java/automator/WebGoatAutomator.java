package automator;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Created by alex on 06/11/15.
 */
public class WebGoatAutomator extends Automator {

    public static void main(String[] args) throws InterruptedException {
        WebGoatAutomator automator = new WebGoatAutomator();

        automator.driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

        for (int i = 0; i < 50; i++) {
            System.out.println(i + 1);
            long t = System.nanoTime();
            automator.startPage();
            automator.timers.add(System.nanoTime() - t);
            t = System.nanoTime();
            automator.signIn();
            if (i < 1) Thread.sleep(10000);
            automator.timers.add(System.nanoTime() - t);
            t = System.nanoTime();
            automator.logout();
            automator.timers.add(System.nanoTime() - t);
        }

        for (Long l : automator.timers) {
            System.out.println(new DecimalFormat("#.00").format((double)l/1000000));
        }

        automator.destroy();
    }

    private void startPage() throws InterruptedException {
        driver.navigate().to("https://fw-pretschner1.informatik.tu-muenchen.de:4046/WebGoat/login.mvc");
        Thread.sleep(2000);
    }

    private void signIn() throws InterruptedException {
        WebElement usernameField = driver.findElement(By.id("exampleInputEmail1"));
        usernameField.clear();
        usernameField.sendKeys("webgoat");
        WebElement passwordField = driver.findElement(By.id("exampleInputPassword1"));
        passwordField.clear();
        passwordField.sendKeys("webgoat");
        driver.findElement(By.xpath("//button[text()='Sign in'][1]")).click();
        Thread.sleep(4000);
    }

    private void logout() throws InterruptedException {
        driver.navigate().to("https://fw-pretschner1.informatik.tu-muenchen.de:4046/WebGoat/j_spring_security_logout");
        Thread.sleep(2000);
    }
}
