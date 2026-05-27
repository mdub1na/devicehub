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

## DeviceHub Capture Smoke

This smoke test checks only the DeviceHub autotests API lifecycle:

1. Capture one Android device.
2. Print captured device data.
3. Free the captured group in `finally`.

It does not start an Appium session yet.

```bash
cd kuber/appium-tests
DEVICEHUB_BASE_URL=https://<devicehub-host> \
DEVICEHUB_TOKEN=<access-token> \
./gradlew devicehubAndroidTest
```

`DEVICEHUB_BASE_URL` can be either the root DeviceHub URL or the `/api/v1` URL.

Optional DeviceHub variables:

- `DEVICEHUB_AMOUNT` defaults to `1`
- `DEVICEHUB_TIMEOUT_SECONDS` defaults to `600`
- `DEVICEHUB_NEED_AMOUNT` defaults to `true`
- `DEVICEHUB_RUN_NAME` defaults to a generated smoke run name
- `DEVICEHUB_DEVICE_TYPE` defaults to empty because current DeviceHub installations may use different internal platform field names
- `DEVICEHUB_ABI`, `DEVICEHUB_MODEL`, `DEVICEHUB_SDK`, `DEVICEHUB_VERSION` default to empty filters

## DeviceHub Appium Smoke

This smoke test captures one DeviceHub device, uses its `serial` as Appium `udid`, runs the Chrome smoke through Appium, and frees the DeviceHub group in `finally`.

```bash
cd kuber/appium-tests
DEVICEHUB_BASE_URL=https://<devicehub-host> \
DEVICEHUB_TOKEN=<access-token> \
APPIUM_SERVER_URL=https://<appium-grid-host> \
./gradlew devicehubAppiumAndroidTest
```
