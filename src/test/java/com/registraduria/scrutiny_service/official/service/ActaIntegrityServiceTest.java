package com.registraduria.scrutiny_service.official.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.registraduria.scrutiny_service.official.entity.ActaE26;
import com.registraduria.scrutiny_service.official.exception.ActaTamperedException;

class ActaIntegrityServiceTest {

    private final ActaIntegrityService service = new ActaIntegrityService();

    @Test
    void verify_true_when_hash_matches_content() {
        String content = "{\"acta\":\"E26-000001\"}";
        ActaE26 acta = ActaE26.builder()
                .actaNumber("E26-000001")
                .contentJson(content)
                .contentHash(Sha256Util.hexDigest(content))
                .build();

        assertThat(service.verify(acta)).isTrue();
    }

    @Test
    void verify_false_when_content_was_tampered() {
        String content = "{\"acta\":\"E26-000001\"}";
        ActaE26 acta = ActaE26.builder()
                .actaNumber("E26-000001")
                .contentJson(content + " TAMPERED")
                .contentHash(Sha256Util.hexDigest(content))
                .build();

        assertThat(service.verify(acta)).isFalse();
    }

    @Test
    void verifyOrThrow_raises_when_tampered() {
        ActaE26 acta = ActaE26.builder()
                .actaNumber("E26-000002")
                .contentJson("real content")
                .contentHash(Sha256Util.hexDigest("different content"))
                .build();

        assertThatThrownBy(() -> service.verifyOrThrow(acta))
                .isInstanceOf(ActaTamperedException.class);
    }
}
