package utils;

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chromium.ChromiumDriver;

/**
 * Simulates network connectivity loss via Chrome DevTools Protocol
 * (Network.emulateNetworkConditions). Uses the raw executeCdpCommand API
 * rather than the typed org.openqa.selenium.devtools.vNNN classes, since
 * those are pinned to a specific Chrome version and the runner's Chrome
 * build has already been observed to be ahead of the bundled devtools
 * artifact (see "Unable to find CDP implementation matching" in the driver
 * startup logs) - the raw command path avoids that version-negotiation
 * entirely.
 */
public class NetworkUtil {

	private NetworkUtil() {
	}

	public static void setNetworkOffline(WebDriver driver, boolean offline) {
		if (!(driver instanceof ChromiumDriver)) {
			throw new UnsupportedOperationException(
					"Network condition emulation is only supported on Chromium-based drivers");
		}
		ChromiumDriver chromiumDriver = (ChromiumDriver) driver;

		chromiumDriver.executeCdpCommand("Network.enable", new HashMap<>());

		Map<String, Object> params = new HashMap<>();
		params.put("offline", offline);
		params.put("latency", 0);
		params.put("downloadThroughput", offline ? 0 : -1);
		params.put("uploadThroughput", offline ? 0 : -1);
		chromiumDriver.executeCdpCommand("Network.emulateNetworkConditions", params);
	}

	/**
	 * Overrides the User-Agent header/navigator.userAgent for subsequent
	 * requests/navigations, to trigger an "unsupported browser" state on
	 * screens that gate on it. The exact detection mechanism used by the
	 * (external, not vendored in this repo) identity-verification app that
	 * renders the browser-compatibility screen isn't confirmed - this assumes
	 * UA sniffing, the most common approach, but if that app instead relies on
	 * feature detection (e.g. missing WebRTC/getUserMedia APIs) this override
	 * alone won't trigger it.
	 */
	public static void setUserAgentOverride(WebDriver driver, String userAgent) {
		if (!(driver instanceof ChromiumDriver)) {
			throw new UnsupportedOperationException(
					"User-Agent override is only supported on Chromium-based drivers");
		}
		ChromiumDriver chromiumDriver = (ChromiumDriver) driver;

		Map<String, Object> params = new HashMap<>();
		params.put("userAgent", userAgent);
		chromiumDriver.executeCdpCommand("Network.setUserAgentOverride", params);
	}
}
