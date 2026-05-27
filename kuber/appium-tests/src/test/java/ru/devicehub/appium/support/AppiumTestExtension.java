package ru.devicehub.appium.support;

import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import java.util.function.Supplier;

public final class AppiumTestExtension implements TestExecutionExceptionHandler, AfterEachCallback {
    private final Supplier<AndroidDriver> driverSupplier;

    public AppiumTestExtension(Supplier<AndroidDriver> driverSupplier) {
        this.driverSupplier = driverSupplier;
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        TestArtifacts.saveFailureArtifacts(driverSupplier.get(), context.getDisplayName());
        throw throwable;
    }

    @Override
    public void afterEach(ExtensionContext context) {
        AndroidDriver driver = driverSupplier.get();
        if (driver != null) {
            driver.quit();
        }
    }
}
