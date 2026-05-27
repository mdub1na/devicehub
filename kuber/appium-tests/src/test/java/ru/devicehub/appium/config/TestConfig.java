package ru.devicehub.appium.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;

public final class TestConfig {
    private static final String DEFAULT_APPIUM_SERVER_URL = "http://127.0.0.1:4723";
    private static final String DEFAULT_DEVICE_NAME = "Android";
    private static final String DEFAULT_BROWSER_NAME = "Chrome";
    private static final String DEFAULT_TARGET_URL = "https://example.com";
    private static final Duration DEFAULT_EXPLICIT_WAIT = Duration.ofSeconds(20);
    private static final Duration DEFAULT_NEW_COMMAND_TIMEOUT = Duration.ofSeconds(120);

    private TestConfig() {
    }

    public static URL appiumServerUrl() {
        try {
            return new URL(env("APPIUM_SERVER_URL", DEFAULT_APPIUM_SERVER_URL));
        }
        catch (MalformedURLException err) {
            throw new IllegalArgumentException("APPIUM_SERVER_URL is not a valid URL", err);
        }
    }

    public static String deviceName() {
        return env("ANDROID_DEVICE_NAME", DEFAULT_DEVICE_NAME);
    }

    public static String browserName() {
        return env("ANDROID_BROWSER_NAME", DEFAULT_BROWSER_NAME);
    }

    public static String targetUrl() {
        return env("TEST_TARGET_URL", DEFAULT_TARGET_URL);
    }

    public static String udid() {
        return env("ANDROID_UDID", "");
    }

    public static Duration explicitWait() {
        return DEFAULT_EXPLICIT_WAIT;
    }

    public static Duration newCommandTimeout() {
        return DEFAULT_NEW_COMMAND_TIMEOUT;
    }

    public static Path artifactsDir() {
        return Path.of(env("TEST_ARTIFACTS_DIR", "target/appium-artifacts"));
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
