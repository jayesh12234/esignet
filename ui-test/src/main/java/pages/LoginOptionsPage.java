package pages;

import base.BasePage;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.EsignetConfigManager;
import utils.LinkAuthUtil;
import utils.MockMdsManager;
import utils.ResourceBundleLoader;

public class LoginOptionsPage extends BasePage {

	private static final Set<String> NON_WALLET_LOGIN_IDS = Set.of(
			"login_with_otp", "login_with_bio", "login_with_pwd", "login_with_pin", "login_with_kbi");

	private static final Logger LOGGER = LoggerFactory.getLogger(LoginOptionsPage.class);

	public LoginOptionsPage(WebDriver driver) {
		super(driver);
	}

	// Verified live (matches ConsentPage.loginTitle): the login-screen title renders as h3#text_heading.
	// "signup-url-button" doesn't exist on this deployment - there's no signup service, and this was
	// never actually a button, just a dead id historically used to read the page's title text.
	@FindBy(id = "text_heading")
	WebElement loginButton;

	@FindBy(xpath = "//img[@class='brand-logo']")
	WebElement brandLogo;

	@FindBy(id = "login_with_walletname")
	WebElement loginWithInji;

	// The language switcher has no id at all - only aria-haspopup="listbox" inside the nav bar.
	// Verified by opening the live dropdown; it defaults to Arabic when the authorize URL carries no
	// ui_locales, and its options render as role="option" buttons (not role="menuitem" divs).
	@FindBy(css = "nav button[aria-haspopup='listbox']")
	WebElement languageDropdown;

	@FindBy(xpath = "//button[@role='option' and normalize-space()='हिन्दी']")
	WebElement hindiLanguage;

	// Rewritten against the current "ThunderID" component library used by esignet-go (esqa) - the
	// classic eSignet UI's login_with_* ids no longer exist. Verified by rendering the live login
	// page in a real browser (2026-08-19): the auth-method-selection screen renders acr_otp/
	// acr_password/acr_bio buttons; login_with_pin/login_with_kbi/login_with_walletname weren't
	// observed on that render (client had no PIN/KBI/wallet auth factors registered) and are left
	// as-is pending verification against a client that does.
	@FindBy(id = "acr_otp")
	WebElement loginWithOtpBtn;

	@FindBy(id = "acr_bio")
	WebElement loginWithBiometricBtn;

	@FindBy(id = "login_with_walletname")
	WebElement loginWithInjiBtn;

	@FindBy(id = "acr_password")
	WebElement loginWithPasswordBtn;

	@FindBy(id = "login_with_pin")
	WebElement loginWithPinBtn;

	@FindBy(id = "login_with_kbi")
	WebElement loginWithKbiBtn;

	@FindBy(id = "show-more-options")
	List<WebElement> moreWaysToSignIn;

	// Same ThunderID rewrite as above, verified by rendering the live ID-type/OTP-request screen.
	@FindBy(id = "login_id_mobile")
	WebElement mobileNumberOption;

	@FindBy(id = "login_id_nrc")
	WebElement nrcIdOption;

	// "vid" is now a combined UIN/VID button/field - login_id_uin.
	@FindBy(id = "login_id_uin")
	WebElement vidOption;

	@FindBy(id = "login_id_email")
	WebElement emailOption;

	@FindBy(id = "back-button")
	WebElement backButton;

	@FindBy(id = "login-header")
	WebElement loginHeader;

	@FindBy(id = "login-subheader")
	WebElement loginSubHeader;

	@FindBy(xpath = "//div[contains(@class,'font-semibold') and contains(@class,'mx-2')]")
	WebElement selectPreferredIdHeader;

	@FindBy(id = "submit_uin")
	WebElement getOtpButton;

	@FindBy(xpath = "//button[@id='login_id_mobile' and contains(@class,'login-id-button--active')]")
	WebElement mobileSelected;

	// Verified live: this is a plain native HTML <select> (no id), not a custom JS dropdown with
	// separately clickable/id'd options - "Otp_login_dropdown_button"/"KHM"/"IND" never existed on
	// this deployment. Its two <option>s carry the country calling codes as their value attribute:
	// value="+91" (India) and value="+855" (Cambodia/KHM) - confirmed via live DOM capture of the
	// mobile-number entry screen. Interact with it via Selenium's Select wrapper, not clickOnElement.
	@FindBy(css = "select.thunderid-affixed-field__prefix-select")
	WebElement prefixNumberField;

	// OTP entry is now 6 separate single-digit boxes (no shared id), not one field - verified by
	// rendering the live OTP screen. Each is aria-labelled "... digit N"; the container carries this
	// class regardless of language.
	@FindBy(css = "input.thunderid-otp-field__input")
	List<WebElement> otpInputFields;

	@FindBy(id = "action_submit_otp")
	WebElement submitOtpButton;

	// Verified live (matches ConsentPage's own attention/consent screen check): the single merged
	// attention/consent screen's real, only interactive element is id="action_allow" - not
	// div.header.my-2, which doesn't exist here.
	@FindBy(id = "action_allow")
	WebElement attentionScreen;

	@FindBy(id = "cancel-button")
	WebElement attentionCancelButton;

	@FindBy(id = "discontinue-button")
	WebElement attentionDiscontinueButton;

	// The ID-type buttons (login_id_uin/login_id_mobile/login_id_email/login_id_nrc) now share a
	// single input field regardless of which type is selected, instead of one field per type.
	@FindBy(id = "username_input")
	WebElement idInputField;

	@FindBy(id = "error-banner-message")
	WebElement invalidIndividualIdErrorMessage;

	@FindBy(id = "sbi_vid")
	WebElement biometricVidField;

	@FindBy(id = "secure-biometric-interface-integration")
	WebElement biometricIntegrationContainer;

	public boolean isLogoDisplayed() {
		return isElementVisible(brandLogo, "Verified is logo displayed");
	}

	public void waitForSignInPageReady() {
		waitForAuthorizeFlowReady();
	}

	public void waitForAuthorizeFlowReady() {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
		wait.until(webDriver -> {
			String url = webDriver.getCurrentUrl();
			int hashIndex = url.indexOf('#');
			if (hashIndex >= 0 && hashIndex < url.length() - 10) {
				return true;
			}
			if (findVisibleWalletLoginButton() != null) {
				return true;
			}
			if (!webDriver.findElements(By.id("login_with_otp")).isEmpty()) {
				return true;
			}
			return !webDriver.findElements(By.id("show-more-options")).isEmpty();
		});
	}

	public void clickOnLoginWithInji() {
		WebElement walletLoginButton = waitForWalletLoginButton();
		clickOnElement(walletLoginButton, "Clicked on login with wallet");
	}

	public boolean isLoginWithInjiOptionVisible() {
		return waitForWalletLoginButton(Duration.ofSeconds(30)) != null;
	}

	public void openLoginWithInjiViaMoreWaysToSignInIfNeeded() {
		waitForAuthorizeFlowReady();
		if (waitForWalletLoginButton(Duration.ofSeconds(15)) == null) {
			clickMoreWaysToSignInIfVisible();
		}
		if (waitForWalletLoginButton(Duration.ofSeconds(15)) == null) {
			clickMoreWaysToSignInIfVisible();
		}
	}

	private WebElement waitForWalletLoginButton(Duration timeout) {
		WebDriverWait wait = new WebDriverWait(driver, timeout);
		try {
			return wait.until(webDriver -> findVisibleWalletLoginButton());
		} catch (TimeoutException e) {
			return null;
		}
	}

	private WebElement waitForWalletLoginButton() {
		WebElement walletLoginButton = waitForWalletLoginButton(Duration.ofSeconds(30));
		if (walletLoginButton == null) {
			throw new TimeoutException("Wallet login option was not displayed");
		}
		return walletLoginButton;
	}

	private WebElement findVisibleWalletLoginButton() {
		for (WebElement button : driver.findElements(By.cssSelector("[id^='login_with_']"))) {
			String id = button.getAttribute("id");
			if (id != null && !NON_WALLET_LOGIN_IDS.contains(id) && button.isDisplayed()) {
				return button;
			}
		}
		if (loginWithInjiBtn != null) {
			try {
				if (loginWithInjiBtn.isDisplayed()) {
					return loginWithInjiBtn;
				}
			} catch (StaleElementReferenceException ignored) {
				// fall through to null
			}
		}
		return null;
	}

	public boolean waitForWalletQrCodeDisplayed() {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
		try {
			wait.until(driver -> isWalletQrCodeDisplayed());
			return true;
		} catch (TimeoutException e) {
			return false;
		}
	}

	public boolean isWalletQrCodeDisplayed() {
		try {
			WebElement qrCode = waitForElementVisible(By.id("wallet-qr-code"));
			return qrCode.isDisplayed();
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isWalletQrHeaderDisplayed() {
		List<WebElement> headers = driver.findElements(By.cssSelector(".qr-title"));
		for (WebElement header : headers) {
			if (!header.isDisplayed()) {
				continue;
			}
			String text = normalizeMessage(safeGetText(header));
			if (text.contains("scan") && text.contains("qr")) {
				return true;
			}
		}
		String bundleHeader = normalizeMessage(ResourceBundleLoader.get("LoginQRCode.wallet_header"));
		if (!bundleHeader.startsWith("!!missing_key:")) {
			String prefix = normalizeMessage(bundleHeader.split("\\{\\{walletname\\}\\}")[0]);
			if (!prefix.isBlank()) {
				for (WebElement header : headers) {
					if (header.isDisplayed() && normalizeMessage(safeGetText(header)).contains(prefix)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean isDontHaveWalletFooterDisplayed() {
		List<WebElement> footers = driver.findElements(
				By.xpath("//p[contains(@class,'text-center') and contains(@class,'font-semibold')]"));
		for (WebElement footer : footers) {
			if (!footer.isDisplayed()) {
				continue;
			}
			String text = normalizeMessage(safeGetText(footer));
			if (text.contains("don't have") || text.contains("dont have")) {
				return true;
			}
		}
		return false;
	}

	public boolean isDownloadNowLinkDisplayed() {
		try {
			WebElement downloadLink = waitForElementVisible(By.id("download_now"));
			return downloadLink.isDisplayed();
		} catch (Exception e) {
			return false;
		}
	}

	public String getDownloadNowLinkHref() {
		WebElement downloadLink = waitForElementVisible(By.id("download_now"));
		return downloadLink.getAttribute("href");
	}

	public String getWalletQrCodeSrc() {
		WebElement qrCode = waitForElementVisible(By.id("wallet-qr-code"));
		return qrCode.getAttribute("src");
	}

	public String clickWalletQrCodeAndCaptureDeepLink() {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript(
				"window.__capturedDeepLink = null;"
						+ "if (!window.__originalWindowOpen) { window.__originalWindowOpen = window.open; }"
						+ "window.open = function(url) { window.__capturedDeepLink = url; return null; };");
		String originalWindow = driver.getWindowHandle();
		WebElement qrButton = waitForElementVisible(By.id("wallet-qr-btn"));
		clickOnElement(qrButton, "Clicked wallet QR code to capture deep link");
		String deepLink = (String) js.executeScript(
				"window.open = window.__originalWindowOpen; return window.__capturedDeepLink;");
		closeExtraBrowserWindows(originalWindow);
		return deepLink;
	}

	private void closeExtraBrowserWindows(String originalWindow) {
		Set<String> handles = driver.getWindowHandles();
		for (String handle : handles) {
			if (!handle.equals(originalWindow)) {
				driver.switchTo().window(handle);
				driver.close();
			}
		}
		driver.switchTo().window(originalWindow);
	}

	public boolean waitForWalletAuthenticateProgressDisplayed() {
		int waitSeconds = Math.max(30, LinkAuthUtil.getMaxUiWaitSeconds());
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds));
		try {
			wait.until(webDriver -> isWalletAuthenticateProgressDisplayed()
					|| (!isWalletQrCodeDisplayed() && hasWalletLinkedSessionIndicators())
					|| (!isWalletQrCodeDisplayed() && isWalletLinkSessionActive()));
			return isWalletAuthenticateProgressDisplayed() || hasWalletLinkedSessionIndicators()
					|| (!isWalletQrCodeDisplayed() && isWalletLinkSessionActive());
		} catch (TimeoutException e) {
			return false;
		}
	}

	public void waitForWalletSessionAfterLinkScan() {
		waitForWalletAuthenticateProgressDisplayed();
	}

	private boolean isWalletLinkSessionActive() {
		return !isWalletQrCodeDisplayed() && !isWalletQrExpiredMessageVisible();
	}

	private boolean hasWalletLinkedSessionIndicators() {
		if (isWalletQrCodeDisplayed()) {
			return false;
		}
		String pageSource = normalizeMessage(driver.getPageSource());
		return pageSource.contains("authenticate") || pageSource.contains("don't refresh")
				|| pageSource.contains("dont refresh") || pageSource.contains("wallet");
	}

	public boolean isWalletAuthenticateProgressDisplayed() {
		if (isWalletQrCodeDisplayed()) {
			return false;
		}
		for (WebElement indicator : driver.findElements(By.cssSelector(".loading-indicator"))) {
			if (!indicator.isDisplayed()) {
				continue;
			}
			String text = normalizeMessage(safeGetText(indicator));
			if (text.contains("authenticate") || text.contains("don't refresh")
					|| text.contains("dont refresh")) {
				return true;
			}
		}
		String expectedMessage = ResourceBundleLoader.get("loadingMsgs.link_auth_waiting");
		if (!expectedMessage.startsWith("!!MISSING_KEY:")) {
			String normalizedExpected = normalizeMessage(expectedMessage);
			for (WebElement indicator : driver.findElements(By.cssSelector(".loading-indicator"))) {
				if (indicator.isDisplayed()
						&& normalizeMessage(safeGetText(indicator)).contains(normalizedExpected.split("\\{\\{")[0])) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean waitForWalletQrCodeExpiredMessage() {
		int waitSeconds = LinkAuthUtil.getConfiguredLinkCodeExpireSeconds()
				+ parseTimeoutProperty("injiQrExpiredUiWaitBufferSeconds", 90);
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds));
		try {
			wait.until(driver -> isWalletQrExpiredMessageVisible());
			return true;
		} catch (TimeoutException e) {
			return false;
		}
	}

	public boolean isWalletQrExpiredMessageVisible() {
		String bannerText = getVisibleErrorBannerText();
		if (bannerText.contains("expired")) {
			return true;
		}
		String expectedMessage = ResourceBundleLoader.get("errors.wallet.qr_code_expired");
		return !expectedMessage.startsWith("!!MISSING_KEY:")
				&& bannerText.contains(normalizeMessage(expectedMessage));
	}

	public boolean isRefreshQrCodeButtonDisplayed() {
		List<WebElement> refreshButtons = driver.findElements(By.id("refresh_qr_code"));
		return refreshButtons.stream().anyMatch(WebElement::isDisplayed);
	}

	public boolean isWalletQrCodeImageLoaded() {
		try {
			String src = getWalletQrCodeSrc();
			return src != null && src.startsWith("data:image") && src.length() > 1000;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isWalletQrCodeWithEmbeddedLogo() {
		try {
			String src = getWalletQrCodeSrc();
			return src != null && src.startsWith("data:image") && src.length() > 4000;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isLinkCodeLimitErrorVisible() {
		String bannerText = normalizeMessage(getVisibleErrorBannerText());
		String expected = ResourceBundleLoader.get("errors.link_code_limit_reached");
		if (!expected.startsWith("!!MISSING_KEY:") && bannerText.contains(normalizeMessage(expected))) {
			return true;
		}
		return bannerText.contains("link code") && bannerText.contains("limit");
	}

	public boolean isInvalidQrConfigErrorVisible() {
		String bannerText = normalizeMessage(getVisibleErrorBannerText());
		String expected = ResourceBundleLoader.get("errors.wallet.invalid_qrcode_config");
		if (!expected.startsWith("!!MISSING_KEY:") && bannerText.contains(normalizeMessage(expected))) {
			return true;
		}
		return bannerText.contains("invalid qrcode configuration")
				|| bannerText.contains("invalid qrcode config");
	}

	public boolean waitForRedirectToRelyingPartyWithError(String errorCode) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(45));
		try {
			wait.until(webDriver -> {
				String url = webDriver.getCurrentUrl().toLowerCase();
				return url.contains("error=" + errorCode.toLowerCase())
						|| url.contains("error%3d" + errorCode.toLowerCase());
			});
			return true;
		} catch (TimeoutException e) {
			return false;
		}
	}

	public String getCurrentUrlErrorCode() {
		String url = driver.getCurrentUrl();
		int errorIndex = url.indexOf("error=");
		if (errorIndex < 0) {
			errorIndex = url.indexOf("error%3D");
			if (errorIndex >= 0) {
				return extractUrlParam(url.substring(errorIndex + 9));
			}
			return null;
		}
		return extractUrlParam(url.substring(errorIndex + 6));
	}

	private String extractUrlParam(String remainder) {
		int ampIndex = remainder.indexOf('&');
		String value = ampIndex >= 0 ? remainder.substring(0, ampIndex) : remainder;
		try {
			return java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8);
		} catch (Exception e) {
			return value;
		}
	}

	public void clickRefreshQrCodeButton() {
		WebElement refreshButton = waitForElementVisible(By.id("refresh_qr_code"));
		clickOnElement(refreshButton, "Clicked refresh QR code button");
	}

	public boolean waitForWalletQrCodeSrcChange(String previousSrc) {
		int waitSeconds = LinkAuthUtil.getConfiguredLinkCodeExpireSeconds()
				+ parseTimeoutProperty("injiQrExpiredUiWaitBufferSeconds", 90);
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds));
		try {
			wait.until(driver -> {
				List<WebElement> qrCodes = driver.findElements(By.id("wallet-qr-code"));
				for (WebElement qrCode : qrCodes) {
					if (qrCode.isDisplayed()) {
						String currentSrc = qrCode.getAttribute("src");
						return currentSrc != null && !currentSrc.isBlank() && !currentSrc.equals(previousSrc);
					}
				}
				return false;
			});
			return true;
		} catch (TimeoutException e) {
			return false;
		}
	}

	public boolean waitForLinkAuthWaitingMessage(String walletName) {
		int waitSeconds = parseTimeoutProperty("injiLinkAuthWaitingTimeoutSeconds", 30);
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds));
		String expectedFragment = normalizeMessage(walletName);
		try {
			wait.until(driver -> {
				String containerText = normalizeMessage(safeGetText(driver.findElement(By.tagName("body"))));
				return containerText.contains("authenticate") && containerText.contains(expectedFragment);
			});
			return true;
		} catch (TimeoutException e) {
			return false;
		}
	}

	public boolean isLinkAuthWaitingIndicatorDisplayed() {
		List<WebElement> indicators = driver.findElements(By.cssSelector(".loading-indicator"));
		return indicators.stream().anyMatch(WebElement::isDisplayed);
	}

	public boolean isLanguageDropdownDisplayed() {
		return isElementVisible(languageDropdown, "Verified language dropdown is visible");
	}

	public void clickOnLanguageDropdown() {
		clickOnElement(languageDropdown, "Clicked on language dropdown");
	}

	public void clickOnHindiLanguage() {
		clickOnElement(hindiLanguage, "Selected hindi language from dropdown");
	}

	public boolean isSelectedLanguageDisplayed() {
		return isElementVisible(loginWithOtpBtn, "Verified selected language displayed");
	}

	public boolean isLoginWithBiometicDisplayed() {
		return isElementVisible(loginWithBiometricBtn, "Verified login with biometric button is displayed");
	}

	public boolean isLoginWithInjiDisplayed() {
		return isElementVisible(loginWithInjiBtn, "Verified login with inji button is displayed");
	}

	public boolean isLoginWithPasswordDisplayed() {
		return isElementVisible(loginWithPasswordBtn, "Verified login with password button is displayed");
	}

	public List<WebElement> getLoginOptions() {
		List<WebElement> options = new ArrayList<>();
		options.add(loginWithOtpBtn);
		options.add(loginWithBiometricBtn);
		options.add(loginWithInjiBtn);
		options.add(loginWithPasswordBtn);
		options.add(loginWithPinBtn);
		options.add(loginWithKbiBtn);
		return options;
	}

	public boolean isMoreWaysToSignInOptionDisplayed() {
		return !moreWaysToSignIn.isEmpty() && moreWaysToSignIn.get(0).isDisplayed();
	}

	public boolean isLoginWithKbiDisplayed() {
		return isElementDisplayed(loginWithKbiBtn);
	}

	// KBI can sit behind the "more ways to sign in" expander when the client offers more than a few
	// auth factors; reveal it first so clickOnLoginWithKbi() finds the button.
	public void revealMoreOptionsIfPresent() {
		if (!isElementDisplayed(loginWithKbiBtn) && isMoreWaysToSignInOptionDisplayed()) {
			clickOnElement(moreWaysToSignIn.get(0), "Clicked on more ways to sign in");
		}
	}

	public void clickOnLoginWithKbi() {
		clickOnElement(loginWithKbiBtn, "Clicked on login with KBI");
	}

	public Map<String, WebElement> getAcrToElementMap() {
		Map<String, WebElement> map = new HashMap<>();
		map.put("PWD", loginWithPasswordBtn);
		map.put("OTP", loginWithOtpBtn);
		map.put("BIO", loginWithBiometricBtn);
		map.put("WLA", loginWithInjiBtn);
		map.put("PIN", loginWithPinBtn);
		map.put("KBI", loginWithKbiBtn);
		return map;
	}

	public void selectLanguage(String language) {
		WebElement langOption = waitForElementVisible(
				By.xpath("//button[@role='option' and normalize-space()=" + toXpathLiteral(language) + "]"));
		clickOnElement(langOption, "Selected language option: " + language);
		// Selecting a language triggers an async re-fetch/re-render of the whole page (new /flow/meta
		// call for the chosen language, nav bar re-render, etc.) - wait for the dropdown button itself
		// to reflect the new selection before returning, so callers that immediately interact with the
		// page again (including BaseTest's auto-switch racing a scenario's own language step) don't hit
		// a stale/mid-transition DOM.
		By navLanguageButton = By.cssSelector("nav button[aria-haspopup='listbox']");
		new WebDriverWait(driver, Duration.ofSeconds(EsignetConfigManager.getTimeout())).until(d -> {
			List<WebElement> buttons = d.findElements(navLanguageButton);
			return !buttons.isEmpty() && buttons.get(0).getText().trim().contains(language);
		});
	}

	/** Opens the dropdown and switches the UI to the given 3-letter language code's display name;
	 *  no-ops if unmapped. */
	public void selectLanguageByCode(String languageCode) {
		String displayName = utils.LanguageUtil.getDisplayName(languageCode);
		if (displayName == null || displayName.equals(languageCode)) {
			return;
		}
		clickOnLanguageDropdown();
		selectLanguage(displayName);
	}

	public boolean isUILanguageChanged(String text) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
		wait.until(ExpectedConditions.textToBePresentInElement(loginButton, text));
		return loginButton.getText().contains(text);
	}

	public WebElement getLoginWithOtpButton() {
		return loginWithOtpBtn;
	}

	public String getLoginWithOtpButtonText() {
		waitForElementVisible(loginWithOtpBtn);
		return loginWithOtpBtn.getText().trim();
	}

	public boolean isMobileNumberOptionDisplayed() {
		return isElementVisible(mobileNumberOption, "Verified mobile number option is displayed for authentication");
	}

	public void clickOnMobileNumberOption() {
		clickOnElement(mobileNumberOption, "Selected mobile number as the login ID type");
	}

	public boolean isNrcIdOptionDisplayed() {
		return isElementVisible(nrcIdOption, "Verified nrc option is displayed for authentication");
	}

	public boolean isVidOptionDisplayed() {
		return isElementVisible(vidOption, "Verified vid option is displayed for authentication");
	}

	public boolean isEmailOptionDisplayed() {
		return isElementVisible(emailOption, "Verified email option is displayed for authentication");
	}

	public boolean isBackButtonDisplayed() {
		return isElementVisible(backButton, "Verified back button is visible for return to Select a preferred mode");
	}

	public void clickOnBackButton() {
		clickOnElement(backButton, "Clicked on back button");
	}

	public void clickOnLoginWithBiometric() {
		clickOnElement(loginWithBiometricBtn, "Clicked on login with biometrics");
	}

	public void clickOnLoginWithPassword() {
		clickOnElement(loginWithPasswordBtn, "Clicked on login with password");
	}

	public boolean isGetOtpButtonEnabled() {
		return isButtonEnabled(getOtpButton, "Verified get otp button is enabled");
	}

	public boolean isMobileNumberSelected() {
		return isElementVisible(mobileSelected, "Verified mobile number seleted in authentication screen");
	}

	// "Displayed" for a native <select>'s <option> doesn't mean visually rendered (that's the
	// browser/OS's own dropdown chrome, invisible to WebDriver until opened) - it means the option
	// genuinely exists as a selectable choice. Checking via Select.getOptions() is the correct way
	// to interact with a native select in Selenium.
	public boolean isKhmCountryCodePrefixDisplayed() {
		waitForElementVisible(prefixNumberField);
		return new Select(prefixNumberField).getOptions().stream()
				.anyMatch(option -> "+855".equals(option.getAttribute("value")));
	}

	public boolean isIndCountryCodePrefixDisplayed() {
		waitForElementVisible(prefixNumberField);
		return new Select(prefixNumberField).getOptions().stream()
				.anyMatch(option -> "+91".equals(option.getAttribute("value")));
	}

	public void clickOnPrefixNumberFieldButton() {
		clickOnElement(prefixNumberField, "Clicked on Prefix Number select field");
	}

	public void clickOnIndCountryCodePrefix() {
		waitForElementVisible(prefixNumberField);
		new Select(prefixNumberField).selectByValue("+91");
	}

	public void clickOnKhmCountryCodePrefix() {
		waitForElementVisible(prefixNumberField);
		new Select(prefixNumberField).selectByValue("+855");
	}

	public boolean isOtpInputFieldIsDisplayed() {
		// otpInputFields.isEmpty() has no wait built in - called right after clicking Get OTP, it can
		// race the page transition and see the list still empty even though the OTP screen is about to
		// render. Poll for at least one box to show up first instead of checking once immediately.
		try {
			new WebDriverWait(driver, Duration.ofSeconds(EsignetConfigManager.getTimeout()))
					.until(d -> !otpInputFields.isEmpty());
		} catch (TimeoutException e) {
			return false;
		}
		return isElementVisible(otpInputFields.get(0), "Verified otp input field is displayed");
	}

	/** Types one OTP digit per box, in order - the OTP field is 6 separate single-character inputs. */
	public void enterOtp(String otp) {
		enterOtpDigits(otpInputFields, otp,
				(field, digit) -> enterText(field, String.valueOf(digit), "Entered OTP digit"));
	}

	public void clickOnSubmitOtpButton() {
		clickOnElement(submitOtpButton, "Clicked on submit OTP button");
	}

	public boolean isAttentionScreenIsDisplayed() {
		return isElementVisible(attentionScreen, "Verified attention screen is displayed");
	}

	public void clickOnAttentionCancelButton() {
		clickOnElement(attentionCancelButton, "Clicked on attention cancel button");
	}

	public void clickOnAttentionDiscontinueButton() {
		clickOnElement(attentionDiscontinueButton, "Clicked on attention discontinue button");
	}

	public void clickOnVidOptionButton() {
		clickOnElement(vidOption, "Clicked on vid option button");
	}

	public boolean isInvalidIndividualIdErrorMessageIsDisplayed() {
		return isElementVisible(invalidIndividualIdErrorMessage,
				"Verified invalid individual id error message is displayed");
	}

	public boolean waitForOtpAuthenticationDeniedForInfant() {
		long deadline = System.currentTimeMillis() + 30_000L;
		while (System.currentTimeMillis() < deadline) {
			if (isAttentionScreenIsDisplayed()) {
				return false;
			}
			if (!getVisibleErrorBannerText().isBlank()) {
				return true;
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}
		return false;
	}

	public String getOtpAuthenticationDenialDetails() {
		String banner = getVisibleErrorBannerText();
		if (!banner.isBlank()) {
			return banner;
		}
		if (isAttentionScreenIsDisplayed()) {
			return "Unexpected navigation to attention screen after infant OTP verify";
		}
		return "No error banner displayed after infant OTP verify";
	}

	public void enterVid(String vid) {
		waitForElementVisible(idInputField);
		idInputField.clear();
		enterText(idInputField, vid, "Entered vid in vid field");
	}

	public void clickOnEmailOptionButton() {
		clickOnElement(emailOption, "Clicked on email option button");
	}

	public void enterEmail(String email) {
		waitForElementVisible(idInputField);
		idInputField.clear();
		enterText(idInputField, email, "Entered email in email field");
	}

	public boolean isBiometricIntegrationContainerDisplayed() {
		return isElementVisible(biometricIntegrationContainer,
				"Verified secure biometric interface integration container is displayed");
	}

	public boolean isBiometricScreenActive() {
		return isBiometricIntegrationContainerVisibleNow()
				&& (isBiometricVidOptionVisibleNow() || isBiometricVidTextFieldVisibleNow());
	}

	private boolean isBiometricIntegrationContainerVisibleNow() {
		List<WebElement> containers = driver.findElements(By.id("secure-biometric-interface-integration"));
		return !containers.isEmpty() && containers.get(0).isDisplayed();
	}

	private boolean isBiometricVidOptionVisibleNow() {
		List<WebElement> options = driver.findElements(By.id("vid"));
		return !options.isEmpty() && options.get(0).isDisplayed();
	}

	private boolean isBiometricVidTextFieldVisibleNow() {
		List<WebElement> fields = driver.findElements(By.id("sbi_vid"));
		return !fields.isEmpty() && fields.get(0).isDisplayed();
	}

	public boolean isBiometricVidOptionDisplayed() {
		return isElementVisible(vidOption, "Verified UIN/VID option is displayed on biometric screen");
	}

	public void clickOnBiometricVidOptionButton() {
		clickOnElement(vidOption, "Clicked on UIN/VID option on biometric screen");
	}

	/**
	 * SBI widget re-renders after Mock MDS retry can hide the UIN/VID input until the tab is selected again.
	 */
	public void ensureBiometricVidFieldVisible() {
		if (isBiometricVidTextFieldVisibleNow()) {
			return;
		}
		if (isBiometricVidOptionVisibleNow()) {
			clickOnBiometricVidOptionButton();
		}
		waitForElementVisible(biometricVidField);
	}

	public boolean isBiometricVidTextFieldDisplayed() {
		return isElementVisible(biometricVidField, "Verified VID text field is displayed on biometric screen");
	}

	private static final String SCANNING_DEVICES_MSG_KEY = "loadingMsgs.scanning_devices_msg";
	private volatile long lastBiometricRescanAttemptMs;
	private volatile boolean rescanActivitySeen;

	public boolean waitForScanningDevicesOrDeviceDiscovered() {
		int waitSeconds = Math.max(getBiometricScanningWaitSeconds(), getBiometricDeviceDiscoveryTimeoutSeconds());
		long deadline = System.currentTimeMillis() + waitSeconds * 1000L;
		while (System.currentTimeMillis() < deadline) {
			if (isScanningDevicesMessageVisible() || isBiometricDeviceDiscovered()) {
				return true;
			}
			if (MockMdsManager.isRunning() && isBrowserSbiDeviceCachePopulated()) {
				return true;
			}
			if (lastBiometricRescanAttemptMs > 0) {
				if (isScanningDevicesMessageVisible() || isBiometricDeviceDiscovered()
						|| isDeviceNotFoundMessageVisible()
						|| (MockMdsManager.isRunning() && isBrowserSbiDeviceCachePopulated())) {
					rescanActivitySeen = true;
				}
			}
			if (isRecentBiometricRescanCompleted()) {
				return true;
			}
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		return isBiometricDeviceDiscovered() || isRecentBiometricRescanCompleted();
	}

	private boolean isRecentBiometricRescanCompleted() {
		if (lastBiometricRescanAttemptMs <= 0
				|| System.currentTimeMillis() - lastBiometricRescanAttemptMs > getBiometricDeviceDiscoveryTimeoutSeconds()
						* 1000L) {
			return false;
		}
		if (!rescanActivitySeen) {
			return false;
		}
		if (isBiometricDeviceDiscovered()) {
			return true;
		}
		if (isScanningDevicesMessageVisible()) {
			return true;
		}
		if (MockMdsManager.isRunning() && isBrowserSbiDeviceCachePopulated()) {
			return true;
		}
		return !MockMdsManager.isRunning() && isDeviceNotFoundMessageVisible();
	}

	private boolean isScanningDevicesMessageVisible() {
		return isLocalizedTextVisibleWithinBiometricContainer(SCANNING_DEVICES_MSG_KEY)
				|| isTextVisibleWithinBiometricContainer("scanning devices");
	}

	public boolean isScanningDevicesMessageDisplayed() {
		return waitForLocalizedTextWithinBiometricContainer(SCANNING_DEVICES_MSG_KEY, getBiometricScanningWaitSeconds());
	}

	public boolean isRetryScanButtonNotDisplayedWhileScanning() {
		if (!waitForLocalizedTextWithinBiometricContainer(SCANNING_DEVICES_MSG_KEY, 5)) {
			return true;
		}
		return !isRetryScanButtonVisible();
	}

	public boolean waitForDeviceNotFoundMessageDisplayed() {
		int waitSeconds = getBiometricDeviceDiscoveryTimeoutSeconds();
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds));
		try {
			wait.until(driver -> isDeviceNotFoundMessageVisible());
			return true;
		} catch (TimeoutException e) {
			return false;
		}
	}

	private static final By ICON_RETRY_BUTTON_SELECTOR = By.cssSelector(
			"#secure-biometric-interface-integration button[type='button'].sbd-cursor-pointer.sbd-ml-1, "
					+ "#secure-biometric-interface-integration div.sbd-dropdown_container + button[type='button'], "
					+ "#secure-biometric-interface-integration div.sbd-flex button[type='button'].sbd-cursor-pointer");

	public void clickOnBiometricDeviceScanRetryButton() {
		if (isBiometricDeviceDiscovered()) {
			return;
		}
		int waitSeconds = getBiometricDeviceDiscoveryTimeoutSeconds();
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds));
		try {
			WebElement retryButton = wait.until(driver -> findDisplayedRetryButton());
			clearBrowserSbiDeviceCache();
			lastBiometricRescanAttemptMs = System.currentTimeMillis();
			rescanActivitySeen = true;
			clickOnElement(retryButton, "Clicked on biometric device scan retry button");
			triggerBrowserSbiDiscovery();
			return;
		} catch (TimeoutException e) {
			if (isBiometricDeviceDiscovered()) {
				return;
			}
			forceBrowserBiometricRescan();
		}
	}

	public void syncBiometricWidgetIfMockMdsRunning() {
		if (!MockMdsManager.isRunning() || !isBiometricScreenActive()) {
			return;
		}
		long deadline = System.currentTimeMillis() + getBiometricDeviceDiscoveryTimeoutSeconds() * 1000L;
		while (System.currentTimeMillis() < deadline && !isBiometricDeviceDiscovered()) {
			injectMockMdsDeviceCacheIfRunning();
			triggerBiometricRescanViaWidget();
			if (findDisplayedRetryButton() != null) {
				clickOnBiometricDeviceScanRetryButton();
				break;
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		if (!isBiometricDeviceDiscovered()) {
			reenterBiometricLoginAfterMockMdsStart();
		}
	}

	/**
	 * When Mock MDS starts after the widget already scanned with no device, going back and re-opening
	 * biometric login forces a fresh SBI discovery pass (more reliable than retry alone).
	 */
	public void reenterBiometricLoginAfterMockMdsStart() {
		if (!MockMdsManager.isRunning()) {
			return;
		}
		try {
			List<WebElement> backButtons = driver.findElements(By.id("back-button"));
			for (WebElement backButton : backButtons) {
				if (backButton.isDisplayed()) {
					clickOnElement(backButton, "Navigated back to re-enter biometric login after Mock MDS start");
					break;
				}
			}
			if (isLoginWithBiometricsOptionVisible()) {
				clickOnElement(loginWithBiometricBtn, "Re-opened Login with Biometrics after Mock MDS start");
			}
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(getBiometricScanningWaitSeconds()));
			wait.until(driver -> isBiometricIntegrationContainerVisibleNow());
			if (isBiometricVidOptionVisibleNow()) {
				clickOnBiometricVidOptionButton();
			}
			lastBiometricRescanAttemptMs = System.currentTimeMillis();
			rescanActivitySeen = true;
			triggerBrowserSbiDiscovery();
			injectMockMdsDeviceCacheIfRunning();
		} catch (Exception e) {
			forceBrowserBiometricRescan();
		}
	}

	public void forceBrowserBiometricRescan() {
		lastBiometricRescanAttemptMs = System.currentTimeMillis();
		rescanActivitySeen = true;
		clearBrowserSbiDeviceCache();
		triggerBrowserSbiDiscovery();
		injectMockMdsDeviceCacheIfRunning();
		if (!triggerBiometricRescanViaWidget() && isBiometricVidOptionVisibleNow()) {
			clickOnBiometricVidOptionButton();
		}
	}

	private void injectMockMdsDeviceCacheIfRunning() {
		if (!MockMdsManager.isRunning() || MockMdsManager.getActivePort() <= 0) {
			return;
		}
		java.util.Map<String, String> cacheEntries = MockMdsManager
				.buildBrowserSbiCacheEntries(MockMdsManager.getActivePort());
		if (cacheEntries.isEmpty()) {
			return;
		}
		try {
			((JavascriptExecutor) driver).executeScript(
					"const entries = arguments[0];"
							+ "try {"
							+ "  if (entries.discover) { localStorage.setItem('discover', entries.discover); }"
							+ "  if (entries.deviceInfo) { localStorage.setItem('deviceInfo', entries.deviceInfo); }"
							+ "  window.dispatchEvent(new Event('storage'));"
							+ "} catch (e) {}",
					cacheEntries);
			rescanActivitySeen = true;
		} catch (Exception ignored) {
			// Best-effort cache seed before widget rescan.
		}
	}

	private void triggerBrowserSbiDiscovery() {
		int scriptTimeoutSeconds = Math.max(getBiometricDeviceDiscoveryTimeoutSeconds(), 45);
		try {
			driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(scriptTimeoutSeconds));
			((JavascriptExecutor) driver).executeAsyncScript(
					"const done = arguments[arguments.length - 1];"
							+ "const decodeJwtPayload = (token) => {"
							+ "  try {"
							+ "    const payload = token.split('.')[1];"
							+ "    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');"
							+ "    const padded = normalized + '='.repeat((4 - normalized.length % 4) % 4);"
							+ "    return JSON.parse(atob(padded));"
							+ "  } catch (e) { return null; }"
							+ "};"
							+ "const isValidDevice = (info) => info && info.certification === 'L1'"
							+ "  && info.purpose === 'Auth' && info.deviceStatus === 'Ready';"
							+ "const ports = Array.from({length: 10}, (_, index) => 4501 + index);"
							+ "const discover = {};"
							+ "const deviceInfo = {};"
							+ "Promise.all(ports.map(async (port) => {"
							+ "  try {"
							+ "    const discResponse = await fetch('http://127.0.0.1:' + port + '/device', {"
							+ "      method: 'MOSIPDISC',"
							+ "      headers: {'Content-Type': 'application/json'},"
							+ "      body: JSON.stringify({type: 'Biometric Device'})"
							+ "    });"
							+ "    if (!discResponse.ok) { return; }"
							+ "    const discData = await discResponse.json();"
							+ "    discover[port] = discData;"
							+ "    const infoResponse = await fetch('http://127.0.0.1:' + port + '/info', {"
							+ "      method: 'MOSIPDINFO'"
							+ "    });"
							+ "    if (!infoResponse.ok) { return; }"
							+ "    const infoData = await infoResponse.json();"
							+ "    if (!Array.isArray(infoData)) { return; }"
							+ "    const decodedDevices = [];"
							+ "    for (const item of infoData) {"
							+ "      const decoded = decodeJwtPayload(item.deviceInfo);"
							+ "      if (!decoded) { continue; }"
							+ "      if (typeof decoded.digitalId === 'string') {"
							+ "        decoded.digitalId = decodeJwtPayload(decoded.digitalId);"
							+ "      }"
							+ "      if (isValidDevice(decoded)) { decodedDevices.push(decoded); }"
							+ "    }"
							+ "    if (decodedDevices.length > 0) { deviceInfo[port] = decodedDevices; }"
							+ "  } catch (e) {}"
							+ "})).finally(() => {"
							+ "  try {"
							+ "    if (Object.keys(discover).length > 0) {"
							+ "      localStorage.setItem('discover', JSON.stringify(discover));"
							+ "    }"
							+ "    if (Object.keys(deviceInfo).length > 0) {"
							+ "      localStorage.setItem('deviceInfo', JSON.stringify(deviceInfo));"
							+ "      window.dispatchEvent(new Event('storage'));"
							+ "    }"
							+ "  } catch (e) {}"
							+ "  done(true);"
							+ "});");
			rescanActivitySeen = true;
		} catch (Exception ignored) {
			// Browser async discovery is best-effort; widget retry paths still run.
		}
	}

	private void clearBrowserSbiDeviceCache() {
		try {
			((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
					"try {"
							+ "localStorage.removeItem('deviceInfo');"
							+ "localStorage.removeItem('discover');"
							+ "localStorage.removeItem('deviceInfos');"
							+ "} catch (e) {}");
		} catch (Exception ignored) {
			// Best-effort cache reset before rescan.
		}
	}

	private boolean triggerBiometricRescanViaWidget() {
		try {
			Object triggered = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
					"const root = document.querySelector('#secure-biometric-interface-integration');"
							+ "if (!root) { return false; }"
							+ "try {"
							+ "localStorage.removeItem('deviceInfo');"
							+ "localStorage.removeItem('discover');"
							+ "localStorage.removeItem('deviceInfos');"
							+ "} catch (e) {}"
							+ "const candidates = root.querySelectorAll('button, a, [role=\"button\"], span');"
							+ "for (const element of candidates) {"
							+ "  const label = (element.textContent || '').trim().toLowerCase();"
							+ "  if (label.includes('retry') || label.includes('try again')) {"
							+ "    element.click(); return true;"
							+ "  }"
							+ "}"
							+ "const alert = root.querySelector(\"div[role='alert']\");"
							+ "if (alert) {"
							+ "  const tryAgain = Array.from(alert.querySelectorAll('*')).find(el =>"
							+ "    (el.textContent || '').toLowerCase().includes('try again'));"
							+ "  if (tryAgain) { tryAgain.click(); return true; }"
							+ "  alert.click();"
							+ "}"
							+ "const vidTab = root.querySelector('#vid, input[id=\"vid\"], input[name=\"vid\"]');"
							+ "if (vidTab) { vidTab.focus(); vidTab.blur(); return true; }"
							+ "return alert != null;");
			return Boolean.TRUE.equals(triggered);
		} catch (Exception e) {
			return false;
		}
	}

	private WebElement findDisplayedRetryButton() {
		String retryTextXpath = "contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ',"
				+ "'abcdefghijklmnopqrstuvwxyz'),'retry') or contains(translate(normalize-space(.),"
				+ "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'try again')";
		List<By> locators = List.of(
				By.xpath("//div[@id='secure-biometric-interface-integration']//button[" + retryTextXpath + "]"),
				By.xpath("//div[@id='secure-biometric-interface-integration']//*[@role='button' and ("
						+ retryTextXpath + ")]"),
				By.xpath("//div[@id='secure-biometric-interface-integration']//a[" + retryTextXpath + "]"),
				By.xpath("//div[@id='secure-biometric-interface-integration']//span[" + retryTextXpath + "]"),
				By.cssSelector("#secure-biometric-interface-integration div[role='alert'] button"),
				By.cssSelector(
						"#secure-biometric-interface-integration button.sbd-cursor-pointer.sbd-block.sbd-w-full"));
		for (By locator : locators) {
			try {
				for (WebElement candidate : driver.findElements(locator)) {
					if (candidate.isDisplayed() && isRetryScanButtonElement(candidate)) {
						return candidate;
					}
				}
			} catch (StaleElementReferenceException ignored) {
				// SBI widget re-renders during scan; retry on next poll.
			}
		}
		return null;
	}

	private boolean isRetryScanButtonElement(WebElement element) {
		try {
			String tagName = element.getTagName();
			if ("input".equalsIgnoreCase(tagName)) {
				return false;
			}
			String className = element.getAttribute("class");
			if (className != null && className.contains("sbd-bg-gradient")) {
				String label = normalizeMessage(safeGetText(element));
				if (!label.contains("retry") && !label.contains("try again")) {
					return false;
				}
			}
			String label = normalizeMessage(safeGetText(element));
			if (label.contains("retry") || label.contains("try again")) {
				return true;
			}
			return className != null && className.contains("sbd-block");
		} catch (StaleElementReferenceException e) {
			return false;
		}
	}

	public void enterBiometricVid(String vid) {
		ensureBiometricVidFieldVisible();
		biometricVidField.clear();
		enterText(biometricVidField, vid, "Entered UIN/VID in biometric field");
		syncBiometricWidgetIfMockMdsRunning();
	}

	public void clearBiometricVidField() {
		ensureBiometricVidFieldVisible();
		biometricVidField.clear();
		biometricVidField.sendKeys(org.openqa.selenium.Keys.TAB);
	}

	public boolean isBiometricScanAndVerifyButtonDisplayed() {
		return findBiometricScanAndVerifyButtons().stream().anyMatch(WebElement::isDisplayed);
	}

	public boolean isBiometricScanAndVerifyButtonEnabled() {
		try {
			List<WebElement> buttons = findBiometricScanAndVerifyButtons();
			for (WebElement button : buttons) {
				try {
					if (button.isDisplayed()) {
						String disabled = button.getAttribute("disabled");
						return disabled == null || "false".equalsIgnoreCase(disabled);
					}
				} catch (StaleElementReferenceException ignored) {
					// SBI widget re-renders during scan; retry on next poll.
				}
			}
		} catch (StaleElementReferenceException ignored) {
			// SBI widget re-renders during scan; retry on next poll.
		}
		return false;
	}

	public boolean isBiometricVidFieldValidationMessageDisplayed() {
		ensureBiometricVidFieldVisible();
		Object message = ((org.openqa.selenium.JavascriptExecutor) driver)
				.executeScript("arguments[0].reportValidity(); return arguments[0].validationMessage;", biometricVidField);
		return message != null && !message.toString().isBlank();
	}

	public void clickMoreWaysToSignInIfVisible() {
		List<WebElement> moreOptions = driver.findElements(By.id("show-more-options"));
		if (!moreOptions.isEmpty() && moreOptions.get(0).isDisplayed()) {
			clickOnElement(moreOptions.get(0), "Clicked on More ways to sign in");
		}
	}

	public boolean isLoginWithBiometricsOptionVisible() {
		return isElementVisible(loginWithBiometricBtn, "Verified Login with Biometrics option is visible");
	}

	public boolean isL0OrUnregisteredDeviceNotAvailable() {
		String containerText = getBiometricContainerText();
		if (containerText.contains("l0")) {
			return false;
		}
		List<WebElement> deviceOptions = driver.findElements(
				By.cssSelector("#secure-biometric-interface-integration .sbd-dropdown_container option, "
						+ "#secure-biometric-interface-integration .sbd-dropdown_container li"));
		for (WebElement option : deviceOptions) {
			if (!option.isDisplayed()) {
				continue;
			}
			String text = normalizeMessage(safeGetText(option));
			if (text.contains("l0") || text.contains("unregistered")) {
				return false;
			}
		}
		return true;
	}

	public boolean waitForBiometricErrorMessageContaining(String... partialMessages) {
		int waitSeconds = getBiometricAuthenticationTimeoutSeconds();
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds));
		try {
			wait.until(driver -> isBiometricErrorMessageVisible(partialMessages));
			return true;
		} catch (TimeoutException e) {
			return false;
		}
	}

	public void dismissBiometricErrorBannerIfVisible() {
		List<WebElement> closeButtons = driver.findElements(By.id("error-close-button"));
		for (WebElement closeButton : closeButtons) {
			if (closeButton.isDisplayed()) {
				clickOnElement(closeButton, "Dismissed biometric error banner");
				return;
			}
		}
	}

	public void clickBiometricBackButtonIfVisible() {
		if (isBackButtonDisplayed()) {
			clickOnBackButton();
		}
	}

	public void attemptClickBiometricScanAndVerifyButtonWithoutWait() {
		List<WebElement> buttons = findBiometricScanAndVerifyButtons();
		for (WebElement button : buttons) {
			if (button.isDisplayed()) {
				try {
					button.click();
				} catch (Exception ignored) {
					// Expected when the SBI widget keeps the button disabled.
				}
				return;
			}
		}
	}

	public boolean waitForBiometricDeviceDiscovered() {
		int waitSeconds = getBiometricDeviceDiscoveryTimeoutSeconds();
		long deadline = System.currentTimeMillis() + waitSeconds * 1000L;
		while (System.currentTimeMillis() < deadline) {
			if (isBiometricDeviceDiscovered()) {
				return true;
			}
			if (MockMdsManager.isRunning()) {
				syncBiometricWidgetIfMockMdsRunning();
				injectMockMdsDeviceCacheIfRunning();
				triggerBiometricRescanViaWidget();
				if (findDisplayedRetryButton() != null) {
					clickOnBiometricDeviceScanRetryButton();
				} else {
					triggerBrowserSbiDiscovery();
					injectMockMdsDeviceCacheIfRunning();
				}
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		return isBiometricDeviceDiscovered();
	}

	public boolean waitForDeviceNotFoundMessageToClear() {
		int waitSeconds = getBiometricDeviceDiscoveryTimeoutSeconds();
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds));
		try {
			wait.until(driver -> !isDeviceNotFoundMessageVisible() && isBiometricDeviceDiscovered());
			return true;
		} catch (TimeoutException e) {
			return false;
		}
	}

	public boolean isDeviceNotFoundMessageDisplayed() {
		return isDeviceNotFoundMessageVisible();
	}

	public void clickBiometricScanAndVerifyButton() {
		syncBiometricWidgetIfMockMdsRunning();
		int waitSeconds = getBiometricAuthenticationTimeoutSeconds();
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds));
		try {
			WebElement scanButton = wait.until(ExpectedConditions.elementToBeClickable(getBiometricScanAndVerifyButtonLocator()));
			clickOnElement(scanButton, "Clicked biometric scan and verify button");
			return;
		} catch (TimeoutException e) {
			syncBiometricWidgetIfMockMdsRunning();
		}
		wait = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds));
		WebElement scanButton = wait.until(ExpectedConditions.elementToBeClickable(getBiometricScanAndVerifyButtonLocator()));
		clickOnElement(scanButton, "Clicked biometric scan and verify button");
	}

	private By getBiometricScanAndVerifyButtonLocator() {
		return By.xpath("//div[@id='secure-biometric-interface-integration']//button[contains("
				+ "translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'scan') "
				+ "or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'verify')]");
	}

	private List<WebElement> findBiometricScanAndVerifyButtons() {
		return driver.findElements(getBiometricScanAndVerifyButtonLocator());
	}

	private boolean isBiometricErrorMessageVisible(String... partialMessages) {
		String bannerText = getVisibleErrorBannerText();
		if (bannerText.isBlank() || partialMessages.length == 0) {
			return false;
		}
		if (partialMessages.length == 1) {
			String partial = partialMessages[0];
			return partial != null && !partial.isBlank()
					&& bannerText.contains(normalizeMessage(partial));
		}
		for (String partial : partialMessages) {
			if (partial != null && !partial.isBlank()
					&& bannerText.contains(normalizeMessage(partial))) {
				return true;
			}
		}
		return false;
	}

	private String getVisibleErrorBannerText() {
		try {
			List<WebElement> banners = driver.findElements(By.id("error-banner-message"));
			for (WebElement banner : banners) {
				if (banner.isDisplayed()) {
					return normalizeMessage(safeGetText(banner));
				}
			}
		} catch (StaleElementReferenceException ignored) {
			return "";
		}
		return "";
	}

	public boolean waitForBiometricAuthenticationSuccess() {
		int waitSeconds = getBiometricAuthenticationTimeoutSeconds();
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds));
		try {
			wait.until(driver -> isBiometricAuthenticationSuccess());
			return true;
		} catch (TimeoutException e) {
			return false;
		}
	}

	public String getBiometricAuthenticationFailureDetails() {
		String banner = getVisibleErrorBannerText();
		if (banner.isBlank()) {
			return "";
		}
		return " (UI error: " + banner + ")";
	}

	private int getBiometricScanningWaitSeconds() {
		return parseTimeoutProperty("biometricScanningWaitSeconds", 15);
	}

	private int getBiometricDeviceDiscoveryTimeoutSeconds() {
		return parseTimeoutProperty("biometricDeviceDiscoveryTimeoutSeconds", 30);
	}

	private int getBiometricAuthenticationTimeoutSeconds() {
		return parseTimeoutProperty("biometricAuthenticationTimeoutSeconds", 60);
	}

	private int parseTimeoutProperty(String propertyName, int defaultValue) {
		try {
			String value = EsignetConfigManager.getproperty(propertyName);
			if (value == null || value.isBlank()) {
				return defaultValue;
			}
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private boolean waitForLocalizedTextWithinBiometricContainer(String resourceKey, int timeoutSeconds) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
		try {
			wait.until(driver -> isLocalizedTextVisibleWithinBiometricContainer(resourceKey));
			return true;
		} catch (TimeoutException e) {
			return false;
		}
	}

	private boolean isDeviceNotFoundMessageVisible() {
		if (hasBiometricDeviceDropdown() || isBiometricScanAndVerifyButtonEnabled()) {
			return false;
		}

		String containerText = getBiometricContainerText();
		if (containerText.contains("device not found") && containerText.contains("connectivity")) {
			return true;
		}

		try {
			List<WebElement> alerts = driver.findElements(
					By.cssSelector("#secure-biometric-interface-integration div[role='alert']"));
			for (WebElement alert : alerts) {
				if (alert.isDisplayed()) {
					String alertText = normalizeMessage(safeGetText(alert));
					if (alertText.contains("device not found") && alertText.contains("connectivity")) {
						return true;
					}
				}
			}
		} catch (StaleElementReferenceException ignored) {
			// DOM is still updating while SBI scans for devices; retry on next wait poll.
		}

		String expectedMessage = ResourceBundleLoader.get("errors.no_devices_found_msg");
		if (expectedMessage.startsWith("!!MISSING_KEY:")) {
			LOGGER.warn("errors.no_devices_found_msg is missing from the resource bundle - "
					+ "device-not-found detection relied only on the English literal check above.");
			return false;
		}
		return containerText.contains(normalizeMessage(expectedMessage));
	}

	private boolean isTextVisibleWithinBiometricContainer(String normalizedPartialText) {
		if (normalizedPartialText == null || normalizedPartialText.isBlank()) {
			return false;
		}
		return getBiometricContainerText().contains(normalizeMessage(normalizedPartialText));
	}

	private String getBiometricContainerText() {
		try {
			if (!biometricIntegrationContainer.isDisplayed()) {
				return "";
			}
			return normalizeMessage(safeGetText(biometricIntegrationContainer));
		} catch (StaleElementReferenceException e) {
			return "";
		}
	}

	private String safeGetText(WebElement element) {
		try {
			return element.getText();
		} catch (StaleElementReferenceException e) {
			return "";
		}
	}

	private boolean isLocalizedTextVisibleWithinBiometricContainer(String resourceKey) {
		String expectedMessage = ResourceBundleLoader.get(resourceKey);
		if (expectedMessage == null || expectedMessage.startsWith("!!MISSING_KEY:")) {
			return false;
		}
		return isTextVisibleWithinBiometricContainer(normalizeMessage(expectedMessage));
	}

	private boolean isRetryScanButtonVisible() {
		return findDisplayedRetryButton() != null;
	}

	private boolean isBiometricDeviceDiscovered() {
		if (isLocalizedTextVisibleWithinBiometricContainer(SCANNING_DEVICES_MSG_KEY)) {
			return false;
		}
		if (hasBiometricDeviceDropdown()) {
			return true;
		}
		return isBiometricScanAndVerifyButtonDisplayed();
	}

	private boolean isBrowserSbiDeviceCachePopulated() {
		try {
			Object result = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
					"try {"
							+ "const d = JSON.parse(localStorage.getItem('deviceInfo') || '{}');"
							+ "return Object.keys(d).length > 0;"
							+ "} catch (e) { return false; }");
			return Boolean.TRUE.equals(result);
		} catch (Exception e) {
			return false;
		}
	}

	private boolean hasBiometricDeviceDropdown() {
		List<WebElement> deviceDropdowns = driver.findElements(By.cssSelector(
				"#secure-biometric-interface-integration .sbd-dropdown_container, "
						+ "#secure-biometric-interface-integration select"));
		return deviceDropdowns.stream().anyMatch(WebElement::isDisplayed);
	}

	private boolean isBiometricAuthenticationSuccess() {
		String currentUrl = driver.getCurrentUrl();
		if (currentUrl != null) {
			if (currentUrl.contains("claim-details") || currentUrl.contains("/consent")
					|| currentUrl.contains("userprofile") || currentUrl.contains("code=")) {
				return true;
			}
		}
		return driver.findElements(By.id("continue")).stream().anyMatch(WebElement::isDisplayed);
	}

	private String normalizeMessage(String message) {
		return message == null ? "" : message.replaceAll("\\s+", " ").trim().toLowerCase();
	}

}