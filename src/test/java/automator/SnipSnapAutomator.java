package automator;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

/**
 * Created by alex on 05/11/15.
 */
public class SnipSnapAutomator extends Automator {

    public static void main(String[] args) {
        SnipSnapAutomator automator = new SnipSnapAutomator();

        automator.driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

        for (int i = 0; i < 5; i++) {
            System.out.println(i + 1);
            long t = System.nanoTime();
            automator.startPage();
            automator.timers.add(System.nanoTime() - t);
            t = System.nanoTime();
            automator.loginPage();
            automator.timers.add(System.nanoTime() - t);
            t = System.nanoTime();
            automator.login();
            automator.timers.add(System.nanoTime() - t);
            t = System.nanoTime();
            automator.clickPostComment();
            automator.timers.add(System.nanoTime() - t);
            t = System.nanoTime();
            automator.enterCommentAndClickPost();
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

    private void startPage() {
        driver.navigate().to("https://fw-pretschner1.informatik.tu-muenchen.de:4046/SnipSnap/space/start/");
    }

    private void loginPage() {
        driver.navigate().to("https://fw-pretschner1.informatik.tu-muenchen.de:4046/SnipSnap/exec/login.jsp");
    }

    private void login() {
        //driver.findElement(By.id("login")).sendKeys("admin");
        WebElement passwordField = driver.findElement(By.id("password"));
        //passwordField.sendKeys("admin");
        driver.findElement(By.name("Login")).click();
    }

    private void clickPostComment() {
        driver.findElement(By.linkText("post comment")).click();
    }

    private void enterCommentAndClickPost() {
        driver.findElement(By.name("content")).sendKeys("testComment");
        driver.findElement(By.name("save")).click();
    }

    private void logout() {
        driver.findElement(By.linkText("logoff")).click();
    }
}
