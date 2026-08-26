package com.emplanorte.security;

import com.emplanorte.model.Usuario;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Genera y valida tokens JWT HS256 sin modificar usuarios ni contraseñas.
 * La identidad sigue saliendo de la tabla usuarios y de la validación BCrypt
 * existente; este servicio solo protege las solicitudes posteriores al login.
 */
@Service
public class JwtTokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenService.class);
    private static final String ALGORITHM = "HmacSHA256";
    private static final String JWT_ALGORITHM = "HS256";
    private static final int MINIMUM_SECRET_BYTES = 32;
    private static final int MAXIMUM_TOKEN_LENGTH = 8192;
    private static final long CLOCK_SKEW_SECONDS = 60;

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final Duration tokenDuration;
    private final Clock clock;
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
    private final Base64.Decoder decoder = Base64.getUrlDecoder();

    @Autowired
    public JwtTokenService(
            @Value("${app.auth.token-secret:}") String configuredSecret,
            @Value("${app.auth.token-duration-hours:24}") long durationHours,
            ObjectMapper objectMapper
    ) {
        this(configuredSecret, durationHours, objectMapper, Clock.systemUTC());
    }

    JwtTokenService(String configuredSecret, long durationHours, ObjectMapper objectMapper, Clock clock) {
        if (durationHours <= 0) {
            throw new IllegalArgumentException("AUTH_TOKEN_DURATION_HOURS debe ser mayor que cero");
        }
        this.objectMapper = objectMapper;
        this.secret = resolveSecret(configuredSecret);
        this.tokenDuration = Duration.ofHours(durationHours);
        this.clock = clock;
    }

    public IssuedToken generateToken(Usuario usuario) {
        if (usuario == null || usuario.getId() == null || usuario.getCorreo() == null
                || usuario.getNombre() == null || usuario.getRol() == null) {
            throw new IllegalArgumentException("No se puede generar un token para un usuario incompleto");
        }

        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(tokenDuration);

        Map<String, Object> header = Map.of("alg", JWT_ALGORITHM, "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", usuario.getId().toString());
        payload.put("correo", usuario.getCorreo());
        payload.put("nombre", usuario.getNombre());
        payload.put("rol", usuario.getRol());
        payload.put("iat", issuedAt.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());
        payload.put("jti", UUID.randomUUID().toString());

        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        String unsignedToken = encodedHeader + "." + encodedPayload;
        String token = unsignedToken + "." + sign(unsignedToken);

        return new IssuedToken(token, expiresAt, tokenDuration.toSeconds());
    }

    public TokenClaims validateToken(String token) {
        if (token == null || token.isBlank() || token.length() > MAXIMUM_TOKEN_LENGTH) {
            throw new InvalidTokenException();
        }

        String[] parts = token.split("\\.", -1);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            throw new InvalidTokenException();
        }

        String unsignedToken = parts[0] + "." + parts[1];
        byte[] receivedSignature;
        byte[] expectedSignature;
        try {
            receivedSignature = decoder.decode(parts[2]);
            expectedSignature = decoder.decode(sign(unsignedToken));
        } catch (IllegalArgumentException ex) {
            throw new InvalidTokenException();
        }
        if (!MessageDigest.isEqual(receivedSignature, expectedSignature)) {
            throw new InvalidTokenException();
        }

        try {
            JsonNode header = objectMapper.readTree(decoder.decode(parts[0]));
            if (!JWT_ALGORITHM.equals(header.path("alg").asText())
                    || !"JWT".equals(header.path("typ").asText())) {
                throw new InvalidTokenException();
            }

            JsonNode payload = objectMapper.readTree(decoder.decode(parts[1]));
            Long userId = parsePositiveLong(payload.path("sub").asText());
            String correo = requiredText(payload, "correo");
            String nombre = requiredText(payload, "nombre");
            String rol = requiredText(payload, "rol");
            long issuedAtSeconds = requiredLong(payload, "iat");
            long expiresAtSeconds = requiredLong(payload, "exp");

            Instant now = clock.instant();
            Instant issuedAt = Instant.ofEpochSecond(issuedAtSeconds);
            Instant expiresAt = Instant.ofEpochSecond(expiresAtSeconds);
            if (!expiresAt.isAfter(now)) {
                throw new ExpiredTokenException();
            }
            if (issuedAt.isAfter(now.plusSeconds(CLOCK_SKEW_SECONDS)) || !expiresAt.isAfter(issuedAt)) {
                throw new InvalidTokenException();
            }

            return new TokenClaims(userId, correo, nombre, rol, issuedAt, expiresAt);
        } catch (ExpiredTokenException | InvalidTokenException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidTokenException();
        }
    }

    public long getTokenDurationSeconds() {
        return tokenDuration.toSeconds();
    }

    private String encodeJson(Object value) {
        try {
            return encoder.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible generar el token", ex);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            return encoder.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible firmar el token", ex);
        }
    }

    private static byte[] resolveSecret(String configuredSecret) {
        if (configuredSecret == null || configuredSecret.isBlank()) {
            byte[] generated = new byte[MINIMUM_SECRET_BYTES];
            new SecureRandom().nextBytes(generated);
            LOGGER.warn("AUTH_TOKEN_SECRET no está configurado. Se generó una clave temporal; "
                    + "configure la variable antes de desplegar para conservar sesiones tras reinicios.");
            return generated;
        }
        byte[] configuredBytes = configuredSecret.getBytes(StandardCharsets.UTF_8);
        if (configuredBytes.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "AUTH_TOKEN_SECRET debe tener al menos " + MINIMUM_SECRET_BYTES + " bytes");
        }
        return configuredBytes;
    }

    private static String requiredText(JsonNode payload, String field) {
        String value = payload.path(field).asText("").trim();
        if (value.isEmpty()) throw new InvalidTokenException();
        return value;
    }

    private static long requiredLong(JsonNode payload, String field) {
        JsonNode value = payload.path(field);
        if (!value.canConvertToLong()) throw new InvalidTokenException();
        return value.asLong();
    }

    private static Long parsePositiveLong(String value) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) throw new InvalidTokenException();
            return parsed;
        } catch (NumberFormatException ex) {
            throw new InvalidTokenException();
        }
    }

    public record IssuedToken(String value, Instant expiresAt, long expiresInSeconds) {}

    public record TokenClaims(
            Long userId,
            String correo,
            String nombre,
            String rol,
            Instant issuedAt,
            Instant expiresAt
    ) {
        public String authority() {
            String normalized = java.text.Normalizer.normalize(rol, java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .toUpperCase(Locale.ROOT)
                    .replaceAll("[^A-Z0-9]", "_");
            return "ROLE_" + normalized;
        }
    }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException() {
            super("Token inválido. Inicia sesión nuevamente.");
        }
    }

    public static class ExpiredTokenException extends RuntimeException {
        public ExpiredTokenException() {
            super("Tu sesión ha expirado. Inicia sesión nuevamente.");
        }
    }
}
