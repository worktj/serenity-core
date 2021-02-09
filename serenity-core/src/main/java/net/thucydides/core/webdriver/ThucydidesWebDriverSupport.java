package net.thucydides.core.webdriver;

import net.serenitybdd.core.environment.WebDriverConfiguredEnvironment;
import net.thucydides.core.annotations.TestCaseAnnotations;
import net.thucydides.core.pages.Pages;
import net.thucydides.core.steps.StepAnnotations;
import net.thucydides.core.steps.StepFactory;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.SessionId;

import java.util.Map;
import java.util.Optional;

/**
 * A utility class that provides services to initialize web testing and reporting-related fields in arbitrary objects.
 * It is designed to help integrate Thucydides into other testing tools such as Cucumber.
 */
public class ThucydidesWebDriverSupport {

    private static final ThreadLocal<WebdriverManager> webdriverManagerThreadLocal = new ThreadLocal<WebdriverManager>();
    private static final ThreadLocal<Pages> pagesThreadLocal = new ThreadLocal<Pages>();
    private static final ThreadLocal<StepFactory> stepFactoryThreadLocal = new ThreadLocal<StepFactory>();
    private static final ThreadLocal<String> defaultDriverType = new ThreadLocal<>();
    private static final ThreadLocal<String> defaultDriverOptions = new ThreadLocal<>();

    public static void initialize() {
        if (!webdriversInitialized()) {
            setWebdriverManager(newWebdriverManager());
        }
    }

    public static void overrideProperties(Map<String, String> propertyValues) {
        getWebdriverManager().overrideProperties(propertyValues);
    }
    public static void initialize(String requestedDriver) {

        WebdriverManager webdriverManagerForThisThread = newWebdriverManager();

        setWebdriverManager(webdriverManagerForThisThread);
        getWebdriverManager().overrideDefaultDriverType(requestedDriver);
    }

    private static WebdriverManager newWebdriverManager() {
        WebDriverFactory webDriverFactoryForThisThread = new WebDriverFactory();
        DriverConfiguration globalConfiguration = WebDriverConfiguredEnvironment.getDriverConfiguration();

        return new SerenityWebdriverManager(webDriverFactoryForThisThread, globalConfiguration);
    }

    public static void initialize(WebdriverManager webdriverManager, String requestedDriver) {
        setupWebdriverManager(webdriverManager, requestedDriver);
        initPagesObjectUsing(getDriver());
    }

    public static void reset() {
        if (webdriverManagerThreadLocal.get() != null) {
            webdriverManagerThreadLocal.get().reset();
        }
        webdriverManagerThreadLocal.remove();
        pagesThreadLocal.remove();
        stepFactoryThreadLocal.remove();
        defaultDriverType.remove();
    }

    public static boolean isInitialised() {
        return (webdriverManagerThreadLocal.get() != null);
    }

    private static boolean webdriversInitialized() {
        return (webdriverManagerThreadLocal.get() != null);
    }

//    private static void lazyInitalize() {
//        if (!webdriversInitialized()) {
//            initialize();
//        }
//    }

    public static void initializeFieldsIn(final Object testCase) {
        initialize();
        getDriver();
        injectDriverInto(testCase);
        injectAnnotatedPagesObjectInto(testCase);
    }

    public static StepFactory getStepFactory() {
        if (pagesThreadLocal.get() == null) {
            initPagesObjectUsing(getDriver());
        }
        return stepFactoryThreadLocal.get();
    }

    public static void useDefaultDriver(String driverName) {
        if (StringUtils.isNotEmpty(driverName)) {
            defaultDriverType.set(driverName);
        }
    }

    public static void useDriverOptions(String driverOptions) {
        defaultDriverOptions.set(driverOptions);
    }

    public static void clearDefaultDriver() {
        defaultDriverType.remove();
        defaultDriverOptions.remove();
        if (webdriverManagerThreadLocal.get() != null) {
            webdriverManagerThreadLocal.get().overrideDefaultDriverType("");
        }
    }

    public static void clearSession() {
        if (getWebdriverManager().getCurrentDriver() != null) {
            getWebdriverManager().getCurrentDriver().manage().deleteAllCookies();
        }
    }

    public static void useDriver(WebDriver driver) {
        initialize();
        getWebdriverManager().registerDriver(driver);
    }

    public static Optional<String> getDefaultDriverType() {
        return Optional.ofNullable(defaultDriverType.get());
    }

    public static Optional<String> getDefaultDriverOptions() {
        return Optional.ofNullable(defaultDriverOptions.get());
    }

    public static WebDriver getDriver() {

        initialize();

        if (webdriverManagerThreadLocal.get() == null) {
            return null;
        }

        WebDriver driver;

        if (defaultDriverType.get() != null) {
            driver = getWebdriverManager().withOptions(Optional.ofNullable(defaultDriverOptions.get()).orElse(""))
                                          .getWebdriver(defaultDriverType.get());
        } else {
            driver = (getWebdriverManager().getCurrentDriver() != null) ?
                    getWebdriverManager().getCurrentDriver() : getWebdriverManager().getWebdriver();
        }

        initPagesObjectUsing(driver);

        return driver;

    }

    public static void closeAllDrivers() {
        if (webdriversInitialized()) {
            getWebdriverManager().closeAllDrivers();
        }
    }

    public static void closeCurrentDrivers() {
        if (webdriversInitialized()) {
            getWebdriverManager().closeCurrentDrivers();
        }
    }

    //closeCurrentDrivers
    public static void closeDriver() {
        if (webdriversInitialized()) {
            getWebdriverManager().closeDriver();
        }
    }

    private static void setupWebdriverManager(WebdriverManager webdriverManager, String requestedDriver) {
        setWebdriverManager(webdriverManager);
        getWebdriverManager().overrideDefaultDriverType(requestedDriver);
    }

    private static void initStepFactoryUsing(final Pages pagesObject) {
        if (stepFactoryThreadLocal.get() != null) {
            stepFactoryThreadLocal.get().usePageFactory(pagesObject);
        } else {
            stepFactoryThreadLocal.set(StepFactory.getFactory().usingPages(pagesObject));
        }
    }

    public static WebdriverManager getWebdriverManager(WebDriverFactory webDriverFactory, DriverConfiguration configuration) {
        initialize(new SerenityWebdriverManager(webDriverFactory, configuration), "");
        return webdriverManagerThreadLocal.get();
    }

    public static WebdriverManager getWebdriverManager() {
//        lazyInitalize();
        if (webdriverManagerIsNotInstantiated()) {
            initialize();
        }
        return webdriverManagerThreadLocal.get();
    }

    private static void initPagesObjectUsing(final WebDriver driver) {
        pagesThreadLocal.set(new Pages(driver));
        initStepFactoryUsing(getPages());
    }

    public static Pages getPages() {
        if (pagesThreadLocal.get() == null) {
            initPagesObjectUsing(getDriver());
        }
        return pagesThreadLocal.get();
    }

    /**
     * Instantiate the @Managed-annotated WebDriver instance with current WebDriver.
     */
    protected static void injectDriverInto(final Object testCase) {
        TestCaseAnnotations.forTestCase(testCase).injectDrivers(getDriver(), getWebdriverManager());
    }

    /**
     * Instantiates the @ManagedPages-annotated Pages instance using current WebDriver.
     */
    protected static void injectAnnotatedPagesObjectInto(final Object testCase) {
        StepAnnotations.injector().injectOptionalAnnotatedPagesObjectInto(testCase, getPages());
    }

    public static <T extends WebDriver> T getProxiedDriver() {
        return (T) ((WebDriverFacade) getDriver()).getProxiedDriver();
}

    public static Class<? extends WebDriver> getDriverClass() {
        if (getDriver() instanceof WebDriverFacade) {
            return ((WebDriverFacade) getDriver()).getDriverClass();
        }
        return getDriver().getClass();
    }

    public static SessionId getSessionId() {
        return getWebdriverManager().getSessionId();

    }

    public static String getCurrentDriverName() {
        if (!webdriversInitialized()) {
            return "";
        }
        return getWebdriverManager().getCurrentDriverType();
    }

    public static String getDriversUsed() {
        if (webdriverManagerIsNotInstantiated()) {
            return "";
        }
        if (getWebdriverManager().getActiveDriverTypes().isEmpty()) {
            return "";
        }
        return getWebdriverManager().getActiveDriverTypes().get(0);
    }

    private static boolean webdriverManagerIsNotInstantiated() {
        return (webdriverManagerThreadLocal.get() == null);
    }

    public static boolean isDriverInstantiated() {
        return isInitialised() && getWebdriverManager().hasAnInstantiatedDriver();
    }

    private static void setWebdriverManager(WebdriverManager webdriverManager) {
        webdriverManagerThreadLocal.set(webdriverManager);
    }

    public static void clearStepLibraries() {
        if (stepFactoryThreadLocal.get() != null) {
            stepFactoryThreadLocal.get().reset();
        }
    }

}
