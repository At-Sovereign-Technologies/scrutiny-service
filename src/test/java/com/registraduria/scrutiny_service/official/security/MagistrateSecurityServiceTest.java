package com.registraduria.scrutiny_service.official.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.registraduria.scrutiny_service.official.TestJwt;
import com.registraduria.scrutiny_service.official.exception.ForbiddenException;
import com.registraduria.scrutiny_service.official.exception.UnauthorizedException;

class MagistrateSecurityServiceTest {

    private static final String SECRET = "unit-test-secret-0123456789-abcdefghijklmnop";

    private final MagistrateSecurityService service =
            new MagistrateSecurityService(SECRET);

    @Test
    void authorizes_valid_magistrate_token() {
        String token = TestJwt.magistrate(SECRET, "MAG-1", "Magistrada Uno");

        MagistrateIdentity identity =
                service.authorize("Bearer " + token, "10.0.0.1");

        assertThat(identity.magistrateId()).isEqualTo("MAG-1");
        assertThat(identity.magistrateName()).isEqualTo("Magistrada Uno");
        assertThat(identity.role()).isEqualTo(MagistrateSecurityService.REQUIRED_ROLE);
    }

    @Test
    void rejects_missing_token_with_401() {
        assertThatThrownBy(() -> service.authorize(null, "10.0.0.1"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejects_non_bearer_header_with_401() {
        assertThatThrownBy(() -> service.authorize("Basic abc", "10.0.0.1"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejects_malformed_token_with_401() {
        assertThatThrownBy(() -> service.authorize("Bearer not.a.valid.jwt.token", "10.0.0.1"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejects_token_signed_with_wrong_secret_with_401() {
        String token = TestJwt.magistrate("a-different-secret-aaaaaaaaaaaaaaaaaaaaaa", "MAG-1", "X");

        assertThatThrownBy(() -> service.authorize("Bearer " + token, "10.0.0.1"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejects_expired_token_with_401() {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", "MAG-1");
        claims.put("role", "MAGISTRADO_CNE");
        claims.put("exp", 1L); // 1970
        String token = TestJwt.sign(SECRET, claims);

        assertThatThrownBy(() -> service.authorize("Bearer " + token, "10.0.0.1"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejects_wrong_role_with_403() {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", "USER-1");
        claims.put("role", "OPERADOR");
        String token = TestJwt.sign(SECRET, claims);

        assertThatThrownBy(() -> service.authorize("Bearer " + token, "10.0.0.1"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void rejects_token_without_sub_with_401() {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("role", "MAGISTRADO_CNE");
        String token = TestJwt.sign(SECRET, claims);

        assertThatThrownBy(() -> service.authorize("Bearer " + token, "10.0.0.1"))
                .isInstanceOf(UnauthorizedException.class);
    }
}
