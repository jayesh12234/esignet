package utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ResourceBundleLoader {

	private static final Map<String, String> resourceBundleMap = new HashMap<>();
	private static final Logger logger = Logger.getLogger(ResourceBundleLoader.class);
	private static volatile boolean loaded = false;
	private static String loadedLanguage = "";

	public static String get(String key) {
		String currentLang = System.getProperty("currentRunLanguage", "eng");
		if (!loaded || !currentLang.equalsIgnoreCase(loadedLanguage)) {
			synchronized (ResourceBundleLoader.class) {
				if (!loaded || !currentLang.equalsIgnoreCase(loadedLanguage)) {
					// Only cache the load as done when it actually succeeded - otherwise a failed load
					// (e.g. a transient network error) gets cached as if it succeeded, and later lookups
					// would never retry.
					if (loadResourceBundleJson(currentLang)) {
						loaded = true;
						loadedLanguage = currentLang;
					}
				}
			}
		}
		return resourceBundleMap.getOrDefault(key, "!!MISSING_KEY: " + key + "!!");
	}

	public static String getByIsoCode(String isoCode, String key) {
		Map<String, String> bundle = loadResourceBundleForIsoCode(isoCode);
		return bundle.getOrDefault(key, "!!MISSING_KEY: " + key + "!!");
	}

	private static boolean loadResourceBundleJson(String currentLang) {
		try {
			resourceBundleMap.clear();
			String twoLetterCode = LanguageUtil.getIsoLanguageCode(currentLang);
			if (twoLetterCode == null) {
				logger.warn("No ISO mapping found for language: " + currentLang + ", falling back to: " + currentLang);
				twoLetterCode = currentLang;
			}
			Map<String, String> bundle = loadResourceBundleForIsoCode(twoLetterCode);
			if (bundle.isEmpty()) {
				logger.warn("Resource bundle for language: " + currentLang + " loaded empty - not caching as loaded");
				return false;
			}
			resourceBundleMap.putAll(bundle);
			logger.info("Loaded resource bundle for language: " + currentLang);
			return true;
		} catch (Exception e) {
			logger.error("Error loading resource bundle JSON for lang: " + currentLang, e);
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String> loadResourceBundleForIsoCode(String isoCode) {
		Map<String, String> bundle = new HashMap<>();
		try {
			// esignet-go doesn't serve /locales/<iso>.json as a static file (SPA catch-all, not real
			// translations) - the real catalog (1261 keys, verified) is embedded under
			// response.i18n.translations of this flow-metadata endpoint instead, keyed by the same
			// 2-letter codes ("en", "km", "hi", ...). No auth/challenge-token needed, unlike
			// /v1/esignet/flow/execute. Note the new catalog uses a different key taxonomy than the
			// classic eSignet UI's (e.g. no "otp.link_using_id"), so lookups for old keys may still
			// come back missing even though the fetch itself now succeeds.
			String clientId = EsignetConfigManager.getproperty("oidcClientId");
			String url = EsignetConfigManager.getproperty("eSignetbaseurl")
					+ "/v1/esignet/flow/meta?id=" + clientId + "&type=APP&language=" + isoCode;
			String jsonContent = downloadJson(url);
			Map<String, Object> response = new ObjectMapper().readValue(jsonContent, new TypeReference<>() {
			});
			Object i18n = response.get("i18n");
			Object translations = i18n instanceof Map ? ((Map<String, Object>) i18n).get("translations") : null;
			if (translations instanceof Map) {
				flatten((Map<String, Object>) translations, "", bundle);
			} else {
				logger.warn("flow/meta response for language '" + isoCode + "' had no i18n.translations object");
			}
			logger.info("Loaded resource bundle for ISO code: " + isoCode);
		} catch (Exception e) {
			logger.error("Error loading resource bundle JSON for ISO code: " + isoCode, e);
		}
		return bundle;
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

	@SuppressWarnings("unchecked")
	private static void flatten(Map<String, Object> source, String prefix, Map<String, String> target) {
		for (Map.Entry<String, Object> entry : source.entrySet()) {
			String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
			Object value = entry.getValue();
			if (value instanceof Map) {
				flatten((Map<String, Object>) value, key, target);
			} else {
				target.put(key, value.toString());
			}
		}
	}

	public static String getPrefixText(String key) {
		String value = get(key);
		if (value != null && value.startsWith("!!MISSING_KEY:")) {
			throw new IllegalStateException("Resource bundle missing key: " + key);
		}
		return value.split("\\{\\{currentID\\}\\}")[0].trim();
	}
}
