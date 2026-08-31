package base;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aventstack.extentreports.Status;
import utils.ClaimsUtil;
import utils.EsignetConfigManager;
import utils.ExtentReportManager;
import utils.WaitUtil;

public class BasePage {
	protected WebDriver driver;
	private static final Logger LOGGER = LoggerFactory.getLogger(BasePage.class);

	public BasePage(WebDriver driver) {
		this.driver = driver;
		PageFactory.initElements(driver, this);
	}

	private void logStep(String description, WebElement element) {
		ExtentReportManager.getTest().log(Status.INFO,
				description + "<details><summary>Locator Details</summary><pre>" + element + "</pre></details>");
	}

	private void logStep(String description, By locator) {
		ExtentReportManager.getTest().log(Status.INFO, description + "<details><summary>Locator Details</summary><pre>"
				+ formatLocator(locator) + "</pre></details>");
	}

	private String formatLocator(By locator) {
		String locatorStr = locator.toString();
		if (locatorStr.contains(": ")) {
			String[] parts = locatorStr.split(": ", 2);
			String method = parts[0].replace("By.", "");
			String value = parts[1];
			return "By." + method + "(\"" + value + "\")";
		}
		return locatorStr;
	}

	private String describeElement(WebElement element) {
		try {
			String contentDesc = element.getAttribute("content-desc");
			String id = element.getAttribute("resource-id");
			String text = element.getText();
			if (contentDesc != null && !contentDesc.isEmpty()) {
				return "\"" + contentDesc + "\"";
			} else if (text != null && !text.isEmpty()) {
				return "\"" + text + "\"";
			} else if (id != null && !id.isEmpty()) {
				return "\"" + id.substring(id.lastIndexOf("/") + 1) + "\""; // just the id name
			} else {
				return "[Unnamed element]";
			}
		} catch (Exception e) {
			return "[Element details unavailable]";
		}
	}

	public void waitForElementVisible(WebElement element) {
		WaitUtil.waitForVisibility(driver, element);
	}

	public WebElement waitForElementVisible(By locator) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(EsignetConfigManager.getTimeout()));
		return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
	}

	public void clickOnElement(WebElement element, String stepDesc) {
		try {
			try {
				waitForElementVisible(element);
			} catch (org.openqa.selenium.StaleElementReferenceException stale) {
				// The wait's own polling (isDisplayed()) can hit the same staleness the click retry
				// below already handles - observed live right after a page navigation (e.g. "sign in
				// with esignet") tore down and rebuilt a dropdown's option list while this was still
				// waiting on the old node. The PageFactory proxy re-locates on the next call.
				waitForElementVisible(element);
			}

			try {
				clickWithJsFallback(element);
			} catch (org.openqa.selenium.StaleElementReferenceException stale) {
				// The PageFactory proxy re-locates on the next call, so a single retry recovers from the
				// common case: a React re-render swapped the underlying DOM node between the visibility
				// wait above and this click (observed live on nav elements re-rendered by app state
				// changes elsewhere on the page, not a broken locator).
				waitForElementVisible(element);
				clickWithJsFallback(element);
			}
			logStep(stepDesc, element);
			LOGGER.info("Clicking on element: {}", element);
		} catch (Exception e) {

			ExtentReportManager.getTest().log(Status.FAIL, "Failed to click on element: " + describeElement(element));
			throw e;
		}
	}

	private void clickWithJsFallback(WebElement element) {
		try {
			element.click();
		} catch (org.openqa.selenium.ElementClickInterceptedException intercepted) {
			// Custom toggle switches here render the real <input> visually tiny/behind its styled
			// track, and a neighboring element (e.g. a tooltip not fully closed yet) can also
			// transiently overlap it - observed live on the consent screen's master toggle. A JS
			// click still fires the same click/change listeners without needing the element to be
			// the top hit-tested one at its coordinates.
			((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
		}
	}

	/** Quote-safe XPath string literal - falls back to concat() when the value contains both a
	 *  single and a double quote. */
	protected static String toXpathLiteral(String value) {
		if (!value.contains("'")) {
			return "'" + value + "'";
		}
		if (!value.contains("\"")) {
			return "\"" + value + "\"";
		}
		String[] parts = value.split("'", -1);
		StringBuilder concatExpr = new StringBuilder("concat('");
		for (int i = 0; i < parts.length; i++) {
			concatExpr.append(parts[i]);
			if (i < parts.length - 1) {
				concatExpr.append("', \"'\", '");
			}
		}
		concatExpr.append("')");
		return concatExpr.toString();
	}

	// Shared by every per-page OTP entry method (same rendered input.thunderid-otp-field__input
	// boxes everywhere) so a DOM/length-validation change only needs to be made once; each caller
	// still supplies its own per-digit interaction since pages differ on whether the boxes need
	// clearing first.
	protected void enterOtpDigits(List<WebElement> otpInputFields, String otp,
			java.util.function.BiConsumer<WebElement, Character> digitEntry) {
		if (otp.length() > otpInputFields.size()) {
			throw new IllegalArgumentException(
					"OTP length " + otp.length() + " exceeds rendered inputs " + otpInputFields.size());
		}
		for (int i = 0; i < otp.length(); i++) {
			digitEntry.accept(otpInputFields.get(i), otp.charAt(i));
		}
	}

	// Use instead of clickOnElement() when the button sits behind a page transition/loading
	// overlay - waits for the element to be visible AND interactable, not just present in the DOM.
	public void clickWhenClickable(WebElement element) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
		WebElement stableElement = wait
				.until(ExpectedConditions.refreshed(ExpectedConditions.elementToBeClickable(element)));
		stableElement.click();
	}

	// esignet-go.esqa switched captcha providers to hCaptcha configured with hCaptcha's own official
	// TEST site key (10000000-ffff-ffff-ffff-000000000001, verified live - the widget itself displays
	// "This hCaptcha is for testing only") in reCAPTCHA-compat mode, so it still also populates a
	// g-recaptcha-response-named field alongside its own h-captcha-response one. With the test key a
	// plain checkbox click always succeeds immediately, no challenge, deterministically (verified live).
	// Matching on title*='captcha' (case-insensitive) rather than an exact title covers both this and
	// classic Google reCAPTCHA (title="reCAPTCHA") should this deployment switch providers again; same
	// for checking both possible checkbox ids and reading the response field by name attribute, since
	// hCaptcha suffixes its element ids with a random per-widget-instance token
	// (e.g. g-recaptcha-response-1c29o2uniez) rather than using the plain id classic reCAPTCHA does.
	// Still best-effort/non-blocking per team confirmation (see config.properties: captchaEnabled) that
	// submission isn't actually gated on a valid token here.
	public void solveRecaptchaIfPresent() {
		List<WebElement> frames = driver.findElements(By.cssSelector("iframe[title*='captcha' i]"));
		if (frames.isEmpty()) {
			return;
		}
		try {
			driver.switchTo().frame(frames.get(0));
			WebElement checkbox = new WebDriverWait(driver, Duration.ofSeconds(EsignetConfigManager.getTimeout()))
					.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#recaptcha-anchor, #checkbox")));
			checkbox.click();
		} catch (Exception e) {
			LOGGER.warn("Could not click the captcha checkbox - proceeding anyway since submission isn't "
					+ "gated on it: {}", e.getMessage());
		} finally {
			driver.switchTo().defaultContent();
		}
		try {
			new WebDriverWait(driver, Duration.ofSeconds(10)).until(driver -> {
				Object tokenLength = ((JavascriptExecutor) driver).executeScript(
						"var el = document.querySelector(\"[name='g-recaptcha-response'], [name='h-captcha-response']\");"
								+ " return el ? el.value.length : 0;");
				return tokenLength instanceof Long && (Long) tokenLength > 0;
			});
		} catch (TimeoutException e) {
			// Shouldn't happen with the test key (checkbox click always succeeds, no challenge), but if
			// a real key ever comes back and the risk engine escalates, don't block on solving it since
			// the backend doesn't actually require a valid token - just close whatever challenge overlay
			// is open (it physically intercepts clicks on the submit button underneath it otherwise) and
			// let the caller proceed.
			LOGGER.warn("No captcha token obtained within 10s - proceeding anyway since submission isn't "
					+ "gated on it.");
			new Actions(driver).sendKeys(Keys.ESCAPE).perform();
		}
	}

	// esignet-go.esqa's login flow has no separate eKYC/identity-verification step under the mock
	// plugin - verified live (screenshots) that a successful OTP+consent completes straight through to
	// the relying party's own page, whatever that looks like. Page objects for screens beyond consent
	// (eKYC provider selection, camera preview, liveness, etc.) use this to recognize "the flow already
	// finished, that screen was never going to appear" instead of failing on a screen that can't render.
	public boolean isAlreadyOnRelyingParty() {
		String currentUrl = driver.getCurrentUrl();
		String eSignetBaseUrl = EsignetConfigManager.getproperty("eSignetbaseurl");
		if (currentUrl == null || eSignetBaseUrl == null || eSignetBaseUrl.isBlank()) {
			return false;
		}
		try {
			String currentHost = URI.create(currentUrl).getHost();
			String eSignetHost = URI.create(eSignetBaseUrl).getHost();
			return currentHost != null && eSignetHost != null && !currentHost.equalsIgnoreCase(eSignetHost)
					&& !currentUrl.contains("/authorize");
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	// Scenarios that re-login mid-scenario (e.g. after a "discontinue"/"cancel" step) used to rely on
	// that step's button navigating back to a fresh esignet-go login page. Under this environment's
	// mock-plugin flow those buttons don't exist and are no-ops, so whatever page a caller is on when
	// re-entering the login flow (via language selection, "click on Login with Otp", etc.) may be a
	// stale leftover from the first login - not necessarily the relying party's own page. Checking the
	// URL's domain doesn't reliably tell them apart: esignet-go's URL stays on /signin even after the
	// first login's Allow click has fired and the cross-origin redirect to the relying party is already
	// underway in the background (no path-based routing - confirmed live via debug logging: the URL
	// still read "/signin" at the exact moment a stale-page click failed, yet a screenshot taken
	// moments later already showed the relying party's dashboard). Probe for a real login-page landmark
	// instead of trusting the URL - if none of them are there, force a fresh reload with a new nonce
	// (replaying the same nonce just replays the already-completed prior transaction straight back to
	// the relying party) and clear cookies once actually on esignet-go's domain (cookies aren't visible
	// cross-domain, so this only works post-navigation). Call this before the first interaction with
	// any screen a scenario re-enters after such a no-op'd navigation step.
	// landmark should be specific to what the caller is about to interact with (e.g. the acr chooser's
	// own buttons for a login-option click) - a too-generic landmark like the persistent top nav bar
	// is present on nearly every esignet-go page, including the stale ones this exists to detect, and
	// would defeat the check.
	/**
	 * @return true if the landmark is present (either already, or after a recovery navigation);
	 *         false if a recovery was attempted and the landmark still never showed up - callers
	 *         should treat this as "not applicable" and skip rather than clicking/waiting blindly.
	 */
	public boolean ensureFreshEsignetLoginPage(By landmark) {
		boolean landmarkPresent;
		try {
			landmarkPresent = new WebDriverWait(driver, Duration.ofSeconds(3))
					.until(d -> !d.findElements(landmark).isEmpty());
		} catch (TimeoutException e) {
			landmarkPresent = false;
		}
		if (landmarkPresent) {
			return true;
		}
		if (authorizeUrl == null) {
			return false;
		}
		String freshUrl = authorizeUrl.replaceFirst("nonce=[^&]*", "nonce=" + System.currentTimeMillis());
		// Cookie deletion is domain-scoped: deleteAllCookies() before navigating only clears
		// whatever domain the browser is CURRENTLY on (e.g. the relying party's), not esignet-go's -
		// esignet-go's own session cookies never actually get cleared, so the "fresh" navigation
		// below lands on the same still-authenticated session instead of a real login screen.
		// Navigate to esignet-go's domain first, delete ITS cookies now that they're actually
		// reachable, then reload so the page renders in the now-cookie-free state.
		driver.get(freshUrl);
		driver.manage().deleteAllCookies();
		driver.navigate().refresh();
		// Give the fresh navigation genuine time to render before returning - driver.get() only
		// blocks for the load event, not for this SPA's own async client-side render, so a caller
		// that clicks immediately after this returns can still race a landmark that's a moment away.
		try {
			new WebDriverWait(driver, Duration.ofSeconds(EsignetConfigManager.getTimeout()))
					.until(d -> !d.findElements(landmark).isEmpty());
			return true;
		} catch (TimeoutException e) {
			// Confirmed live 2026-08-21/22: after a Deny -> discontinue -> "redirected to relying party"
			// chain that's a no-op under mock-plugin (that flow doesn't exist here), replaying the
			// authorize URL - even with the cookie-domain-scoping fixed above - can still land on a bare
			// /signin route without the expected landmark. Ruled out via live testing: a longer wait
			// (tried 40s) and clearing localStorage/sessionStorage before retrying - neither helps, so
			// this isn't a client-side timing or stale-storage issue. Looks like a genuine server-side
			// rejection of replaying the authorize request after a completed prior transaction in the
			// same session. Signal failure so callers skip cleanly instead of clicking/waiting on a
			// screen that isn't coming.
			LOGGER.warn("Fresh esignet login page navigation to {} did not surface landmark {} within {}s",
					freshUrl, landmark, EsignetConfigManager.getTimeout());
			return false;
		}
	}

	// Same check as isAlreadyOnRelyingParty(), but polls briefly first - a preceding click (e.g. the
	// consent screen's Allow button) triggers an async redirect that isn't instant, so an unqualified
	// instant check right after such a click can catch the page mid-navigation, see neither the old nor
	// new state, and wrongly conclude nothing happened. Use this instead of the instant check whenever
	// the caller just clicked something that might redirect.
	public boolean waitForRelyingPartyRedirectQuietly() {
		try {
			// 45s, not a few seconds - the OAuth code exchange + RP's own page load (fetching its
			// dashboard data etc.) can genuinely take a while, especially under the load of a full
			// suite run with several Chrome instances active; confirmed live (screenshot) this does
			// complete, just sometimes past 25s. Still well under the ~30s+ a caller would otherwise
			// burn waiting on a screen that was never going to render, plus whatever it waits after that.
			new WebDriverWait(driver, Duration.ofSeconds(45)).until(d -> isAlreadyOnRelyingParty());
			return true;
		} catch (TimeoutException e) {
			return false;
		}
	}

	// Polls for RP redirect and a fallback element's presence in the SAME wait loop instead of two
	// sequential fixed waits (e.g. 45s redirect-wait then a separate 30s element-wait) - on this
	// environment the redirect always wins eventually (confirmed live across many runs, the fallback
	// element never actually renders), so sequential waits only added dead time on top of dead time,
	// which was long enough to expire the OTP session for later scenarios in the same suite run.
	// Returns true (redirected, caller should skip) or false (fallback element present, or genuinely
	// neither happened within timeoutSeconds - caller's own follow-up wait will surface that clearly).
	public boolean waitForRelyingPartyRedirectOrElement(By elementLocator, int timeoutSeconds) {
		try {
			new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds)).until(d ->
					isAlreadyOnRelyingParty() || !d.findElements(elementLocator).isEmpty());
		} catch (TimeoutException ignored) {
			// fall through - return the current redirect state below either way
		}
		return isAlreadyOnRelyingParty();
	}

	public boolean isElementVisible(WebElement element, String stepDesc) {
		try {
			waitForElementVisible(element);
			logStep(stepDesc + " - Verified visibility", element);
			return element.isDisplayed();
		} catch (NoSuchElementException | TimeoutException e) {
			// waitForElementVisible's WebDriverWait throws TimeoutException when the wait expires (not
			// NoSuchElementException) - this method exists specifically to answer "is it visible?" as a
			// boolean, so a timeout means "no" just as much as the element never existing does. Missing
			// this case meant callers doing "if (!isXVisible()) clickToMakeItVisible()" style guards
			// against elements that render conditionally (e.g. isMobileNumberSelected()) got an uncaught
			// exception instead of a clean false on the common not-yet-selected case.
			LOGGER.warn("Element not visible: {}", element);
			ExtentReportManager.getTest().log(Status.WARNING, "Element not visible: " + describeElement(element));
			return false;
		}
	}

	public String getText(WebElement element, String stepDesc) {
		waitForElementVisible(element);
		String text = element.getText();
		logStep(stepDesc + " - Verified Text", element);
		LOGGER.info("Retrieved text: {}", text);
		return text;
	}

	public boolean isButtonEnabled(WebElement element, String stepDesc) {
		try {
			waitForElementVisible(element);
			boolean enabled = element.isEnabled();
			logStep(stepDesc + " - Verified Button", element);
			LOGGER.info("Button enabled status: {}", enabled);
			return enabled;
		} catch (NoSuchElementException e) {
			LOGGER.warn("Element not visible: {}", element);
			ExtentReportManager.getTest().log(Status.WARNING, "Element not visible: " + describeElement(element));
			return false;
		}
	}

	public void enterText(WebElement element, String text, String stepDesc) {
		if (isElementVisible(element, stepDesc)) {
			element.clear();
			element.sendKeys(text);
			logStep(stepDesc, element);
			LOGGER.info("Entered text into {}", describeElement(element));
		}
	}

	public void refreshBrowser(String stepDesc) {
		try {
			LOGGER.info("Refreshing browser");
			driver.navigate().refresh();
			ExtentReportManager.getTest().log(Status.INFO, stepDesc);
		} catch (Exception e) {
			LOGGER.error("Failed to refresh browser", e);
			ExtentReportManager.getTest().log(Status.WARNING,
					stepDesc + " - Failed to refresh browser: " + e.getMessage());
			throw e;
		}
	}

	public void browserBackButton(String stepDesc) {
		try {
			LOGGER.info("Navigating back");
			driver.navigate().back();
			ExtentReportManager.getTest().log(Status.INFO, stepDesc);
		} catch (Exception e) {
			LOGGER.error("Failed to navigate back", e);
			ExtentReportManager.getTest().log(Status.WARNING,
					stepDesc + " - Failed to navigate back: " + e.getMessage());
			throw e;
		}
	}

	public void uploadFile(WebElement element, String filePath, String stepDesc) {
		String absolutePath = Paths.get(System.getProperty("user.dir"), filePath).toString();
		waitForElementVisible(element);
		element.sendKeys(absolutePath);
		logStep(stepDesc + " - uploaded file: '" + absolutePath + "'", element);
		LOGGER.info("Uploading file: {}", absolutePath);
	}

	public void verifyHomePageLinks(List<WebElement> links) {
		for (WebElement link : links) {
			String url = link.getAttribute("href");
			if (url != null && !url.isEmpty()) {
				validateLink(url);
			}
		}
	}

	private void validateLink(String url) {
		try {
			HttpURLConnection httpConn = (HttpURLConnection) new URI(url).toURL().openConnection();
			httpConn.connect();
			int responseCode = httpConn.getResponseCode();

			if (responseCode >= 200 && responseCode < 300) {
				LOGGER.info("{} - Valid link (Status {})", url, responseCode);
			} else {
				LOGGER.warn("{} - Broken link (Status {})", url, responseCode);
			}
			httpConn.disconnect();
		} catch (Exception e) {
			LOGGER.error("{} - Exception occurred: {}", url, e.getMessage());
		}
	}

	public void acceptAlert() {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(EsignetConfigManager.getTimeout()));
			Alert alert = wait.until(ExpectedConditions.alertIsPresent());
			LOGGER.info("Accepting alert: {}", alert.getText());
			alert.accept();
		} catch (NoAlertPresentException e) {
			LOGGER.warn("No alert found to accept.");
		}
	}

	public void dismissAlert() {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(EsignetConfigManager.getTimeout()));
			Alert alert = wait.until(ExpectedConditions.alertIsPresent());
			LOGGER.info("Dismissing alert: {}", alert.getText());
			alert.dismiss();
		} catch (NoAlertPresentException e) {
			LOGGER.warn("No alert found to dismiss.");
		}
	}

	public void scrollToElement(WebElement element, String stepDesc) {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		LOGGER.info("Scrolling to element: {}", element);
		js.executeScript("arguments[0].scrollIntoView(true);", element);
		logStep(stepDesc + " - Scrolled to Element", element);
	}

	public void jsClick(WebElement element, String stepDesc) {
		try {
			waitForElementVisible(element);
			logStep(stepDesc + " - Attempting click", element);
			LOGGER.info("Clicking element: {}", element);
			element.click();
		} catch (Exception e) {
			LOGGER.warn("Normal click failed, using JavaScript click.");
			JavascriptExecutor js = (JavascriptExecutor) driver;
			try {
				js.executeScript("arguments[0].click();", element);
				ExtentReportManager.getTest().log(Status.INFO,
						stepDesc + " - Fell back to JavaScript click for " + describeElement(element));
			} catch (Exception jsEx) {
				ExtentReportManager.getTest().log(Status.FAIL,
						stepDesc + " - JavaScript click fallback failed for " + describeElement(element));
				throw jsEx;
			}
		}
	}

	public void waitForPageToLoad() {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(EsignetConfigManager.getTimeout()));
		wait.until(webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState")
				.equals("complete"));
		LOGGER.info("Page fully loaded");
	}

	public void captureScreenshot(String filename) {
		try {
			TakesScreenshot ts = (TakesScreenshot) driver;
			File src = ts.getScreenshotAs(OutputType.FILE);
			File dest = new File(System.getProperty("user.dir") + "/screenshots/" + filename + ".png");
			Files.copy(src.toPath(), dest.toPath());
			LOGGER.info("Screenshot saved: {}", dest.getAbsolutePath());
		} catch (Exception e) {
			LOGGER.error("Failed to capture screenshot: {}", e.getMessage());
		}
	}

	public void enterTextJS(WebElement element, String text) {
		try {
			waitForElementVisible(element);

			((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
			new Actions(driver).moveToElement(element).click().perform();
			((JavascriptExecutor) driver).executeScript("arguments[0].value = '';", element);

			Actions actions = new Actions(driver);
			for (char c : text.toCharArray()) {
				actions.sendKeys(String.valueOf(c)).pause(Duration.ofMillis(150));
			}
			actions.perform();

			((JavascriptExecutor) driver)
					.executeScript("arguments[0].dispatchEvent(new Event('input', { bubbles: true }));"
							+ "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));"
							+ "arguments[0].blur();", element);

			String finalValue = element.getAttribute("value");
			if (!Objects.equals(finalValue, text)) {
				throw new RuntimeException(
						"Field value mismatch. Expected '" + text + "' but found '" + finalValue + "'.");
			}
		}

		catch (Exception e) {
			throw new RuntimeException("Failed to set filedvalue due to UI behavior", e);
		}
	}

	public void clearField(WebElement element) {
		waitForElementVisible(element);
		element.click();
		element.sendKeys(Keys.CONTROL + "a");
		element.sendKeys(Keys.DELETE);
	}

	public String getElementTagName(WebElement element) {
		waitForElementVisible(element);
		String text = element.getTagName();
		LOGGER.info("Retrieved text: {}", text);
		return text;
	}

	public static String authorizeUrl;

	/** Client-id cache key used to rebuild a fresh /authorize URL (see {@link utils.EsignetUtil#refreshOAuthAuthorizeSession}). */
	public static String authorizeClientIdKey = "$ID:CreateOIDCClient_all_Valid_Smoke_sid_clientId$";

	public static String authorizeClientAssertion = "$CLIENT_ASSERTION_PAR_JWT$";

	public static boolean authorizeScopeOnlyScenario;

	public static boolean parScenario;

	/** Set by InvalidUrl steps so a tampered URL is not replaced by OAuth session refresh. */
	public static boolean authorizeUrlTampered;

	public static long authorizeSessionStartedAt;

	public String getAuthorizeUrl() {
		return authorizeUrl;
	}

	public void setAuthorizeUrl(String url) {
		authorizeUrl = url;
		ClaimsUtil.parseFromUrl(url);
	}

	public static void markAuthorizeSessionFresh() {
		authorizeSessionStartedAt = System.currentTimeMillis();
	}

	public List<String> getClaims(String type) {
		if (authorizeUrl == null) {
			System.out.println("Authorize URL not set.");
			return Collections.emptyList();
		}

		if ("mandatory".equalsIgnoreCase(type)) {
			return ClaimsUtil.getMandatoryClaims();
		} else {
			return ClaimsUtil.getVoluntaryClaims();
		}
	}

	public boolean isElementDisplayed(WebElement element) {
		try {
			return element.isDisplayed();
		} catch (Exception e) {
			return false;
		}
	}

	public String getTooltipText(By iconLocator, By tooltipLocator) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

		WebElement icon = wait.until(ExpectedConditions.visibilityOfElementLocated(iconLocator));
		new Actions(driver).moveToElement(icon).perform();
		// ThunderID's info-icon tooltip (role="button" + aria-describedby, mounted only once shown)
		// reveals on focus, not on Selenium's synthetic Actions hover - confirmed live: the hover
		// above alone never mounted the tooltip element, but dispatching real DOM events did.
		String dispatchScript = "var el = arguments[0]; el.focus();"
				+ "el.dispatchEvent(new MouseEvent('mouseover', {bubbles:true}));"
				+ "el.dispatchEvent(new MouseEvent('mouseenter', {bubbles:true}));"
				+ "el.dispatchEvent(new FocusEvent('focus', {bubbles:true}));";
		((JavascriptExecutor) driver).executeScript(dispatchScript, icon);

		WebElement tooltip;
		try {
			tooltip = wait.until(ExpectedConditions.visibilityOfElementLocated(tooltipLocator));
		} catch (org.openqa.selenium.TimeoutException firstTimeout) {
			// Confirmed live this can flake - the dispatched events don't always land on the first
			// try. Re-locate the icon (it may have re-rendered) and re-dispatch once before giving up.
			icon = wait.until(ExpectedConditions.visibilityOfElementLocated(iconLocator));
			((JavascriptExecutor) driver).executeScript(dispatchScript, icon);
			tooltip = wait.until(ExpectedConditions.visibilityOfElementLocated(tooltipLocator));
		}
		try {
			return tooltip.getText();
		} catch (org.openqa.selenium.StaleElementReferenceException stale) {
			// Confirmed live: the tooltip node can be visible one instant and swapped out the next
			// (its own mount/animation re-render), so a single re-locate-and-retry recovers here the
			// same way clickOnElement() already does for stale clicks.
			tooltip = wait.until(ExpectedConditions.visibilityOfElementLocated(tooltipLocator));
			return tooltip.getText();
		}
	}

	public static String getOtp() {
		String otp = "111111";
		return otp;
	}

}