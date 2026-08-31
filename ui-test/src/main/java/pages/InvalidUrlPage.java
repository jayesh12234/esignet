package pages;

import java.time.Duration;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import base.BasePage;

public class InvalidUrlPage extends BasePage {

	public InvalidUrlPage(WebDriver driver) {
		super(driver);
	}

	// Verified live (full-page DOM capture after hash-tampering): esignet-go shows the SAME generic
	// error page as the nonce-tampering case (see unauthorizedErrorHeading below) - a div.error-page-header
	// ("Something went wrong (401)") plus a div.error-page-detail ("An unexpected error occurred.
	// Please try again later."), not a standalone "unable to process" message with the old Tailwind
	// classes, which don't exist here. The detail line is the one whose text actually changes on
	// language switch (checked via isErrorMsgLanguageChanged), matching its "please try again" framing.
	@FindBy(xpath = "//div[@class='error-page-detail']")
	WebElement unableToProcessErrorMsg;

	@FindBy(id = "language_dropdown")
	WebElement languageDropdownInErrorPage;

	// esignet-go has no "signup-url-button" - the language switcher (verified nav element, no id, only
	// aria-haspopup="listbox") renders on every real esignet UI screen, so its presence is the generic
	// "we're still on a real esignet page, not an error page" signal.
	@FindBy(css = "nav button[aria-haspopup='listbox']")
	WebElement languageSwitcherNav;

	@FindBy(xpath = "//div[@class='error-page-header']")
	WebElement pageDoesNotExistErrorMsg;

	@FindBy(xpath = "//h1[@class='text-center text-2xl']")
	WebElement pageNotExistError;

	@FindBy(id = "reset-password-button")
	WebElement resetPasswordButton;

	@FindBy(id = "register-button")
	WebElement registerButton;

	@FindBy(xpath = "//div[@class='flex flex-col items-center gap-y-2']")
	WebElement somethingWentWrongErrorMsg;

	@FindBy(id = "proceed-button")
	WebElement proceedButtonAttentionScreen;

	public boolean isUnableToProcessErrorDisplayed() {
		return isElementVisible(unableToProcessErrorMsg, "Verified unable to process error message displayed");
	}

	public void clickOnLanguageDropdownOption() {
		clickOnElement(languageDropdownInErrorPage, "Clicked on language dropdown");
	}

	public boolean isErrorMsgLanguageChanged(String text) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
		wait.until(ExpectedConditions.textToBePresentInElement(unableToProcessErrorMsg, text));
		return unableToProcessErrorMsg.getText().contains(text);
	}

	public boolean isEsignetPageRetained() {
		return isElementVisible(languageSwitcherNav, "Verified esignet page is retained");
	}

	public boolean isPageDoesNotExistErrorMsgDisplayed() {
		return isElementVisible(pageDoesNotExistErrorMsg,
				"Verified page looking for does not exist error is displayed");
	}

	public boolean isPageNotExistErrorScreenDisplayed() {
		return isElementVisible(pageNotExistError, "Verified page not exist error is displayed");
	}

	public boolean isResetPasswordButtonVisible() {
		return isElementVisible(resetPasswordButton, "Verified reset password button is displayed");
	}

	public boolean isRegisterButtonVisible() {
		return isElementVisible(registerButton, "Verified register button is displayed");
	}

	public void clickOnResetPasswordButton() {
		clickOnElement(resetPasswordButton, "Clicked on reset password button");
	}

	public boolean isSomethingWentWrongErrorDisplayed() {
		return isElementVisible(somethingWentWrongErrorMsg, "Verified something went wrong error screen is displayed");
	}

	// Both the "unauthorized" and "page does not exist" error types render the same generic
	// div.error-page-header container - verified live - so this documents that intentionally rather
	// than pretending to distinguish them with a second, identically-located field.
	public boolean isUnauthorizedErrorDisplayed() {
		return isPageDoesNotExistErrorMsgDisplayed();
	}

	public boolean isAttentionScreenDisplayed() {
		return isElementVisible(proceedButtonAttentionScreen, "Verified attention screen is displayed");
	}

}