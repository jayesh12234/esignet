package pages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.WebDriverWait;
import base.BasePage;
import utils.BaseTestUtil;
import utils.LanguageUtil;

public class LivenessCheckPage extends BasePage {

	private static final Logger LOGGER = Logger.getLogger(LivenessCheckPage.class.getName());
	private static final String SLOT_ENDPOINT = "/identity-verification/slot";
	private static final long EXPECTED_POLL_INTERVAL_SECONDS = 6;
	private static final long POLL_INTERVAL_TOLERANCE_SECONDS = 1;
	private static final int MAX_POLL_ATTEMPTS = 10;

	private static final String VERIFICATION_INCOMPLETE_ERROR_CODE = "verification_incomplete";
	// Curly right-single-quote (’), matching EkycPage.CONSENT_REJECTED_MESSAGE and
	// the literal character oidc-ui's locale file uses - a straight-apostrophe
	// match would otherwise silently fail against the real rendered copy.
	private static final String CONSENT_NOT_SHARED_MESSAGE = "We’re sorry! Your login was unsuccessful as consent was not shared.";
	private static final Duration SESSION_TIMEOUT_WAIT = Duration.ofMinutes(3);
	private static final Duration RELYING_PARTY_REDIRECT_WAIT = Duration.ofSeconds(30);

	// Countdown digits (e.g. "05") tick down in real time, so the exact number is
	// not asserted - only the surrounding message template.
	private static final Pattern WELCOME_COUNTDOWN_MESSAGE_PATTERN = Pattern
			.compile("Welcome!\\s*Initiating Identity verification process in \\d+ seconds");

	/**
	 * Mirrors common mobile viewport breakpoints (small phone / mid-size phone /
	 * large phone) already used for responsiveness checks elsewhere in the
	 * suite, so the video eKYC screen is exercised at the same set of sizes.
	 */
	private static final int[][] MOBILE_VIEWPORT_SIZES = { { 360, 640 }, { 390, 844 }, { 412, 915 } };

	private List<Long> slotRequestTimestamps;

	public LivenessCheckPage(WebDriver driver) {
		super(driver);
	}

	@FindBy(xpath = "//div[contains(@class,'video-message')]")
	WebElement livenessCheckHeader;

	@FindBy(tagName = "video")
	WebElement livenessVideo;

	@FindBy(xpath = "//*[@data-testid='ekyc-fail-icon']")
	WebElement ekycFailIcon;

	// Placeholder locator - not yet confirmed against the real DOM, verify during debug
	@FindBy(xpath = "//div[@data-testid='ekyc-status']//h1")
	WebElement ekycErrorHeader;

	@FindBy(xpath = "//div[@data-testid='ekyc-status']//p[contains(@class,'text-gray-500')]")
	WebElement ekycErrorMessage;

	@FindBy(id = "okay-button")
	WebElement ekycErrorOkayButton;

	@FindBy(xpath = "//p[contains(@class,\"text-[#D52929]\")]")
	WebElement networkDroppedMessage;

	@FindBy(id = "language_selection")
	WebElement languageDropdown;

	@FindBy(xpath = "//img[@class='brand-logo']")
	WebElement logo;

	@FindBy(xpath = "//*[contains(text(),'Powered by') and contains(.,'eSignet')]")
	WebElement poweredByFooter;

	@FindBy(xpath = "//div[@role='menuitem']")
	List<WebElement> languageDropdownItems;

	private String contentSnapshotBeforeLanguageSwitch;

	public boolean isLivenessCheckHeaderDisplayed() {
		return isElementVisible(livenessCheckHeader, "Verified liveness check header is visible");
	}

	/**
	 * Verifies the onscreen instruction shown above the video frame matches the
	 * "Welcome! Initiating Identity verification process in NN seconds" template.
	 */
	public boolean isWelcomeCountdownMessageDisplayed() {
		waitForElementVisible(livenessCheckHeader);
		String actualText = livenessCheckHeader.getText().trim();
		boolean matches = WELCOME_COUNTDOWN_MESSAGE_PATTERN.matcher(actualText).find();
		if (!matches) {
			LOGGER.warning(
					"Expected welcome/countdown message not found on video eKYC screen. Actual text: '" + actualText + "'");
		}
		return matches;
	}

	/**
	 * A visible <video> tag alone doesn't prove the feed is rendering - checks
	 * that the element actually has decoded frame data bound to it.
	 */
	public boolean isUserVisibleInVideoFeed() {
		waitForElementVisible(livenessVideo);
		JavascriptExecutor js = (JavascriptExecutor) driver;
		Boolean isStreaming = (Boolean) js.executeScript(
				"var v = arguments[0]; return v.readyState >= 2 && v.videoWidth > 0 && v.videoHeight > 0;",
				livenessVideo);
		return Boolean.TRUE.equals(isStreaming);
	}

	public boolean isLogoDisplayed() {
		return isElementVisible(logo, "Verified logo is displayed on video eKYC screen");
	}

	public boolean isLanguageDropdownDisplayed() {
		return isElementVisible(languageDropdown, "Verified language dropdown is displayed on video eKYC screen");
	}

	/**
	 * Confirms clicking the language dropdown actually expands it: at least one
	 * language option (role='menuitem') becomes visible.
	 */
	public boolean isLanguageDropdownExpanded() {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
		try {
			wait.until(d -> languageDropdownItems.stream().anyMatch(WebElement::isDisplayed));
			return true;
		} catch (TimeoutException e) {
			LOGGER.warning("Language dropdown did not expand (no visible menu items) on video eKYC screen");
			return false;
		}
	}

	/**
	 * Confirms the language dropdown collapses back down: no language option
	 * remains visible.
	 */
	public boolean isLanguageDropdownCollapsed() {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
		try {
			wait.until(d -> languageDropdownItems.stream().noneMatch(WebElement::isDisplayed));
			return true;
		} catch (TimeoutException e) {
			LOGGER.warning("Language dropdown did not collapse (menu items still visible) on video eKYC screen");
			return false;
		}
	}

	public boolean isPoweredByFooterDisplayed() {
		return isElementVisible(poweredByFooter, "Verified 'Powered by eSignet' footer is displayed on video eKYC screen");
	}

	/**
	 * Expected result calls for instructions positioned above the video frame
	 * specifically, not just non-overlapping (contrast with
	 * areInstructionsAndVideoFrameAligned, which only checks the two don't
	 * overlap in either order).
	 */
	public boolean isInstructionsDisplayedAboveVideoFrame() {
		waitForElementVisible(livenessCheckHeader);
		waitForElementVisible(livenessVideo);
		if (!livenessCheckHeader.isDisplayed() || !livenessVideo.isDisplayed()) {
			LOGGER.warning("Instructions or video frame is not visible on the video eKYC screen");
			return false;
		}

		Rectangle headerRect = livenessCheckHeader.getRect();
		Rectangle videoRect = livenessVideo.getRect();
		boolean isAbove = headerRect.getY() + headerRect.getHeight() <= videoRect.getY();
		if (!isAbove) {
			LOGGER.warning("Onscreen instructions are not displayed above the video frame on the video eKYC screen");
		}
		return isAbove;
	}

	public void disconnectNetwork() {
		BaseTestUtil.setNetworkOffline(driver, true);
	}

	public void reconnectNetwork() {
		BaseTestUtil.setNetworkOffline(driver, false);
	}

	public boolean isEkycErrorDisplayed() {
		return isElementVisible(ekycFailIcon, "Verified eKYC failure icon is visible")
				&& isElementVisible(ekycErrorMessage, "Verified eKYC error message is visible");
	}

	/**
	 * Verifies the exact copy on the eKYC failure popup: header, message and
	 * the Okay button label.
	 */
	public boolean isEkycFailurePopupContentCorrect(String expectedHeader, String expectedMessage,
			String expectedButtonLabel) {
		waitForElementVisible(ekycFailIcon);

		String actualHeader = ekycErrorHeader.getText();
		if (!expectedHeader.equals(actualHeader)) {
			LOGGER.warning("Expected eKYC failure popup header '" + expectedHeader + "' but found '" + actualHeader
					+ "'");
			return false;
		}

		String actualMessage = ekycErrorMessage.getText();
		if (!expectedMessage.equals(actualMessage)) {
			LOGGER.warning("Expected eKYC failure popup message '" + expectedMessage + "' but found '" + actualMessage
					+ "'");
			return false;
		}

		String actualButtonLabel = ekycErrorOkayButton.getText();
		if (!expectedButtonLabel.equalsIgnoreCase(actualButtonLabel)) {
			LOGGER.warning("Expected eKYC failure popup button label '" + expectedButtonLabel + "' but found '"
					+ actualButtonLabel + "'");
			return false;
		}
		return true;
	}

	public void clickOkayOnEkycError() {
		clickOnElement(ekycErrorOkayButton, "Clicked Okay button on eKYC error screen");
	}

	public boolean isNetworkDroppedMessageDisplayed() {
		return isElementVisible(networkDroppedMessage, "Verified network dropped message is visible");
	}

	/**
	 * Must be called before the action that triggers slot polling (e.g. before
	 * clicking Proceed on the camera preview page).
	 */
	public void startCapturingSlotRequests() {
		slotRequestTimestamps = BaseTestUtil.captureRequestTimestamps(driver, SLOT_ENDPOINT);
	}

	/**
	 * Validates the captured /slot request timestamps against the documented
	 * contract: checked every 6s, for a max of 10 attempts. Note: on a happy
	 * path where a slot is allocated on the first attempt, only one request
	 * will have been captured - this trivially satisfies the contract since
	 * there's no interval to violate. To actually exercise the retry cadence,
	 * the scenario needs a precondition that forces slot unavailability.
	 */
	public boolean isSlotPollingContractValid() {
		if (slotRequestTimestamps == null || slotRequestTimestamps.isEmpty()) {
			LOGGER.warning("No slot availability requests were captured");
			return false;
		}
		List<Long> timestamps = new ArrayList<>(slotRequestTimestamps);

		if (timestamps.size() > MAX_POLL_ATTEMPTS) {
			LOGGER.warning("Slot availability was checked " + timestamps.size() + " times, exceeding the max of "
					+ MAX_POLL_ATTEMPTS);
			return false;
		}

		for (int i = 1; i < timestamps.size(); i++) {
			long gapSeconds = (timestamps.get(i) - timestamps.get(i - 1)) / 1000;
			if (Math.abs(gapSeconds - EXPECTED_POLL_INTERVAL_SECONDS) > POLL_INTERVAL_TOLERANCE_SECONDS) {
				LOGGER.warning("Gap between slot check attempt " + i + " and " + (i + 1) + " was " + gapSeconds
						+ "s, expected ~" + EXPECTED_POLL_INTERVAL_SECONDS + "s");
				return false;
			}
		}
		return true;
	}

	/**
	 * Resizes the (already mobile-emulated) viewport across representative phone
	 * breakpoints and checks that the header and the self-view video stay
	 * visible and within the viewport bounds at each size.
	 */
	public boolean isMobileResponsive() {
		for (int[] size : MOBILE_VIEWPORT_SIZES) {
			driver.manage().window().setSize(new Dimension(size[0], size[1]));

			if (!isElementWithinViewport(livenessCheckHeader, size[0], "Liveness check header")) {
				return false;
			}
			if (!isElementWithinViewport(livenessVideo, size[0], "Self-view video")) {
				return false;
			}
		}
		return true;
	}

	private boolean isElementWithinViewport(WebElement element, int viewportWidth, String elementName) {
		waitForElementVisible(element);
		if (!element.isDisplayed()) {
			LOGGER.warning(elementName + " is not visible at viewport width " + viewportWidth);
			return false;
		}

		Rectangle rect = element.getRect();
		if (rect.getWidth() <= 0) {
			LOGGER.warning(elementName + " has collapsed width at viewport width " + viewportWidth);
			return false;
		}
		if (rect.getX() < 0 || rect.getX() + rect.getWidth() > viewportWidth) {
			LOGGER.warning(elementName + " overflows the viewport (x=" + rect.getX() + ", width=" + rect.getWidth()
					+ ") at viewport width " + viewportWidth);
			return false;
		}
		return true;
	}

	/**
	 * Snapshots the currently displayed instruction text so a later language
	 * switch can be confirmed to have actually re-rendered the content, not
	 * just flipped the locale flag.
	 */
	public void captureContentSnapshot() {
		waitForElementVisible(livenessCheckHeader);
		contentSnapshotBeforeLanguageSwitch = livenessCheckHeader.getText();
	}

	public void clickOnLanguageDropdown() {
		clickOnElement(languageDropdown, "Clicked on language dropdown in video eKYC screen");
	}

	public void selectLanguage(String langCode) {
		String displayName = LanguageUtil.getDisplayName(langCode);
		WebElement languageOption = driver
				.findElement(By.xpath("//div[@role='menuitem' and normalize-space()='" + displayName + "']"));
		clickOnElement(languageOption, "Selected " + displayName + " language on video eKYC screen");
	}

	/**
	 * Confirms both halves of a real language switch: the persisted i18next
	 * locale actually changed, and the on-screen instructions re-rendered as a
	 * result (a locale flag flipping with stale content left on screen would
	 * otherwise pass a check that only looked at localStorage).
	 */
	public boolean isContentDisplayedInLanguage(String langCode) {
		String expectedIsoCode = LanguageUtil.getIsoLanguageCode(langCode);
		String actualIsoCode = getLanguageFromLocalStorage();
		if (!Objects.equals(expectedIsoCode, actualIsoCode)) {
			LOGGER.warning(
					"Expected language code '" + expectedIsoCode + "' but localStorage had '" + actualIsoCode + "'");
			return false;
		}

		waitForElementVisible(livenessCheckHeader);
		String currentContent = livenessCheckHeader.getText();
		boolean contentChanged = contentSnapshotBeforeLanguageSwitch == null
				|| !contentSnapshotBeforeLanguageSwitch.equals(currentContent);
		if (!contentChanged) {
			LOGGER.warning("Video eKYC screen content did not change after switching to language '" + langCode + "'");
		}
		return !currentContent.isBlank() && contentChanged;
	}

	private String getLanguageFromLocalStorage() {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		return (String) js.executeScript("return window.localStorage.getItem('i18nextLng');");
	}

	public void disableCameraAccessAtRuntime() {
		BaseTestUtil.setCameraPermissionAtRuntime(driver, "denied");
	}

	public void enableCameraAccessAtRuntime() {
		BaseTestUtil.setCameraPermissionAtRuntime(driver, "granted");
	}

	/**
	 * The exact session-timeout duration isn't documented, so this polls with a
	 * generous upper bound instead of a fixed sleep for the failure status the
	 * app is expected to surface once the video feed drops out after camera
	 * access is revoked mid-session.
	 */
	public boolean waitForSessionTimeoutMessage() {
		WebDriverWait wait = new WebDriverWait(driver, SESSION_TIMEOUT_WAIT);
		try {
			wait.until(d -> !d.findElements(By.xpath("//*[@data-testid='ekyc-fail-icon']")).isEmpty());
			return true;
		} catch (TimeoutException e) {
			LOGGER.warning("Session timeout message was not observed within " + SESSION_TIMEOUT_WAIT.getSeconds()
					+ "s of disabling camera access");
			return false;
		}
	}

	/**
	 * Confirms the documented outcome of the camera-disabled session timeout:
	 * redirect back to the relying party carrying the verification_incomplete
	 * error code, with the relying party's consent-not-shared message on screen.
	 */
	public boolean waitForRedirectWithVerificationIncompleteError() {
		WebDriverWait wait = new WebDriverWait(driver, RELYING_PARTY_REDIRECT_WAIT);
		try {
			wait.until(d -> d.getCurrentUrl().contains(VERIFICATION_INCOMPLETE_ERROR_CODE));
		} catch (TimeoutException e) {
			LOGGER.warning("Redirect containing '" + VERIFICATION_INCOMPLETE_ERROR_CODE + "' was not observed within "
					+ RELYING_PARTY_REDIRECT_WAIT.getSeconds() + "s. Current URL: " + driver.getCurrentUrl());
			return false;
		}

		String pageText = driver.findElement(By.tagName("body")).getText();
		if (!pageText.contains(CONSENT_NOT_SHARED_MESSAGE)) {
			LOGGER.warning("Expected consent-not-shared message not found on relying party page. Page text: "
					+ pageText);
			return false;
		}
		return true;
	}

	/**
	 * Placeholder heuristic - not yet confirmed against the real DOM (same
	 * caveat as ekycErrorHeader above: this is an external identity-verification
	 * micro-frontend not vendored in this repo). Rather than guess a specific
	 * class/testid for the color-flash overlay - which could silently match
	 * nothing, or the wrong element, and still report a false pass - this looks
	 * for the largest near-full-viewport element with a solid (non-transparent)
	 * background color and samples it over a short window to confirm the color
	 * actually cycles, which is the observable behavior color-based frame
	 * verification depends on. Verify/tighten the selector during debug once
	 * the real DOM is available.
	 */
	public boolean isSolidColorFrameCyclingAcrossFullScreen() {
		String sampleScript = "function isSolid(c) { return c && c !== 'rgba(0, 0, 0, 0)' && c !== 'transparent'; }"
				+ "var vw = window.innerWidth, vh = window.innerHeight;" + "var best = null, bestArea = 0;"
				+ "document.querySelectorAll('body *').forEach(function(el) {"
				+ "  var r = el.getBoundingClientRect();" + "  var area = r.width * r.height;"
				+ "  if (area < vw * vh * 0.8) return;"
				+ "  var bg = window.getComputedStyle(el).backgroundColor;" + "  if (!isSolid(bg)) return;"
				+ "  if (area > bestArea) { bestArea = area; best = el; }" + "});" + "if (!best) return null;"
				+ "return window.getComputedStyle(best).backgroundColor;";

		JavascriptExecutor js = (JavascriptExecutor) driver;
		List<String> observedColors = new ArrayList<>();
		long deadline = System.currentTimeMillis() + 10000;
		while (System.currentTimeMillis() < deadline && observedColors.size() < 10) {
			Object color = js.executeScript(sampleScript);
			if (color != null) {
				observedColors.add(color.toString());
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}

		if (observedColors.isEmpty()) {
			LOGGER.warning("No near-full-viewport solid-color element was found on the video eKYC screen");
			return false;
		}

		long distinctColors = observedColors.stream().distinct().count();
		if (distinctColors < 2) {
			LOGGER.warning(
					"Full-screen color did not change across samples; observed only: " + observedColors.get(0));
			return false;
		}
		return true;
	}

	/**
	 * Proxy for "instructions and video frame are aligned properly": confirms
	 * both are visible and don't visually overlap (one fully above/below the
	 * other), rather than being stacked on top of each other.
	 */
	public boolean areInstructionsAndVideoFrameAligned() {
		waitForElementVisible(livenessCheckHeader);
		waitForElementVisible(livenessVideo);
		if (!livenessCheckHeader.isDisplayed() || !livenessVideo.isDisplayed()) {
			LOGGER.warning("Instructions or video frame is not visible on the video eKYC screen");
			return false;
		}

		Rectangle headerRect = livenessCheckHeader.getRect();
		Rectangle videoRect = livenessVideo.getRect();
		boolean noOverlap = headerRect.getY() + headerRect.getHeight() <= videoRect.getY()
				|| videoRect.getY() + videoRect.getHeight() <= headerRect.getY();
		if (!noOverlap) {
			LOGGER.warning("Instructions and video frame overlap on the video eKYC screen");
		}
		return noOverlap;
	}
}
