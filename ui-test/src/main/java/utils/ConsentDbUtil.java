package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.testng.SkipException;

import io.mosip.testrig.apirig.utils.ConfigManager;

/**
 * Reads consent registry rows from {@code esignet.consent_detail} for UI test verification.
 * DB connectivity is resolved from Kernel properties ({@code db-server}, {@code postgres-password})
 * with optional overrides in config.properties.
 */
public final class ConsentDbUtil {

	private static final Logger logger = Logger.getLogger(ConsentDbUtil.class);

	public static final String PRIMARY_CLIENT_ID_KEY = "$ID:CreateOIDCClient_all_Valid_Smoke_sid_clientId$";
	public static final String SECONDARY_CLIENT_ID_KEY = "$ID:CreateOIDCClient_secondary_Smoke_sid_clientId$";

	private ConsentDbUtil() {
	}

	public record ConsentRecord(String psuToken, String claims, String acceptedClaims) {
	}

	public static void requireDbConfigured() {
		if (!isDbConfigured()) {
			throw new SkipException(
					"Consent DB verification skipped - configure db-server/postgres-password in Kernel.properties");
		}
	}

	public static boolean isDbConfigured() {
		return resolveDbUrl() != null && resolveDbUsername() != null && !resolveDbUsername().isBlank()
				&& resolveDbPassword() != null && !resolveDbPassword().isBlank();
	}

	public static Optional<ConsentRecord> findLatestByClientId(String clientId) {
		requireDbConfigured();
		String schema = getSchema();
		String sql = "SELECT psu_token, claims, accepted_claims FROM " + schema
				+ ".consent_detail WHERE client_id = ? ORDER BY cr_dtimes DESC LIMIT 1";

		try (Connection connection = openConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, clientId);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					return Optional.empty();
				}
				return Optional.of(new ConsentRecord(resultSet.getString("psu_token"),
						resultSet.getString("claims"), resultSet.getString("accepted_claims")));
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to query consent_detail for clientId=" + clientId, e);
		}
	}

	public static void assertConsentStoredWithPsuToken(String clientIdKey) {
		String clientId = EsignetUtil.resolveClientId(clientIdKey);
		ConsentRecord record = findLatestByClientId(clientId)
				.orElseThrow(() -> new AssertionError("No consent_detail row found for clientId=" + clientId));

		if (record.psuToken() == null || record.psuToken().isBlank()) {
			throw new AssertionError("consent_detail.psu_token is empty for clientId=" + clientId);
		}
		if (record.claims() == null || record.claims().isBlank()) {
			throw new AssertionError("consent_detail.claims is empty for clientId=" + clientId);
		}
		try {
			new JSONObject(record.claims());
		} catch (Exception e) {
			throw new AssertionError(
					"consent_detail.claims is not valid JSON for clientId=" + clientId + ": " + record.claims(), e);
		}
		logger.info("Verified consent_detail row for clientId=" + clientId + " with psu_token present and claims JSON");
	}

	public static void assertAcceptedClaimsEmpty(String clientIdKey) {
		String clientId = EsignetUtil.resolveClientId(clientIdKey);
		ConsentRecord record = findLatestByClientId(clientId)
				.orElseThrow(() -> new AssertionError("No consent_detail row found for clientId=" + clientId));

		String acceptedClaims = record.acceptedClaims();
		if (acceptedClaims == null || acceptedClaims.isBlank() || "{}".equals(acceptedClaims.trim())
				|| "[]".equals(acceptedClaims.trim())) {
			logger.info("Verified empty accepted_claims for clientId=" + clientId);
			return;
		}
		throw new AssertionError(
				"Expected empty accepted_claims for clientId=" + clientId + " but found: " + acceptedClaims);
	}

	private static String resolveDbUrl() {
		String override = EsignetConfigManager.getproperty("esignetDbUrl");
		if (override != null && !override.isBlank()) {
			return override.trim();
		}
		String server = ConfigManager.getDbServer();
		if (server == null || server.isBlank()) {
			return null;
		}
		String port = ConfigManager.getDbPort();
		if (port == null || port.isBlank()) {
			port = "5432";
		}
		return "jdbc:postgresql://" + server.trim() + ":" + port.trim() + "/mosip_esignet";
	}

	private static String resolveDbUsername() {
		String override = EsignetConfigManager.getproperty("esignetDbUsername");
		if (override != null && !override.isBlank()) {
			return override.trim();
		}
		String user = ConfigManager.getproperty("db-su-user");
		return (user != null && !user.isBlank()) ? user.trim() : "postgres";
	}

	private static String resolveDbPassword() {
		String override = EsignetConfigManager.getproperty("esignetDbPassword");
		if (override != null && !override.isBlank()) {
			return override.trim();
		}
		String password = ConfigManager.getproperty("postgres-password");
		return password != null ? password.trim() : "";
	}

	private static Connection openConnection() throws SQLException {
		return DriverManager.getConnection(resolveDbUrl(), resolveDbUsername(), resolveDbPassword());
	}

	private static String getSchema() {
		String schema = EsignetConfigManager.getproperty("esignetDbSchema");
		return (schema == null || schema.isBlank()) ? "esignet" : schema.trim();
	}
}
