#@smokeAndRegression
Feature: Esignet Login Options Page
  This feature file is for verifying the Login options page

  @smoke @AuthenticaionPage
  Scenario Outline: Verifying Login page Options
   Given user captures the authorize url
   Then verify IDP UI uses default language configured in env-config
   Then verify dropdown language selection is present
   And verify multiple options for login is available
   And authentication screen should show login options based on acr_values from url
   And verify more ways to signIn option is available
   When user selects "<other lang>" from the language dropdown
   Then verify the UI is displayed in selected language "<text>"
   When click on Language selection option
   And select the mandatory language
   
  Examples:
   | other lang | text   |
   | हिन्दी        | लॉगिन    |
   
  @mobile @mobileViewFeatures
  Scenario: Verifying the UI in mobile view
   When user triggers the authorization endpoint, the response should have status code 200 and contain valid HTML with JS content
   Given user captures the authorize url
   And click on Language selection option
   And select the mandatory language
   Then user views the portal on multiple screen sizes
   And user verifies the behavior after resizing the browser window to different dimensions
   And user verify the otp button remain visible and aligned after resizing
   Then verify dropdown language selection is present
   And verify multiple options for login is available
   And verify more ways to signIn option is available
   And user click on Login with Otp
   Then user enters Registered mobile number into the mobile number field
   And user click on get otp button
   When user enters the correct otp
   And click on verify Otp button
   
   
   @smoke @supportOfPrefixAndPostfix @MOSIP-22717
  Scenario: Verifying support for multiple prefix and postfix type for the individual ID
   When click on Language selection option
   And select the mandatory language
   And user click on Login with Otp
   Then verify mobile number option is present for authentication
   Then verify nrc id option is present for authentication
   Then verify vid option is present for authentication
   Then verify email option is present for authentication
   Then verify mobile number selected for authentication
   And clicks on prefix number button in authentication screen page
   Then verify khm country prefix displayed for mobile number
   Then verify ind country prefix displayed for mobile number
   And clicks on prefix number button in authentication screen page
   Then verify get otp button is disabled in authentication screen
   Then user enters Registered mobile number into the mobile number field
   Then verify get otp button is enabled in authentication screen
   And user click on get otp button
   Then verify user navigate to verify otp screen
   Then verify the otp verification button is disabled on the verification screen
   When user enters the correct otp
   Then verify the otp verification button is enabled on the verification screen
   And click on verify Otp button
   Then verify user navigate to Attention screen
   And user completes consent flow through eKYC and returns to relying party
   And clicks on sign in with esignet button in login page
   When click on Language selection option
   And select the mandatory language
   And user click on Login with Otp
   Then clicks on vid option button in authentication screen page
   When user enters prerequisite vid1 into vid field
   And user click on get otp button
   When user enters the correct otp
   And click on verify Otp button
   And user completes consent flow through eKYC if attention screen is displayed
   And clicks on sign in with esignet button in login page
   When click on Language selection option
   And select the mandatory language
   And user click on Login with Otp
   Then clicks on vid option button in authentication screen page
   When user enters invalid vid into vid field
   And user click on get otp button
   Then verify user should get invalid individual id error message in authentication screen
   When user enters special characters into vid field
   And user click on get otp button
   Then verify user should get invalid individual id error message in authentication screen
   When user enters only space into vid field
   Then verify get otp button is disabled in authentication screen
   Then clicks on email option button in authentication screen page
   When user enters invalid email into email field
   And user click on get otp button
   Then verify user should get invalid individual id error message in authentication screen
   When user enters special characters into email field
   And user click on get otp button
   Then verify user should get invalid individual id error message in authentication screen
   When user enters only space into email field
   Then verify get otp button is disabled in authentication screen
   And clicks on sign in with esignet button in login page
   When click on Language selection option
   And select the mandatory language
   And user click on Login with Otp
   Then clicks on vid option button in authentication screen page
   When user enters prerequisite vid2 into vid field
   And user click on get otp button
   When user enters the correct otp
   And click on verify Otp button
   Then verify consent is not requested after authentication
   And clicks on sign in with esignet button in login page
   When click on Language selection option
   And select the mandatory language
   And user click on Login with Otp
   Then clicks on vid option button in authentication screen page
   When user enters prerequisite infant uin into vid field
   And user click on get otp button
   Then verify user navigate to verify otp screen
   When user enters the correct otp
   And click on verify Otp button
   Then verify otp authentication is denied for infant uin

  @smoke @BiometricDeviceNotDetected @MOSIP-22718
  Scenario: Log in with Biometrics - Device Not Detected (MOSIP-22718 TC_04)
   Given user captures the authorize url
   When click on Language selection option
   And select the mandatory language
   And user click on Login with Biometrics
   Then verify secure biometric interface is displayed
   And verify uin vid option is displayed on biometric screen
   When user clicks on uin vid option on biometric screen
   Then verify vid text field is displayed on biometric screen
   And verify scanning devices message is displayed on biometric screen
   And verify retry scan button is not displayed while scanning devices
   Then verify device not found message is displayed on biometric screen
   When user clicks on biometric device scan retry button
   Then verify scanning devices message is displayed on biometric screen

  @smoke @BiometricDeviceDetectedOnRetry @MOSIP-22718
  Scenario: Log in with Biometrics - Device discovered after Mock MDS retry (MOSIP-22718 TC_06)
   Given user captures the authorize url
   When click on Language selection option
   And select the mandatory language
   And user click on Login with Biometrics
   Then verify secure biometric interface is displayed
   And verify uin vid option is displayed on biometric screen
   When user clicks on uin vid option on biometric screen
   Then verify vid text field is displayed on biometric screen
   And verify scanning devices message is displayed on biometric screen
   And verify retry scan button is not displayed while scanning devices
   Then verify device not found message is displayed on biometric screen
   When mock mds is started for biometric device scan
   And user clicks on biometric device scan retry button
   Then verify scanning devices message is displayed on biometric screen
   And verify device not found message is cleared after mock mds retry scan
   And verify biometric device is discovered on biometric screen

  @smoke @NeedsUIN @BiometricLogin @MOSIP-22718
  Scenario: Log in with Biometrics - Mock MDS fingerprint login (MOSIP-22718 TC_05)
   Given user captures the authorize url
   When click on Language selection option
   And select the mandatory language
   When mock mds is started for biometric device scan
   And user click on Login with Biometrics
   Then verify secure biometric interface is displayed
   When user clicks on uin vid option on biometric screen
   And user enters prerequisite uin into biometric vid field
   Then verify biometric device is discovered on biometric screen
   When user clicks biometric scan and verify button
   Then verify user is authenticated via biometrics successfully

  @smoke @BiometricAuthenticationFlow @MOSIP-22718 @NeedsUIN @NeedsVID
  Scenario: IdP-UI biometrics authentication end-to-end (MOSIP-22718 TC_09-TC_29)
   Given user captures the authorize url
   When click on Language selection option
   And select the mandatory language
   When user opens login with biometrics via more ways to sign in if needed
   Then verify login with biometrics option is available in sign in options
   When user click on Login with Biometrics
   Then verify secure biometric interface is displayed
   When user clicks on uin vid option on biometric screen
   And verify scanning devices message is displayed on biometric screen
   Then verify device not found message is displayed on biometric screen
   When mock mds is started for biometric device scan
   And user clicks on biometric device scan retry button
   Then verify biometric device is discovered on biometric screen
   Then verify l0 or unregistered biometric device is not available
   Then verify biometric scan and verify button is displayed
   When user clears biometric vid field
   Then verify biometric scan and verify button is disabled
   When user enters "1" into biometric vid field
   Then verify biometric scan and verify button is enabled
   When user clears biometric vid field
   And user enters invalid uin into biometric vid field
   And user clicks biometric scan and verify button
   Then verify biometric error message contains "incorrect"
   When user dismisses biometric error banner if displayed
   When user enters invalid vid into biometric vid field
   And user clicks biometric scan and verify button
   Then verify biometric error message contains "incorrect"
   When user dismisses biometric error banner if displayed
   When user enters configured exception uin into biometric vid field
   And user clicks biometric scan and verify button
   Then verify biometric error message contains "biometric data"
   When user dismisses biometric error banner if displayed
   When user enters configured exception vid into biometric vid field
   And user clicks biometric scan and verify button
   Then verify biometric error message contains "biometric data"
   When user dismisses biometric error banner if displayed
   When user enters configured wrong match uin into biometric vid field
   And user clicks biometric scan and verify button
   Then verify biometric error message contains "did not match"
   When user dismisses biometric error banner if displayed
   When user enters configured wrong match vid into biometric vid field
   And user clicks biometric scan and verify button
   Then verify biometric error message contains "did not match"
   When user dismisses biometric error banner if displayed
   When user enters prerequisite vid into biometric vid field
   And user clicks biometric scan and verify button
   Then verify user is authenticated via biometrics successfully
   When user completes consent flow through eKYC if attention screen is displayed
   And clicks on sign in with esignet button in login page
   When click on Language selection option
   And select the mandatory language
   And user click on Login with Biometrics
   When user clicks on uin vid option on biometric screen
   And mock mds is started for biometric device scan
   And user clicks on biometric device scan retry button
   Then verify biometric device is discovered on biometric screen
   When user enters prerequisite uin into biometric vid field
   And user clicks biometric scan and verify button
   Then verify user is authenticated via biometrics successfully
   Then verify biometric capture timeout scenario is skipped for mock mds

  @smoke @LoginWithInji @MOSIP-24755
  Scenario: IdP-UI Login with Inji QR code (MOSIP-24755 TC_05-TC_19)
   Given user captures the authorize url
   When click on Language selection option
   And select the mandatory language
   When user opens login with inji via more ways to sign in if needed
   Then verify login with inji option is available in sign in options
   When user click on Login with Inji
   Then verify inji qr code login screen is displayed
   And verify link code expires in configured seconds
   When user refreshes browser on inji qr login screen
   And user opens login with inji via more ways to sign in if needed
   And user click on Login with Inji
   Then verify different inji link code is generated for same transaction after page refresh
   When user waits for inji qr code to expire without scanning
   Then verify inji qr code expired message is displayed
   And verify refresh qr code option is available after inji qr expiry
   When user refreshes inji qr code after expiry
   Then verify new inji qr code is generated after refresh
   And verify link code expires in configured seconds
   When user waits for first inji link code to expire
   And user attempts to link transaction with expired inji link code
   When user relaunches authorize flow for inji link code tests
   And click on Language selection option
   And select the mandatory language
   And user opens login with inji via more ways to sign in if needed
   And user click on Login with Inji
   And verify link code expires in configured seconds
   When user validates inji qr code by simulating wallet scan
   Then verify inji transaction is shifted to wallet app
   And verify inji qr authenticate progress is displayed
   When user relaunches authorize flow for inji link code tests
   And click on Language selection option
   And select the mandatory language
   And user opens login with inji via more ways to sign in if needed
   And user click on Login with Inji
   And verify link code expires in configured seconds
   When user generates second inji link code for same transaction
   And user attempts to link transaction with first inji link code after second is generated
   Then verify first inji link code is rejected after second link code is generated
   When user links transaction with latest inji link code
   Then verify linked inji link code status is LINKED and other link code times out
   Then verify new inji link code cannot be generated after transaction is linked
   And user attempts to link transaction with old inji link code after shift

  @LoginWithInji @MOSIP-25869
  Scenario: Max active inji link codes per transaction (MOSIP-25869 TC_09)
   Given user captures the authorize url
   When click on Language selection option
   And select the mandatory language
   When user opens login with inji via more ways to sign in if needed
   And user click on Login with Inji
   When user generates 4 inji link codes for same transaction
   Then verify inji link code index 1 is no longer active
   And verify inji link code index 2 is no longer active
   When user simulates inji wallet scan with link code index 3
   Then verify inji transaction is shifted to wallet app
   And verify inji qr authenticate progress is displayed

  # MOSIP-24755 TC_22-TC_26 require real Inji mobile app (credential list, face auth, inji consent, RP login).
  # Automate manually or via a dedicated mobile/device farm suite; not covered in this browser UI suite.

  @LoginWithInji @MOSIP-26238
  Scenario: Inji wallet logo on QR code (MOSIP-26238 TC_03 TC_04)
   Given user captures the authorize url
   When click on Language selection option
   And select the mandatory language
   When user opens login with inji via more ways to sign in if needed
   When user click on Login with Inji without capturing extra link code
   Then verify inji qr code login screen is displayed
   And verify inji wallet logo is displayed on qr code
   When user validates inji qr code by simulating wallet scan
   Then verify inji transaction is shifted to wallet app

  @LoginWithInji @ES-206
  Scenario: Inji link code limit and auto refresh (ES-206 TC_15 TC_16)
   Given user captures the authorize url
   When click on Language selection option
   And select the mandatory language
   When user opens login with inji via more ways to sign in if needed
   When user click on Login with Inji without capturing extra link code
   And user captures first inji link code from ui session
   When user exhausts inji server link code limit via api
   And user clicks inji qr refresh after link code limit exhausted
   Then verify user redirected to relying party with invalid_transaction
   When user relaunches authorize flow for inji link code tests
   And click on Language selection option
   And select the mandatory language
   When user opens login with inji via more ways to sign in if needed
   When user click on Login with Inji without capturing extra link code
   And user captures first inji link code from ui session
   Then verify inji qr auto refresh limit shows expired state
   When user attempts to generate inji link code beyond server limit
   Then verify user redirected to relying party with invalid_transaction

  @LoginWithInji @ES-21
  Scenario: Active inji link codes with auto refresh (ES-21 TC_02 TC_03)
   Given user captures the authorize url
   When click on Language selection option
   And select the mandatory language
   When user opens login with inji via more ways to sign in if needed
   When user click on Login with Inji without capturing extra link code
   And user captures first inji link code from ui session
   When user generates 3 inji link codes via api for same transaction
   Then verify inji link code index 1 is no longer active
   And verify inji link code index 2 is still active
   When user simulates inji wallet scan with link code index 2
   Then verify inji transaction is shifted to wallet app

  @mobile @LoginWithInji @ES-254
  Scenario: Mobile view Inji QR login (ES-254 TC_05)
   Given user captures the authorize url
   When click on Language selection option
   And select the mandatory language
   When user opens login with inji via more ways to sign in if needed
   When user click on Login with Inji without capturing extra link code
   Then verify inji qr code login screen is displayed
   When user simulates inji wallet scan on mobile viewport
   Then verify inji transaction is shifted to wallet app
   And verify inji qr authenticate progress is displayed
