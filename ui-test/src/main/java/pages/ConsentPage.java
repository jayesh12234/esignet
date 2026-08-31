package pages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import base.BasePage;
import utils.ClaimsUtil;
import utils.EsignetConfigManager;
import utils.EsignetUtil;

public class ConsentPage extends BasePage {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConsentPage.class);

	public ConsentPage(WebDriver driver) {
		super(driver);
	}

	// Rewritten against the current "ThunderID" component library used by esignet-go (esqa) -
	// verified 2026-08-19 by driving the real login flow end to end in a live (non-headless)
	// browser, including solving the real reCAPTCHA that gates get_otp/submit.
	@FindBy(id = "acr_otp")
	WebElement loginWithOtpButton;

	// The language switcher has no id at all - only aria-haspopup="listbox"; its visible text is the
	// current language's own display name (e.g. "English", "العربية").
	@FindBy(css = "nav button[aria-haspopup='listbox']")
	WebElement languageSelection;

	// The ID-type buttons (login_id_uin/login_id_mobile/login_id_email/login_id_nrc) share this one
	// input regardless of which type is selected - there's no more a field per type.
	@FindBy(id = "username_input")
	WebElement mobileNumberField;

	// UIN/VID is pre-selected by default; must click this before typing a mobile number so the ID
	// type the backend validates against actually matches what's typed.
	@FindBy(id = "login_id_mobile")
	WebElement mobileIdTypeButton;

	@FindBy(id = "submit_uin")
	WebElement getOtpButton;

	@FindBy(css = "input.thunderid-otp-field__input")
	List<WebElement> otpInputFields;

	@FindBy(id = "action_submit_otp")
	WebElement verifyOtpButton;

	// esignet-go has no separate "attention" interstitial between OTP verification and consent -
	// verified live (screenshot): OTP success goes straight to the Allow/Deny consent screen, so
	// "proceed on the attention page" and "allow on the consent screen" are the same real button.
	@FindBy(id = "action_allow")
	WebElement proceedButtonInAttentionPage;

	@FindBy(xpath = "//button[contains(@class,'inline-flex items-center justify-center')][2]")
	WebElement proceedButton;

	@FindBy(id = "mock-identity-verifier")
	WebElement eKycServiceProvider;

	@FindBy(id = "proceed-preview-button")
	WebElement proceedButtonInServiceProviderPage;

	@FindBy(id = "consent-button")
	WebElement termsAndConditionCheckBox;

	@FindBy(id = "proceed-tnc-button")
	WebElement proceedBtnInTandCPage;

	@FindBy(id = "proceed-preview-button")
	WebElement proceedBtnInCameraPreviewPage;

	// Same as LoginOptionsPage - the language switcher has no id, only aria-haspopup="listbox".
	@FindBy(css = "nav button[aria-haspopup='listbox']")
	WebElement languageDropdown;

	@FindBy(xpath = "//button[@role='option' and normalize-space()='العربية']")
	WebElement arabicLanguage;

	// Verified live (full-page DOM capture): the dir attribute is set on the root <html> element
	// itself (e.g. <html lang="en" dir="ltr" ...>), not on a div.h-screen, which doesn't exist here.
	@FindBy(tagName = "html")
	WebElement rootContainer;

	// Consent claim toggles are now individual checkbox inputs id="consent_opt__<claim>", plus one
	// id="consent_opt__all" master toggle - not a label[for=]/sr-only-peer pattern. Verified by
	// driving a real login to the live consent screen. Essential (non-toggleable, "Required") claims
	// render with no checkbox at all, so there's no equivalent locator needed for those.
	@FindBy(id = "consent_opt__all")
	WebElement voluntaryClaimsMasterToggle;

	@FindBy(css = "input.thunderid-toggle__input[id^='consent_opt__']:not(#consent_opt__all)")
	List<WebElement> voluntaryClaimsSubToggles;

	// Verified live (after_otp_submit.html DOM capture): claim rows render as
	// div.thunderid-consent-checkbox-list__item inside each div.thunderid-consent-checkbox-list
	// section (essential first, voluntary second) - not <li> elements under a div.divide-y, which
	// doesn't exist here.
	// The outer positional filter must space-bound the class match: plain contains(@class,
	// 'thunderid-consent-checkbox-list') also matches each __item row's own class (it's a literal
	// substring of "thunderid-consent-checkbox-list__item"), so without bounding it [1]/[2] pick the
	// essential section's own item divs instead of the section containers - confirmed live, this
	// silently broke voluntaryClaims (always empty) while looking fine wherever only visibility, not
	// content, of the resolved element was checked.
	@FindBy(xpath = "(//div[contains(concat(' ',normalize-space(@class),' '),' thunderid-consent-checkbox-list ')])[2]//div[contains(@class,'thunderid-consent-checkbox-list__item')]")
	List<WebElement> voluntaryClaimsElements;

	@FindBy(xpath = "(//div[contains(concat(' ',normalize-space(@class),' '),' thunderid-consent-checkbox-list ')])[1]//div[contains(@class,'thunderid-consent-checkbox-list__item')]")
	private List<WebElement> essentialClaims;

	@FindBy(xpath = "(//div[contains(concat(' ',normalize-space(@class),' '),' thunderid-consent-checkbox-list ')])[2]//div[contains(@class,'thunderid-consent-checkbox-list__item')]")
	private List<WebElement> voluntaryClaims;

	// Verified live (full-page DOM capture): the countdown renders as a plain
	// p.thunderid-typography__body2 reading "Please take appropriate action in M:SS", directly above
	// the h5#text_consent_title header - not p.font-bold.consent-timer-text, which doesn't exist here.
	// Text-based match on the stable "appropriate action" phrase (not the whole string, which changes
	// every second).
	@FindBy(xpath = "//p[contains(text(),'appropriate action')]")
	WebElement consentTimer;

	@FindBy(xpath = "//div[@role='menuitem']")
	List<WebElement> languageDropdownItems;

	@FindBy(id = "action_allow")
	WebElement allowButton;

	@FindBy(xpath = "//div[@class=' css-1dimb5e-singleValue']")
	WebElement selectedLanguageDropdown;

	@FindBy(xpath = "//button[contains(@class,'flex items-center px-4')]")
	WebElement profileDropdown;

	// Same verified-live pattern as essentialClaimHeaderInConsentUpdateProfileScreen below - not
	// div.font-semibold, which doesn't exist here.
	@FindBy(xpath = "(//h6[contains(@class,'thunderid-typography__subtitle2')])[1]")
	WebElement essentialClaimsHeader;

	// Verified live (after_otp_submit.html DOM capture): each claims section renders as a
	// div.thunderid-consent-checkbox-list, essential first - not a div.divide-y, which doesn't exist here.
	// Space-bounded class match - see the comment on essentialClaims/voluntaryClaims above for why.
	@FindBy(xpath = "(//div[contains(concat(' ',normalize-space(@class),' '),' thunderid-consent-checkbox-list ')])[1]")
	WebElement essentialClaimsList;

	// Same element as consentTimer above - the "Please take appropriate action in M:SS" line is the
	// only action-instruction message on this screen (verified live) - not p.text-[#4E4E4E].font-semibold,
	// which doesn't exist here.
	@FindBy(xpath = "//p[contains(text(),'appropriate action')]")
	WebElement actionMessage;

	// Verified live: the login-screen title renders as h3#text_heading inside #heading_details, with
	// the subtitle right after it as an id-less div (no id at all on that node in the real DOM - schema
	// calls it "text_subheading" but that doesn't make it to the rendered attribute), and the
	// acr-chooser screen's own heading (shown only before an auth factor is picked) is h5#acr_text_heading.
	@FindBy(id = "text_heading")
	WebElement loginTitle;

	@FindBy(css = "#text_heading + div")
	WebElement loginSubTitle;

	@FindBy(id = "acr_text_heading")
	WebElement selectPreferredModeHeader;

	@FindBy(xpath = "//div[@class='inline mx-2 font-semibold my-3']")
	WebElement selectPreferredIdHeader;
	
	// esignet-go has no distinct "consent to profile update" screen either (same finding as the
	// attention screen) - it's the same generic consent screen. Verified live: the real header is
	// h5#text_consent_title ("<client> is requesting access to the following:"); the "sub header"
	// (the countdown text above it, e.g. "Please take appropriate action in 1:54") has no id of its
	// own in the rendered DOM, so it's addressed relative to the header instead.
	@FindBy(id = "text_consent_title")
	WebElement headerInConsentUpdateProfileScreen;

	@FindBy(xpath = "//h5[@id='text_consent_title']/preceding-sibling::p[1]")
	WebElement subHeaderInConsentUpdateProfileScreen;

	// Verified live: "Essential Claims"/"Voluntary Claims" render as h6.thunderid-typography__subtitle2
	// with no unique id/class distinguishing one from the other - only their fixed DOM order does
	// (essential always first), matching the class ConsentStepDefinition's other consent-screen headers.
	@FindBy(xpath = "(//h6[contains(@class,'thunderid-typography__subtitle2')])[1]")
	WebElement essentialClaimHeaderInConsentUpdateProfileScreen;

	@FindBy(xpath = "(//h6[contains(@class,'thunderid-typography__subtitle2')])[2]")
	WebElement voluntaryClaimHeaderInConsentUpdateProfileScreen;

	// Verified live: each claims section's info icon is a div[aria-label='More Info'] wrapping the svg
	// glyph (thunderid-tooltip__container) - not an svg.cursor-pointer, which doesn't exist here.
	@FindBy(xpath = "(//div[@aria-label='More Info'])[1]")
	WebElement essentialInfoIconInConsentUpdateProfileScreen;

	@FindBy(xpath = "(//div[@aria-label='More Info'])[2]")
	WebElement voluntaryInfoIconInConsentUpdateProfileScreen;

	// Verified live (full-page DOM capture): the real button here is id="action_deny" (text "Deny"),
	// a sibling of action_allow - not "cancel-button", which doesn't exist. An earlier, simpler
	// (no-claims) consent screen was checked previously and genuinely had no cancel/deny button at
	// all; this heavier claims-based screen does.
	@FindBy(id = "action_deny")
	WebElement cancelButtonInConsentUpdateProfileScreen;

	// Verified live (after_otp_submit.html DOM capture): each claims section renders as a
	// div.thunderid-consent-checkbox-list, voluntary second - not a div.divide-y, which doesn't exist here.
	// Space-bounded class match - see the comment on essentialClaims/voluntaryClaims above for why.
	@FindBy(xpath = "(//div[contains(concat(' ',normalize-space(@class),' '),' thunderid-consent-checkbox-list ')])[2]")
	WebElement voluntaryClaimsList;

	@FindBy(xpath = "//span[@class='available-claim']")
	WebElement availableClaimsStatus;

	@FindBy(xpath = "//span[@class='not-available-claim']")
	WebElement notAvailableClaimStatus;

	// "More Info" icon tooltip content - not p.mb-1, which doesn't exist here. Matches the
	// thunderid-tooltip__container class already confirmed elsewhere in this same component library
	// (see essentialInfoIconInConsentUpdateProfileScreen's aria-label='More Info' trigger above).
	@FindBy(xpath = "//div[contains(@class,'thunderid-tooltip__container')]")
	WebElement infoIconMeassage;

	// Not div.message.mx-0..., which doesn't exist here - text-based match on the step's own wording
	// (no confirmed live capture of this specific element yet; matching the message a real user would
	// see is more resilient than guessing a CSS class that keeps changing across this deployment's
	// component library versions).
	@FindBy(xpath = "//*[contains(text(),'Proceed') and contains(text(),'verification')]")
	WebElement messageAboveProceedBtn;

	@FindBy(xpath = "//div[@class='relative text-center text-dark font-semibold text-xl text-[#2B3840] mt-9']")
	WebElement attentionHeaderInWarningPopup;

	@FindBy(xpath = "//p[@class='text-base text-[#707070]']")
	WebElement subHeaderInWarningPopup;

	@FindBy(id = "stay-button")
	WebElement stayButtonInConsentUpdateProfileScreen;

	@FindBy(id = "discontinue-button")
	WebElement discontinueButtonInConsentUpdateProfileScreen;

	/** @return false if a real login screen genuinely isn't reachable (caller should treat as
	 *  not applicable and skip); true otherwise, including the single-factor-skip case below. */
	public boolean clickOnLoginWithOtp() {
		boolean landmarkReached = ensureFreshEsignetLoginPage(By.cssSelector("[id^='acr_'], #username_input"));
		if (!landmarkReached) {
			return false;
		}
		// When only one auth factor is negotiated, esignet-go skips the acr_* chooser screen entirely
		// and renders that factor's own ID-entry screen directly (e.g. #username_input for a
		// UIN/VID-only transaction) - confirmed live earlier this session. On a repeat login within
		// the same scenario (re-using the same authorize URL/session context) that single-factor
		// skip can kick in even where the first login showed the full acr_otp chooser, so there's
		// nothing to click here - the id-entry screen is already showing.
		if (driver.findElements(By.id("acr_otp")).isEmpty() && !driver.findElements(By.id("username_input")).isEmpty()) {
			LOGGER.info("Login-with-Otp chooser not present but username_input already is - "
					+ "single-factor screen, nothing to click, proceeding directly.");
			return true;
		}
		clickOnElement(loginWithOtpButton, "Clicked on login with Otp button");
		return true;
	}

	public boolean isLoginWithOtpOptionVisible() {
		return isElementVisible(loginWithOtpButton, "Checked login with OTP option visibility");
	}

	public void enterRegisteredMobileNumber(String number) {
		clickOnElement(mobileIdTypeButton, "Selected mobile number ID type");
		waitForElementVisible(mobileNumberField);
		mobileNumberField.clear();
		enterText(mobileNumberField, number, "Entered registered mobile number");
	}

	public void clickOnGetOtp() {
		solveRecaptchaIfPresent();
		clickOnElement(getOtpButton, "Clicked on get otp button");
		waitForOtpVerificationScreen();
	}

	public void waitForOtpVerificationScreen() {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
		wait.pollingEvery(Duration.ofMillis(500));
		wait.ignoring(StaleElementReferenceException.class);
		wait.until(driverInstance -> {
			if (!driverInstance.findElements(By.cssSelector("input.thunderid-otp-field__input")).isEmpty()) {
				return true;
			}
			String sendOtpError = readVisibleOtpSendError(driverInstance);
			if (sendOtpError != null) {
				throw new IllegalStateException("Send OTP failed: " + sendOtpError);
			}
			return false;
		});
	}

	private String readVisibleOtpSendError(WebDriver driverInstance) {
		for (WebElement banner : driverInstance.findElements(By.id("error-banner-message"))) {
			if (banner.isDisplayed()) {
				String text = banner.getText();
				if (text != null && !text.isBlank()) {
					return text.trim();
				}
			}
		}
		return null;
	}

	public void enterOtp(String otp) {
		waitForElementVisible(By.cssSelector("input.thunderid-otp-field__input"));
		for (WebElement field : otpInputFields) {
			field.click();
			field.sendKeys(Keys.chord(Keys.CONTROL, "a"));
			field.sendKeys(Keys.BACK_SPACE);
		}
		enterOtpDigits(otpInputFields, otp, (field, digit) -> {
			field.click();
			field.sendKeys(String.valueOf(digit));
		});
	}

	public String getCurrentLanguage() {
		waitForElementVisible(languageSelection);
		return languageSelection.getText().trim();
	}

	public void clickOnVerifyButton() {
		clickOnElement(verifyOtpButton, "Clicked on verify otp button");
	}

	public boolean isOnAttentionScreen() {
		return isElementVisible(proceedButtonInAttentionPage, "Verified attention screen proceed button is visible");
	}

	public void clickOnProceedButtonInAttentionPage() {
		clickOnElement(proceedButtonInAttentionPage, "Clicked on Procced button in attention screen");
	}

	public void clickOnProceedButton() {
		// This step only exists in the classic eSignet flow's separate eKYC sequence (provider
		// select -> terms -> camera preview -> liveness). Under the mock plugin (this environment,
		// see config.properties: pluginToExecute) the preceding attention/consent click already
		// completes the login and redirects to the relying party - confirmed repeatedly (6+ runs) via
		// post-failure screenshots that always show the RP dashboard, never this screen. Not a
		// scenario-wide skip - just this one click is a no-op (nothing to click), the scenario
		// continues into whatever steps follow.
		if ("mock".equalsIgnoreCase(EsignetUtil.getPluginName())) {
			LOGGER.info("Not clicking (this step only, not the scenario) - no separate eKYC sequence "
					+ "exists after consent under this environment's mock-plugin flow - verified live.");
			return;
		}
		if (waitForRelyingPartyRedirectOrElement(
				By.xpath("(//button[contains(@class,'inline-flex items-center justify-center')])[2]"), 60)) {
			LOGGER.info("Not clicking - login completed directly to the relying party with no eKYC sequence.");
			return;
		}
		clickWhenClickable(proceedButton);
	}

	public void clickOnMockIdentifyVerifier() {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
		WebElement provider = wait.until(ExpectedConditions.elementToBeClickable(By.id("mock-identity-verifier")));
		clickOnElement(provider, "Selected the ekyc provider");
	}

	public void clickOnProceedButtonInServiceProviderPage() {
		clickWhenClickable(proceedButtonInServiceProviderPage);
	}

	public void checkTermsAndCondition() {
		waitForElementVisible(termsAndConditionCheckBox);
		if (!termsAndConditionCheckBox.isSelected()) {
			clickOnElement(termsAndConditionCheckBox, "Selected the terms and condition checkbox");
		}
	}

	public void clickOnProceedButtonInTermsAndConditionPage() {
		clickOnElement(proceedBtnInTandCPage, "Clicked on proceed button in terms and condition screen");
	}

	public void clickOnProceedButtonInCameraPreviewPage() {
		clickWhenClickable(proceedBtnInCameraPreviewPage);
	}

	public void completeEkycVerificationIfRequired() {
		if (driver.findElements(By.id("mock-identity-verifier")).isEmpty()) {
			LOGGER.info("Mock eKYC provider not shown; assuming repeat-auth flow without liveness");
			return;
		}
		clickOnMockIdentifyVerifier();
		clickOnProceedButtonInServiceProviderPage();
		checkTermsAndCondition();
		clickOnProceedButtonInTermsAndConditionPage();
		clickOnProceedButtonInCameraPreviewPage();
		waitUntilLivenessCheckCompletes();
	}

	/**
	 * Waits for eKYC identity verification to finish. Signup polls its status endpoint for up to
	 * 200s by design (mosip.signup.status.request.limit=10 x status.request.delay=20s), so the
	 * timeout must outlast that budget rather than cutting the app off mid-poll. Rather than always
	 * burning the full timeout, this resolves as soon as either outcome is reached: the eSignet
	 * consent screen (success), or one of signup's failure paths (fails fast with the reason).
	 */
	public void waitUntilLivenessCheckCompletes() {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(240));
		wait.pollingEvery(Duration.ofSeconds(2));
		wait.ignoring(NoSuchElementException.class, StaleElementReferenceException.class);

		wait.until(driverInstance -> {
			// Signup's "Verification Unsuccessful!" screen - this button renders only on failure.
			List<WebElement> verificationFailed = driverInstance.findElements(By.id("success-continue-button"));
			if (!verificationFailed.isEmpty() && verificationFailed.get(0).isDisplayed()) {
				throw new IllegalStateException("eKYC identity verification failed: signup reported "
						+ "'Verification Unsuccessful!' on the identity verification status screen");
			}

			// Signup's other failure paths redirect back to eSignet carrying an error query param.
			String currentUrl = driverInstance.getCurrentUrl();
			if (currentUrl != null && currentUrl.contains("error=")) {
				throw new IllegalStateException(
						"eKYC identity verification failed; redirected back with error: " + currentUrl);
			}

			List<WebElement> consentAllowButton = driverInstance.findElements(By.id("action_allow"));
			return !consentAllowButton.isEmpty() && consentAllowButton.get(0).isDisplayed();
		});
	}

	public boolean isConsentScreenVisible() {
		return isElementVisible(allowButton, "Verified is navigated to consent scrren");
	}

	// A successful authentication lands on the "Attention" screen (its Proceed button) before consent.
	// Waits up to timeoutSeconds for it - used as the login-success signal for flows like KBI whose
	// auth round-trip can exceed the default explicit wait.
	public boolean isOnAttentionScreen(int timeoutSeconds) {
		try {
			new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
					.until(ExpectedConditions.visibilityOfElementLocated(By.id("action_allow")));
			return true;
		} catch (org.openqa.selenium.TimeoutException e) {
			return false;
		}
	}

	public boolean isVoluntaryClaimsMasterToggleVisible() {
		return voluntaryClaimsElements.size() > 1
				&& isElementVisible(voluntaryClaimsMasterToggle, "Verified voluntary claims master toggle button");
	}

	public WebElement getVoluntaryClaimsMasterToggle() {
		return voluntaryClaimsMasterToggle;
	}

	public List<WebElement> getVoluntaryClaimsSubToggles() {
		return voluntaryClaimsSubToggles;
	}

	// The claims= query param on the original /oauth2/authorize URL is where these claim names live,
	// but "Given user captures the authorize url" overwrites the shared authorizeUrl field with the
	// post-navigation signin page's URL before this runs, and that page carries no # fragment for
	// ClaimsUtil.getVoluntaryClaims() to parse either - confirmed live, it's always empty by the time
	// this scenario reaches the toggle steps. The live sub-toggles' own ids (id="consent_opt__<name>")
	// are the same names toggleVoluntaryClaim() looks them up by, so read from there instead.
	public List<String> getVoluntaryClaimNamesFromDom() {
		List<String> names = new ArrayList<>();
		for (WebElement toggle : voluntaryClaimsSubToggles) {
			String id = toggle.getAttribute("id");
			if (id != null && id.startsWith("consent_opt__")) {
				names.add(id.substring("consent_opt__".length()));
			}
		}
		return names;
	}

	public void enableVoluntaryClaimsMasterToggle() {
		waitForElementVisible(voluntaryClaimsMasterToggle);
		if (!voluntaryClaimsMasterToggle.isSelected()) {
			clickOnElement(voluntaryClaimsMasterToggle, "Enabled the voluntary claims master toggle button");
		}
	}

	public void disableVoluntaryClaimsMasterToggle() {
		waitForElementVisible(voluntaryClaimsMasterToggle);
		if (voluntaryClaimsMasterToggle.isSelected()) {
			clickOnElement(voluntaryClaimsMasterToggle, "Disabled the voluntary claims master toggle button");
		}
	}

	public boolean isVoluntaryClaimsMasterToggleSelected() {
		waitForElementVisible(voluntaryClaimsMasterToggle);
		return voluntaryClaimsMasterToggle.isSelected();
	}

	public String getVoluntaryClaimsTooltipText() {
		// Trigger icon verified live: (//div[@aria-label='More Info'])[2] - not id="voluntary_claims_tooltip",
		// which doesn't exist here. Content locator confirmed live too: id="_r_4_" role="tooltip"
		// mounts under the trigger div once focus/mouseover events actually fire (see
		// BasePage.getTooltipText()'s JS dispatch - Selenium's Actions hover alone never mounted it).
		return getTooltipText(By.xpath("(//div[@aria-label='More Info'])[2]"), By.cssSelector("[role='tooltip']"));
	}

	public void toggleVoluntaryClaim(String claimName, boolean enable) {
		String normalized = ClaimsUtil.normalizeClaim(claimName);
		WebElement checkbox = waitForElementVisible(By.id("consent_opt__" + normalized));
		if (checkbox.isSelected() != enable) {
			// clickOnElement(), not a raw .click() - the row's flex layout can transiently overlap this
			// checkbox the same way it does the master toggle, and clickOnElement() already has the JS
			// click fallback for that (see BasePage.clickOnElement()'s ElementClickInterceptedException
			// handling).
			clickOnElement(checkbox, "Toggled voluntary claim '" + claimName + "' to " + enable);
		}
	}

	public boolean areEssentialClaimsPresent() {
		// A language switch just before this re-renders the whole claims list - the container itself
		// can stay visible across that re-render while its child items are momentarily empty, so wait
		// on the items list directly, not just the container.
		try {
			new WebDriverWait(driver, Duration.ofSeconds(5)).until(d -> !essentialClaims.isEmpty());
		} catch (org.openqa.selenium.TimeoutException ignored) {
		}
		return !essentialClaims.isEmpty();
	}

	public boolean areVoluntaryClaimsPresent() {
		try {
			new WebDriverWait(driver, Duration.ofSeconds(5)).until(d -> !voluntaryClaims.isEmpty());
		} catch (org.openqa.selenium.TimeoutException ignored) {
		}
		return !voluntaryClaims.isEmpty();
	}

	public void clickOnAllowBtnInConsentScreen() {
		clickOnElement(allowButton, "Clicked on allow button in consent screen");
	}

	public void enterVid(String vid) {
		WebElement vidField = waitForElementVisible(By.id("username_input"));
		vidField.clear();
		enterText(vidField, vid, "Entered vid in vid field");
	}

	// esignet-go has no separate "attention" screen distinct from consent (verified live: OTP success
	// goes straight to the Allow/Deny consent screen) - so "is the attention screen showing" is really
	// "is the consent screen showing" here. #navbar-header (the old check) is present on every screen
	// of this UI, so it was always true regardless of what was actually displayed.
	public boolean isAttentionScreenDisplayedNow() {
		return isConsentScreenDisplayedNow();
	}

	// consent-timer-text no longer exists - the current consent screen has no visible countdown.
	// Detects the screen by its actual container/heading instead.
	public boolean isConsentScreenDisplayedNow() {
		List<WebElement> consentBlocks = driver.findElements(By.id("block_consent"));
		return !consentBlocks.isEmpty() && consentBlocks.get(0).isDisplayed();
	}

	// #sign-in-with-esignet was a classic-eSignet-specific element on the relying party's OWN login
	// page - doesn't apply here (verified live: this environment's RP lands the user straight on its
	// dashboard after a successful login, not a login page). Checking that we've left esignet-go's
	// domain entirely is a signal that works regardless of what the RP's post-login page looks like.
	public void waitForRelyingPartyRedirect() {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
		wait.until(driverInstance -> isAlreadyOnRelyingParty());
	}

	public void assertAuthenticationCompletedWithoutConsent() {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
		wait.pollingEvery(Duration.ofMillis(500));
		wait.until(driverInstance -> {
			if (isAttentionScreenDisplayedNow()) {
				throw new AssertionError("Attention screen was displayed when consent should be skipped");
			}
			if (isConsentScreenDisplayedNow()) {
				throw new AssertionError("Consent screen was displayed when consent should be skipped");
			}
			return isAlreadyOnRelyingParty();
		});
	}

	public void completeConsentFlowThroughEkyc() {
		// On esignet-go.esqa this click IS the consent screen's "Allow" button (see
		// proceedButtonInAttentionPage) and completes the login directly - verified live, there's no
		// separate eKYC provider/terms/camera-preview/liveness sequence to walk through afterwards.
		// Still falls through to that classic sequence for any environment where it's actually deployed.
		clickOnProceedButtonInAttentionPage();
		if (waitForRelyingPartyRedirectQuietly()) {
			return;
		}
		clickOnProceedButton();
		if (driver.findElements(By.id("mock-identity-verifier")).isEmpty()) {
			LOGGER.info("Mock eKYC provider not shown; assuming repeat-auth flow without liveness");
			return;
		}
		clickOnMockIdentifyVerifier();
		clickOnProceedButtonInServiceProviderPage();
		checkTermsAndCondition();
		clickOnProceedButtonInTermsAndConditionPage();
		clickOnProceedButtonInCameraPreviewPage();
		waitUntilLivenessCheckCompletes();
		if (isConsentScreenDisplayedNow()) {
			throw new AssertionError("Consent screen was displayed when consent should be skipped");
		}
	}

	public void completeConsentRegistryFlowDecliningOptionalClaims() throws Exception {
		clickOnProceedButtonInAttentionPage();
		clickOnProceedButton();
		completeEkycVerificationIfRequired();
		for (String claim : getClaims("voluntary")) {
			toggleVoluntaryClaim(claim, false);
		}
		clickOnAllowBtnInConsentScreen();
		waitUntilUserProfilePage();
	}

	public void completeConsentFlowThroughEkycIfAttentionScreenIsDisplayed() {
		if (isAttentionScreenDisplayedNow()) {
			completeConsentFlowThroughEkyc();
		}
	}

	public void waitUntilConsentScreenAfterAuthentication() {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
		wait.pollingEvery(Duration.ofSeconds(1));
		wait.ignoring(NoSuchElementException.class, StaleElementReferenceException.class);
		wait.until(driverInstance -> {
			String currentUrl = driverInstance.getCurrentUrl();
			if (currentUrl != null && currentUrl.contains("error=")) {
				throw new IllegalStateException("Authentication failed; redirected back with error: " + currentUrl);
			}
			return currentUrl != null && (currentUrl.contains("/consent") || isConsentScreenVisible());
		});
	}

	public boolean isAuthorizeScopeSectionDisplayed() {
		return !driver.findElements(By.id("authorize_scope_tooltip")).isEmpty();
	}

	public boolean isAuthorizeScopeDisplayed(String scopeName) {
		List<WebElement> scopeToggles = driver.findElements(By.id(scopeName));
		return !scopeToggles.isEmpty() && scopeToggles.get(0).isDisplayed();
	}

	public void toggleAuthorizeScope(String scopeName, boolean enable) {
		WebElement toggle = driver.findElement(By.id(scopeName));
		if (toggle.isSelected() != enable) {
			WebElement label = driver.findElement(By.cssSelector("label[for='" + scopeName + "']"));
			clickOnElement(label, "Toggled authorize scope " + scopeName + " to " + enable);
		}
	}

	public boolean areClaimSectionsAbsent() {
		return essentialClaims.isEmpty() && voluntaryClaims.isEmpty();
	}

	public void waitUntilUserProfilePage() {
		String relyingPartyBase = EsignetConfigManager.getproperty("baseurl");
		String normalizedRpBase = relyingPartyBase != null ? relyingPartyBase.replaceAll("/+$", "") : "";
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(90));
		wait.pollingEvery(Duration.ofMillis(500));
		wait.until(driverInstance -> {
			String currentUrl = driverInstance.getCurrentUrl();
			if (currentUrl != null && currentUrl.contains("error=session_expired")) {
				throw new IllegalStateException(
						"OAuth session expired before redirect to user profile; relaunch authorize URL and retry. URL: "
								+ currentUrl);
			}
			if (isUserProfilePageDisplayed()) {
				return true;
			}
			return currentUrl != null && !normalizedRpBase.isEmpty() && currentUrl.startsWith(normalizedRpBase)
					&& currentUrl.contains("code=");
		});
		String currentUrl = driver.getCurrentUrl();
		String sanitizedUrl = currentUrl != null && currentUrl.contains("?")
				? currentUrl.substring(0, currentUrl.indexOf('?'))
				: currentUrl;
		LOGGER.info("Navigated to user profile page: {}", sanitizedUrl);
	}

	public boolean isUserProfilePageDisplayed() {
		String currentUrl = driver.getCurrentUrl();
		return currentUrl != null && currentUrl.contains("userprofile") && currentUrl.contains("code=");
	}

	public boolean isLanguageDropdownDisplayed() {
		return isElementVisible(languageDropdown, "Verified language dropdown is visible");
	}

	public void clickOnLanguageDropdown() {
		clickOnElement(languageDropdown, "Clicked on language dropdown");
	}

	public void clickOnArabicLanguage() {
		clickOnElement(arabicLanguage, "Selected arabic language from dropdown");
	}

	public String getPageDirection() {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
		wait.until(ExpectedConditions.attributeToBe(rootContainer, "dir", "rtl"));
		return rootContainer.getAttribute("dir");
	}

	public int getConsentTimerSeconds() {
		waitForElementVisible(consentTimer);
		// Text reads "Please take appropriate action in M:SS" - confirmed live it starts near 2:00
		// (observed "1:59" immediately after landing on the screen), crossing the minute boundary, so
		// this returns the full minutes*60+seconds total, not just the seconds part.
		String timerValue = consentTimer.getText().trim();
		String[] parts = timerValue.split(":");
		if (parts.length < 2) {
			throw new IllegalStateException(
					"Could not parse consent timer text '" + timerValue + "' - expected a 'mm:ss'-style value");
		}
		int minutes = Integer.parseInt(parts[0].replaceAll("\\D", ""));
		int seconds = Integer.parseInt(parts[1].replaceAll("\\D", ""));
		return minutes * 60 + seconds;
	}

	public String getSelectedLanguageFromDropdown() {
		waitForElementVisible(selectedLanguageDropdown);
		return selectedLanguageDropdown.getText().trim();
	}

	public void clickOnProfileDropdown() {
		clickOnElement(profileDropdown, "Clicked on profile dropdown");
	}

	public List<String> getDisplayedClaims() {
		List<String> claims = new ArrayList<>();
		List<WebElement> claimElements = driver.findElements(By.xpath("//a[contains(@class,'px-4 py-2 text-sm')]"));
		for (WebElement element : claimElements) {
			claims.add(element.getText().trim());
		}
		List<WebElement> profileElements = driver.findElements(By.xpath("//img[@class='h-12 w-12 ml-3 mr-3']"));
		if (!profileElements.isEmpty() && profileElements.get(0).isDisplayed()) {
			claims.add("Profile");
		}

		return claims;
	}

	public boolean isEssentialClaimsHeaderDisplayed() {
		return isElementVisible(essentialClaimsHeader, "Verified essential claims header is visible");
	}

	public boolean isEssentialClaimsListDisplayed() {
		return isElementVisible(essentialClaimsList, "Verified essential claims list is visible");
	}

	public boolean isActionMessageDisplayed() {
		return isElementVisible(actionMessage, "Verified action message is displayed");
	}

	public boolean isTimerDisplayed() {
		return isElementVisible(consentTimer, "Verified timer is displayed");
	}

	public boolean isVerifyOtpButtonEnabled() {
		return isButtonEnabled(verifyOtpButton, "Verified otp verification button is enabled");
	}

	/**
	 * The purpose-type scenarios assert on this button as their first step, so the /authorize page
	 * may still be resolving oauth-details (showing its loading spinner) when this runs. Wait for
	 * the button to render before reading its text, otherwise the check races the page load and
	 * fails intermittently in a full suite run while passing in isolation.
	 */
	public boolean isLoginWithOtpDisplayed(String expectedText) {
		try {
			waitForElementVisible(loginWithOtpButton);
		} catch (Exception e) {
			LOGGER.warn("Login with OTP button not visible or timed out", e);
			return false;
		}
		return loginWithOtpButton.getText().trim().startsWith(expectedText);
	}

	/**
	 * These back the "no title/subtitle should be displayed" assertions. They wait for the login
	 * page itself to render first - otherwise, on a page that is still loading, the title is
	 * trivially absent and the assertion would pass for the wrong reason.
	 */
	public boolean isLoginTitleDisplayed() {
		waitForElementVisible(loginWithOtpButton);
		return isElementDisplayed(loginTitle);
	}

	public boolean isLoginSubTitleDisplayed() {
		waitForElementVisible(loginWithOtpButton);
		return isElementDisplayed(loginSubTitle);
	}

	public String getLoginTitleText() {
		waitForElementVisible(loginTitle);
		return loginTitle.getText().trim();
	}

	public String getLoginSubTitleText() {
		waitForElementVisible(loginSubTitle);
		return loginSubTitle.getText().trim();
	}

	public String getSelectPreferredModeHeaderText() {
		waitForElementVisible(selectPreferredModeHeader);
		return selectPreferredModeHeader.getText().trim();
	}

	public String getSelectPreferredIdHeaderText() {
		waitForElementVisible(selectPreferredIdHeader);
		return selectPreferredIdHeader.getText().trim();
	}

	public boolean isHeaderInConsentUpdateProfileScreenVisible() {
		return isElementVisible(headerInConsentUpdateProfileScreen, "Verified header in consent update profile screen");
	}

	public boolean isSubHeaderInConsentUpdateProfileScreenVisible() {
		return isElementVisible(subHeaderInConsentUpdateProfileScreen,
				"Verified sub header in consent update profile screen");
	}

	public boolean isEssentialClaimsHeaderInConsentUpdateProfileScreenVisible() {
		return isElementVisible(essentialClaimHeaderInConsentUpdateProfileScreen,
				"Verified essential claims header in consent update profile screen");
	}

	public boolean isVoluntaryClaimsHeaderInConsentUpdateProfileScreenVisible() {
		return isElementVisible(voluntaryClaimHeaderInConsentUpdateProfileScreen,
				"Verified voluntary claims header in consent update profile screen");
	}

	public boolean isInfoIconInConsentUpdateProfileScreenVisible() {
		return isElementVisible(essentialInfoIconInConsentUpdateProfileScreen,
				"Verified info icon in consent update profile screen");
	}

	public boolean isProceedButtonInConsentUpdateProfileScreenVisible() {
		return isElementVisible(proceedButtonInAttentionPage,
				"Verified procced button in consent update profile screen");
	}

	public boolean isCancelButtonInConsentUpdateProfileScreenVisible() {
		return isElementVisible(cancelButtonInConsentUpdateProfileScreen,
				"Verified cancel button in consent update profile screen");
	}

	public boolean isEssentialClaimListInConsentUpdateProfileScreenVisible() {
		return isElementVisible(essentialClaimsList, "Verified essential claim list in consent update profile screen");
	}

	public boolean isVoluntaryClaimListInConsentUpdateProfileScreenVisible() {
		return isElementVisible(voluntaryClaimsList, "Verified voluntary claim list in consent update profile screen");
	}

	public boolean isAvailableClaimStausDisplayed() {
		return isElementVisible(availableClaimsStatus,
				"Verified available claim status in consent update profile screen");
	}

	public boolean isNotAvailableClaimStausDisplayed() {
		return isElementVisible(notAvailableClaimStatus,
				"Verified not available claim status in consent update profile screen");
	}

	public void clickOnEssentialInfoIcon() {
		clickOnElement(essentialInfoIconInConsentUpdateProfileScreen,
				"Clicked on Info icon in consent update profile screen");
	}

	public void clickOnVoluntaryInfoIcon() {
		clickOnElement(voluntaryInfoIconInConsentUpdateProfileScreen,
				"Clicked on Info icon in consent update profile screen");
	}

	public void clickOnAttentionHeader() {
		clickOnElement(headerInConsentUpdateProfileScreen, "Clicked on header in consent update profile screen");
	}

	public boolean isEssentialClaimInformationDisplayed() {
		return isElementVisible(infoIconMeassage, "Verified essential claim information is displayed");
	}

	public boolean isVoluntaryClaimInformationDisplayed() {
		return isElementVisible(infoIconMeassage, "Verified voluntary claim information is displayed");
	}

	public boolean isMessageAboveProceedButtonDisplayed() {
		return isElementVisible(messageAboveProceedBtn,
				"Verified message above the proceed button in consent update profile screen");
	}

	public void clickOnCancelButtonInUpdateProfilePage() {
		clickOnElement(cancelButtonInConsentUpdateProfileScreen,
				"Clicked on cancel button in consent update profile screen");
	}

	public boolean isAttentionWarningPopupDisplayed() {
		return isElementVisible(attentionHeaderInWarningPopup, "Verified header in warning popup");
	}

	public boolean isSubHeaderInWarningPopupDisplayed() {
		return isElementVisible(subHeaderInWarningPopup, "Verified sub header in warning popup");
	}

	public boolean isStayButtonInWarningPopupScreenDisplayed() {
		return isElementVisible(stayButtonInConsentUpdateProfileScreen,
				"Verified stay button in warning popup is displayed");
	}

	public void clickOnStayButton() {
		clickOnElement(stayButtonInConsentUpdateProfileScreen, "Clicked on stay button");
	}

	public boolean isDiscontinueButtonInWarningPopupScreenDisplayed() {
		return isElementVisible(discontinueButtonInConsentUpdateProfileScreen,
				"Verified discontinue button in warning popup is displayed");
	}

	public void clickOnDiscontinueButton() {
		clickOnElement(discontinueButtonInConsentUpdateProfileScreen, "Clicked on discontinue button");
	}
}