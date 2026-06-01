package ru.devicehub.appium.support;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.remote.CapabilityType;
import ru.devicehub.appium.config.TestConfig;
import ru.devicehub.appium.devicehub.CapturedDevice;

public final class DriverFactory {
    private DriverFactory() {
    }

    public static AndroidDriver createAndroidBrowserDriver() {
        return createAndroidBrowserDriver(TestConfig.udid());
    }

    public static AndroidDriver createAndroidBrowserDriver(String udid) {
        UiAutomator2Options options = baseAndroidOptions(udid, TestConfig.appiumRemoteAdbHost());
        options.setCapability(CapabilityType.BROWSER_NAME, TestConfig.browserName());

        return new AndroidDriver(TestConfig.appiumServerUrl(), options);
    }

    public static AndroidDriver createAndroidBrowserDriver(CapturedDevice device) {
        UiAutomator2Options options = baseAndroidOptions(
            device.serial(),
            TestConfig.appiumRemoteAdbHost(device.provider())
        );
        options.setCapability(CapabilityType.BROWSER_NAME, TestConfig.browserName());

        return new AndroidDriver(TestConfig.appiumServerUrl(), options);
    }

    public static AndroidDriver createAndroidSettingsDriver(String udid) {
        UiAutomator2Options options = baseAndroidOptions(udid, TestConfig.appiumRemoteAdbHost())
            .setAppPackage("com.android.settings")
            .setAppActivity(".Settings");

        return new AndroidDriver(TestConfig.appiumServerUrl(), options);
    }

    public static AndroidDriver createAndroidSettingsDriver(CapturedDevice device) {
        UiAutomator2Options options = baseAndroidOptions(
            device.serial(),
            TestConfig.appiumRemoteAdbHost(device.provider())
        )
            .setAppPackage("com.android.settings")
            .setAppActivity(".Settings");

        return new AndroidDriver(TestConfig.appiumServerUrl(), options);
    }

    private static UiAutomator2Options baseAndroidOptions(String udid, String remoteAdbHost) {
        UiAutomator2Options options = new UiAutomator2Options()
            .setAutomationName("UiAutomator2")
            .setPlatformName("Android")
            .setDeviceName(TestConfig.deviceName())
            .setNewCommandTimeout(TestConfig.newCommandTimeout())
            .setNoReset(true);

        if (!remoteAdbHost.isBlank()) {
            options.setCapability("appium:remoteAdbHost", remoteAdbHost);
            options.setCapability("appium:adbPort", TestConfig.appiumAdbPort());
        }

        if (!udid.isBlank()) {
            options.setUdid(udid);
        }

        int systemPort = TestConfig.appiumSystemPort();
        if (systemPort > 0) {
            options.setSystemPort(systemPort);
        }

        return options;
    }
}
