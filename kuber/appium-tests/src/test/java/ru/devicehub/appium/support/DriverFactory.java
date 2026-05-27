package ru.devicehub.appium.support;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.remote.CapabilityType;
import ru.devicehub.appium.config.TestConfig;

public final class DriverFactory {
    private DriverFactory() {
    }

    public static AndroidDriver createAndroidBrowserDriver() {
        UiAutomator2Options options = new UiAutomator2Options()
            .setAutomationName("UiAutomator2")
            .setPlatformName("Android")
            .setDeviceName(TestConfig.deviceName())
            .setNewCommandTimeout(TestConfig.newCommandTimeout())
            .setNoReset(true);

        options.setCapability(CapabilityType.BROWSER_NAME, TestConfig.browserName());

        String udid = TestConfig.udid();
        if (!udid.isBlank()) {
            options.setUdid(udid);
        }

        return new AndroidDriver(TestConfig.appiumServerUrl(), options);
    }
}
