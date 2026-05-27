package com.registraduria.scrutiny_service.portal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.registraduria.scrutiny_service.portal.dto.PortalResultsResponse;
import com.registraduria.scrutiny_service.portal.entity.PortalPublication;
import com.registraduria.scrutiny_service.portal.repository.PortalPublicationRepository;

@ExtendWith(MockitoExtension.class)
class PortalServiceTest {

    @Mock PortalPublicationRepository repository;

    @InjectMocks PortalService service;

    @Test
    void markPreliminary_uses_preliminar_badge_and_no_permalink() {
        when(repository.findByScrutinyCode("NAC")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        PortalPublication pub = service.markPreliminary("NAC");

        assertThat(pub.isOfficial()).isFalse();
        assertThat(pub.getBadgeText()).isEqualTo(PortalService.BADGE_PRELIMINARY);
        assertThat(pub.getActaPermalink()).isNull();
    }

    @Test
    void publishOfficial_removes_preliminar_and_shows_official_badge() {
        when(repository.findByScrutinyCode("NAC")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        PortalPublication pub = service.publishOfficial("NAC", "http://x/actas/1");

        assertThat(pub.isOfficial()).isTrue();
        assertThat(pub.getBadgeText()).isEqualTo(PortalService.BADGE_OFFICIAL);
        assertThat(pub.getBadgeText().toLowerCase()).doesNotContain("preliminar");
        assertThat(pub.getActaPermalink()).isEqualTo("http://x/actas/1");
    }

    @Test
    void response_for_official_publication_is_wcag_aa_green_and_clean() {
        PortalPublication pub = PortalPublication.builder()
                .scrutinyCode("NAC")
                .official(true)
                .badgeText(PortalService.BADGE_OFFICIAL)
                .actaPermalink("http://x/actas/1")
                .build();

        PortalResultsResponse res = service.toResponse(pub);

        assertThat(res.official()).isTrue();
        assertThat(res.badgeText()).isEqualTo("RESULTADOS OFICIALES");
        assertThat(res.badgeCssClass()).isEqualTo("results-official");
        assertThat(res.badgeColorHex()).isEqualTo("#1a7f37");
        assertThat(res.wcagLevel()).isEqualTo("AA");
        assertThat(res.preliminaryLabelsRemoved()).isTrue();
        assertThat(res.actaPermalink()).isEqualTo("http://x/actas/1");
    }
}
