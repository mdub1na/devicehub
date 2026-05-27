# DeviceHub Appium Tests

Java smoke tests for Android devices. The first target is a local Android device; DeviceHub integration will be added after the local browser test is stable.

## Requirements

- JDK 17+
- Gradle Wrapper is included (`./gradlew`)
- Android platform tools (`adb`)
- Appium 3 with the UiAutomator2 driver
- Chrome installed on the Android device

## Local Run

Check that the device is visible and authorized:

```bash
adb devices -l
```

Expected state is `device`. If the state is `unauthorized`, unlock the phone and allow USB debugging.

Start Appium:

```bash
appium --use-drivers uiautomator2 --allow-insecure uiautomator2:chromedriver_autodownload
```

The `chromedriver_autodownload` feature lets Appium download a Chromedriver version compatible with Chrome on the Android device.

Run the smoke test:

```bash
cd kuber/appium-tests
APPIUM_SERVER_URL=http://127.0.0.1:4723 \
ANDROID_UDID=<adb-device-serial> \
./gradlew localAndroidTest
```

The `localAndroidTest` task always executes tests, even if Gradle thinks the task is up to date.

Optional variables:

- `ANDROID_DEVICE_NAME` defaults to `Android`
- `ANDROID_BROWSER_NAME` defaults to `Chrome`
- `TEST_TARGET_URL` defaults to `https://example.com`
- `TEST_ARTIFACTS_DIR` defaults to `target/appium-artifacts`

Failure artifacts are saved under `target/appium-artifacts`.
