package utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LanguageUtil {

    public static final Map<String, String> languagesMap = new HashMap<>();
    private static final Map<String, String> langCodeMappingMap = new HashMap<>();
    public static List<String> supportedLanguages = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(LanguageUtil.class);

    // esignet-go doesn't serve /locales/default.json (see the fallback below), so languagesMap stays
    // empty and can't supply real display names. The language-switcher dropdown's option text is a
    // static map baked into the frontend bundle, not fetched from any API - verified live against the
    // login page for these codes. Extend as more languages are exercised.
    private static final Map<String, String> FRONTEND_DISPLAY_NAMES = Map.of(
            "eng", "English",
            "khm", "Khmer",
            "hin", "हिन्दी",
            "fra", "français",
            "spa", "español");

    // Same static display names as FRONTEND_DISPLAY_NAMES, keyed by 2-letter ISO code instead of the
    // 3-letter one - needed by getDisplayNameFromIso(), which is always called with a 2-letter code.
    private static final Map<String, String> FRONTEND_DISPLAY_NAMES_BY_ISO2 = Map.of(
            "en", "English",
            "km", "Khmer",
            "hi", "हिन्दी",
            "fr", "français",
            "es", "español");

    // 3-letter -> 2-letter fallback for the same languages, for getIsoLanguageCode() on environments
    // where langCodeMappingMap stays empty (locales/default.json not served - see the static init
    // fallback above). config.properties' runLanguage (e.g. "eng") is always a 3-letter code.
    private static final Map<String, String> FRONTEND_ISO2_BY_CODE = Map.of(
            "eng", "en",
            "khm", "km",
            "hin", "hi",
            "fra", "fr",
            "spa", "es");

    static {
        try {
            // URL from config
            String localeUrl = EsignetConfigManager.getproperty("localeUrl");
            String url = (localeUrl.endsWith("/") ? localeUrl : localeUrl + "/") + "locales/default.json";

            // Download JSON content as String
            String jsonContent = downloadJson(url);

            // Parse JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonContent);

            // Populate languages_2Letters map
            rootNode.get("languages_2Letters").fields()
                    .forEachRemaining(entry -> languagesMap.put(entry.getKey(), entry.getValue().asText()));

            // Populate langCodeMapping map
            rootNode.get("langCodeMapping").fields()
                    .forEachRemaining(entry -> langCodeMappingMap.put(entry.getKey(), entry.getValue().asText()));

            // Populate keys list
            supportedLanguages = new ArrayList<>(langCodeMappingMap.keySet());

        } catch (Exception e) {
            logger.error("Error language locale JSON", e);

            // Some eSignet deployments (e.g. the Thunder/eSignet-go build) don't serve locales/default.json
            // as a static file - fall back to config's runLanguage so the run isn't blocked. Display-name and
            // ISO-code lookups (languagesMap/langCodeMappingMap) stay empty in this fallback and degrade to
            // returning the raw language code (see getDisplayName/getIsoLanguageCode).
            String runLanguage = EsignetConfigManager.getproperty("runLanguage");
            if (runLanguage != null && !runLanguage.isBlank()) {
                supportedLanguages = Arrays.stream(runLanguage.split(","))
                        .map(String::trim)
                        .filter(lang -> !lang.isEmpty())
                        .collect(java.util.stream.Collectors.toList());
                logger.warn("Falling back to config's runLanguage for supported languages: " + supportedLanguages);
            }
        }
    }

    /**
     * Returns the display name for a given language code.
     * If code is not found, returns the code itself.
     */
    public static String getDisplayName(String code) {
        String twoLetter = langCodeMappingMap.getOrDefault(code, code);
        String fromApi = languagesMap.get(twoLetter);
        if (fromApi != null) {
            return fromApi;
        }
        String key = code == null ? "" : code.trim().toLowerCase();
        return FRONTEND_DISPLAY_NAMES.getOrDefault(key, code);
    }

    /**
     * Returns the two-letter code for a given language code.
     * If code is not found, returns the code itself.
     */
    public static String getIsoLanguageCode(String code) {
        String mapped = langCodeMappingMap.get(code);
        if (mapped != null) {
            return mapped;
        }
        String key = code == null ? "" : code.trim().toLowerCase();
        return FRONTEND_ISO2_BY_CODE.get(key);
    }

    /**
     * Resolves a browser-reported locale (e.g. {@code en-US}) to a supported two-letter code.
     */
    public static String resolveFromBrowserLocale(String navigatorLanguage) {
        if (navigatorLanguage == null || navigatorLanguage.isBlank()) {
            return null;
        }
        String primary = navigatorLanguage.split("-")[0].toLowerCase();
        if (languagesMap.containsKey(primary)) {
            return primary;
        }
        String mapped = langCodeMappingMap.get(navigatorLanguage.toLowerCase());
        if (mapped != null) {
            return mapped;
        }
        mapped = langCodeMappingMap.get(primary);
        if (mapped != null) {
            return mapped;
        }
        return FRONTEND_ISO2_BY_CODE.getOrDefault(primary, primary);
    }

    public static boolean matchesLanguageCode(String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        String resolvedActual = resolveFromBrowserLocale(actual);
        String resolvedExpected = resolveFromBrowserLocale(expected);
        return resolvedActual != null && resolvedActual.equalsIgnoreCase(resolvedExpected);
    }

    /**
     * Neutral browser locale so navigator.language does not match any supported IDP language (TC_14).
     * "xx" is not a locale Chrome's --lang flag recognizes, so it's silently ignored - navigator.language
     * stays on the OS default (en-US), which always resolves to a supported language here, so the TC_14
     * assertion that depends on it can never pass on this deployment. Tried switching this to a real,
     * Chrome-recognized locale ("de") to actually exercise the fallback - confirmed live 2026-08-21 this
     * regresses ~7 unrelated scenarios, since applyBrowserLocale() applies it to every scenario's browser
     * session, not just TC_14. Reverted; TC_14 stays a known, environment-specific failure until the
     * locale override can be scoped to just that scenario (e.g. via a tag-driven browser option) instead
     * of applied globally.
     */
    public static String getNeutralBrowserLocale() {
        String locale = EsignetConfigManager.getproperty("defaultLangTestNeutralLocale");
        return (locale != null && !locale.isBlank()) ? locale.trim() : "xx";
    }

    /** True when the app persisted the synthetic neutral locale instead of DEFAULT_LANG (MOSIP-24002 TC_14). */
    public static boolean isNeutralStoredLanguage(String storedLanguage) {
        if (storedLanguage == null || storedLanguage.isBlank()) {
            return false;
        }
        String neutral = getNeutralBrowserLocale();
        return matchesLanguageCode(storedLanguage, neutral) || "xx".equalsIgnoreCase(storedLanguage.trim());
    }

    public static String fetchDefaultLangFromEnvConfig() {
        String baseUrl = EsignetConfigManager.getProperty("eSignetbaseurl", "").trim();
        if (baseUrl.isEmpty()) {
            throw new IllegalStateException("eSignetbaseurl is not configured; cannot read DEFAULT_LANG");
        }
        String url = (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + "env-config.js";
        try {
            String content = downloadJson(url);
            Matcher matcher = Pattern.compile("DEFAULT_LANG\\s*:\\s*['\"]([^'\"]+)['\"]").matcher(content);
            if (!matcher.find()) {
                throw new IllegalStateException("DEFAULT_LANG not found in env-config.js at " + url);
            }
            String defaultLang = matcher.group(1);
            logger.info("DEFAULT_LANG from env-config.js: " + defaultLang);
            return defaultLang;
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Failed to fetch env-config.js from " + url, e);
        }
    }

    /**
     * Resolves {@code DEFAULT_LANG} from env-config (2- or 3-letter) to a supported two-letter code.
     */
    public static String resolveDefaultLangToIsoCode(String defaultLang) {
        if (defaultLang == null || defaultLang.isBlank()) {
            return null;
        }
        String normalized = defaultLang.trim().toLowerCase();
        if (languagesMap.containsKey(normalized)) {
            return normalized;
        }
        if (langCodeMappingMap.containsKey(normalized)) {
            return langCodeMappingMap.get(normalized);
        }
        if (FRONTEND_ISO2_BY_CODE.containsKey(normalized)) {
            return FRONTEND_ISO2_BY_CODE.get(normalized);
        }
        return normalized.split("-")[0];
    }

    public static String getDisplayNameFromIso(String isoCode) {
        String fromApi = languagesMap.get(isoCode);
        if (fromApi != null) {
            return fromApi;
        }
        // languagesMap stays empty on environments (e.g. esignet-go) that don't serve
        // /locales/default.json - see the static init fallback above. Fall back to the same static
        // display names getDisplayName() uses, just keyed by the 2-letter code this method receives.
        String key = isoCode == null ? "" : isoCode.trim().toLowerCase();
        return FRONTEND_DISPLAY_NAMES_BY_ISO2.getOrDefault(key, isoCode);
    }

    public static boolean isSupportedBrowserLocale(String navigatorLanguage) {
        String resolved = resolveFromBrowserLocale(navigatorLanguage);
        return resolved != null
                && (languagesMap.containsKey(resolved) || FRONTEND_DISPLAY_NAMES_BY_ISO2.containsKey(resolved));
    }

    private static String downloadJson(String url) throws IOException {
        URI uri = URI.create(url);
        URLConnection connection = uri.toURL().openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(10_000);
        try (InputStream in = connection.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
