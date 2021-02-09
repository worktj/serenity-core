package net.thucydides.core.pages.integration;


import io.github.bonigarcia.wdm.WebDriverManager;
import net.serenitybdd.core.pages.ListOfWebElementFacades;

import net.thucydides.core.webdriver.javascript.ShadowDom;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.temporal.ChronoUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CheckingShadowDomContentWithTheFluentElementAPI {

    static WebDriver localDriver;
    static ShadowDomSitePage page;

    @BeforeClass
    public static void openStaticPage() {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions firefoxOptions = new FirefoxOptions();
        firefoxOptions.setHeadless(true);
        localDriver = new FirefoxDriver(firefoxOptions);
        page = new ShadowDomSitePage(localDriver);
        page.open();
    }

    @Before
    public void refreshPage() {
        page.getDriver().navigate().refresh();
        page.setWaitForTimeout(5000);
        page.setImplicitTimeout(2, ChronoUnit.SECONDS);
    }


    @Test
    public void should_find_slot_elements_in_shadow_dom() {
        assertThat(page.findAll(ShadowDom.of("[slot=title]")).get(0).getText(), is("Tab 1"));
        assertThat(page.findAll(ShadowDom.of("[slot=title]")).get(1).getText(), is("Tab 2"));
        assertThat(page.findAll(ShadowDom.of("[slot=title]")).get(2).getText(), is("Tab 3"));
    }

    @Test
    public void should_find_internal_elements_in_shadow_dom() {
        ListOfWebElementFacades allTabPanels = page.findAll(ShadowDom.of("[role=tabpanel]"));
        assertThat(allTabPanels.get(0).getTextContent(), is("content panel 1"));
        assertThat(allTabPanels.get(1).getTextContent(), is("content panel 2"));
        assertThat(allTabPanels.get(2).getTextContent(), is("content panel 3"));
    }

}
