package pages;

import java.util.Objects;
import java.util.logging.Logger;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import base.BasePage;
import utils.BaseTestUtil;
import utils.LanguageUtil;

public class VideoPreviewPage extends BasePage {

	private static final Logger LOGGER = Logger.getLogger(VideoPreviewPage.class.getName());

	public VideoPreviewPage(WebDriver driver) {
		super(driver);
	}

	@FindBy(id = "video-preview-header")
	WebElement keyInformation;

	@FindBy(id = "cancel-preview-button")
	WebElement cancelButton;

	@FindBy(id = "proceed-preview-button")
	WebElement proceedButton;

	@FindBy(xpath = "//div[contains(@class,'scrollable-div')]")
	private WebElement scrollOption;

	@FindBy(id = "stay-button")
	WebElement stayButton;

	@FindBy(id = "dismiss-button")
	WebElement discontinueButton;

	@FindBy(xpath = "//div[contains(@class,'video-message')]")
	WebElement loadingScreenMessage;

	@FindBy(id = "sign-in-with-esignet")
	WebElement signInWithEsignetButton;

	@FindBy(xpath = "//span[contains(@class,'video-preview-content')]")
	WebElement listOfInstructions;

	@FindBy(xpath = "//div[@role='alertdialog']//h2[contains(@class,'font-semibold')]")
	WebElement attentionWarningPopup;

	@FindBy(className = "video-preview-disabled-header")
	WebElement cameraAccessDisabledHeader;

	@FindBy(className = "video-preview-disabled-subheader")
	WebElement cameraAccessDisabledSubHeader;

	public boolean isVideoPreviewScreenDisplayed() {
		return isElementVisible(keyInformation, "Verified video preview screen is visible");
	}

	public boolean isKeyInformationHeaderDisplayed() {
		return isElementVisible(keyInformation, "Verified keyInformation header is visible");
	}

	public boolean isCancelButtonDisplayed() {
		return isElementVisible(cancelButton, "Verified cancel button is visible");
	}

	public boolean isProceedButtonDisplayed() {
		return isElementVisible(proceedButton, "Verified proceed button is visible");
	}

	public boolean isProceedButtonEnabled() {
		return isButtonEnabled(proceedButton, "Verified proceed button is enabled");
	}

	public boolean isScrollOptionPresent() {
		return isElementVisible(scrollOption, "Verified scroll option is visible");
	}

	public void clickOnCancelButton() {
		clickOnElement(cancelButton, "Clicked on cancel button");
	}

	public boolean isAttentionWarningPopupDisplayed() {
		return isElementVisible(attentionWarningPopup, "Verified attention warning popup is visible");
	}

	public void clickOnStayButtonInAttentionWarningPopup() {
		clickOnElement(stayButton, "Clicked on stay button in attention warning popup");
	}

	public void clickOnDiscontinueButtonInAttentionWarningPopup() {
		clickOnElement(discontinueButton, "Clicked on discontinue button in attention warning popup");
	}

	public boolean isLoadingScreenMessageDisplayed() {
		return isElementVisible(loadingScreenMessage, "Verified loading screen message is visible");
	}

	public void clickOnSignInWithEsignetButton() {
		clickOnElement(signInWithEsignetButton, "Clicked on sign in with esignet button");
	}

	public boolean isListOfInstructionsDisplayed() {
		return isElementVisible(listOfInstructions, "Verified list of instructions is visible");
	}

	public boolean isCameraAccessDisabledHeaderDisplayed() {
		return isElementVisible(cameraAccessDisabledHeader, "Verified camera access disabled header is visible");
	}

	public boolean isCameraAccessDisabledSubHeaderDisplayed() {
		return isElementVisible(cameraAccessDisabledSubHeader, "Verified camera access disabled subtitle is visible");
	}

	public boolean isProceedButtonDisabled() {
		return !isButtonEnabled(proceedButton, "Verified proceed button state");
	}

	/**
	 * Flips camera permission to granted mid-scenario via CDP, simulating a user
	 * re-enabling access from browser settings after an earlier denial.
	 */
	public void grantCameraAccessAtRuntime() {
		BaseTestUtil.setCameraPermissionAtRuntime(driver, "granted");
	}

	/**
	 * Confirms the screen is rendering in the language selected at login: the
	 * persisted i18next locale matches the expected ISO code, and the header /
	 * instructions that anchor this screen are actually visible (proxy for a
	 * real render rather than a blank/broken page under the switched locale).
	 */
	public boolean isDisplayedInLanguage(String langCode) {
		String expectedIsoCode = LanguageUtil.getIsoLanguageCode(langCode);
		String actualIsoCode = getLanguageFromLocalStorage();
		if (!Objects.equals(expectedIsoCode, actualIsoCode)) {
			LOGGER.warning(
					"Expected language code '" + expectedIsoCode + "' but localStorage had '" + actualIsoCode + "'");
			return false;
		}
		return isKeyInformationHeaderDisplayed() && isListOfInstructionsDisplayed();
	}

	private String getLanguageFromLocalStorage() {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		return (String) js.executeScript("return window.localStorage.getItem('i18nextLng');");
	}
}
