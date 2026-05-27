package com.registraduria.scrutiny_service.official.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.registraduria.scrutiny_service.candidate.dto.CandidateAggregateSummary;
import com.registraduria.scrutiny_service.candidate.repository.CandidateVoteRepository;
import com.registraduria.scrutiny_service.official.dto.CloseGeneralScrutinyRequest;
import com.registraduria.scrutiny_service.official.dto.GeneralScrutinyResponse;
import com.registraduria.scrutiny_service.official.dto.MagistrateApprovalResponse;
import com.registraduria.scrutiny_service.official.entity.ActaE26;
import com.registraduria.scrutiny_service.official.entity.GeneralScrutiny;
import com.registraduria.scrutiny_service.official.entity.MagistrateApproval;
import com.registraduria.scrutiny_service.official.enums.GeneralScrutinyStatus;
import com.registraduria.scrutiny_service.official.exception.InvalidScrutinyStateException;
import com.registraduria.scrutiny_service.official.exception.ResourceNotFoundException;
import com.registraduria.scrutiny_service.official.repository.ActaE26Repository;
import com.registraduria.scrutiny_service.official.repository.GeneralScrutinyRepository;
import com.registraduria.scrutiny_service.official.repository.MagistrateApprovalRepository;
import com.registraduria.scrutiny_service.official.security.MagistrateIdentity;
import com.registraduria.scrutiny_service.portal.service.PortalService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GeneralScrutinyServiceTest {

    @Mock GeneralScrutinyRepository scrutinyRepository;
    @Mock MagistrateApprovalRepository approvalRepository;
    @Mock ActaE26Repository actaRepository;
    @Mock CandidateVoteRepository candidateVoteRepository;
    @Mock MagistrateSignatureService signatureService;
    @Mock ActaGenerationService actaGenerationService;
    @Mock PortalService portalService;

    @InjectMocks GeneralScrutinyService service;

    private final MagistrateIdentity mag1 =
            new MagistrateIdentity("MAG-1", "Magistrada Uno", "MAGISTRADO_CNE");

    private GeneralScrutiny cerrado() {
        return GeneralScrutiny.builder()
                .id(1L)
                .scrutinyCode("NAC-2026")
                .status(GeneralScrutinyStatus.CERRADO)
                .electoralMethod("DHONDT")
                .seats(3)
                .requiredQuorum(2)
                .stateHash("statehash")
                .build();
    }

    @Test
    void close_creates_cerrado_and_marks_portal_preliminary() {
        when(scrutinyRepository.existsByScrutinyCode("NAC-2026")).thenReturn(false);
        when(candidateVoteRepository.aggregateAllCandidates()).thenReturn(List.of(
                new CandidateAggregateSummary("A1", "Ana", "A", 100L)));
        when(scrutinyRepository.save(any())).thenAnswer(i -> {
            GeneralScrutiny s = i.getArgument(0);
            s.setId(1L);
            return s;
        });

        GeneralScrutinyResponse res = service.close(
                new CloseGeneralScrutinyRequest("NAC-2026", 2, 3, null));

        assertThat(res.status()).isEqualTo(GeneralScrutinyStatus.CERRADO);
        assertThat(res.electoralMethod()).isEqualTo("DHONDT");
        assertThat(res.stateHash()).isNotBlank();
        verify(portalService).markPreliminary("NAC-2026");
    }

    @Test
    void close_rejects_duplicate_scrutiny_code() {
        when(scrutinyRepository.existsByScrutinyCode("NAC-2026")).thenReturn(true);

        assertThatThrownBy(() -> service.close(
                new CloseGeneralScrutinyRequest("NAC-2026", 2, 3, null)))
                .isInstanceOf(InvalidScrutinyStateException.class);
    }

    @Test
    void approve_below_quorum_sets_en_aprobacion_without_acta() {
        when(scrutinyRepository.findByScrutinyCode("NAC-2026"))
                .thenReturn(Optional.of(cerrado()));
        when(approvalRepository.existsByGeneralScrutinyIdAndMagistrateId(1L, "MAG-1"))
                .thenReturn(false);
        when(signatureService.sign(anyString(), anyString(), anyString())).thenReturn("sig");
        when(approvalRepository.save(any())).thenAnswer(i -> {
            MagistrateApproval a = i.getArgument(0);
            a.setId(10L);
            return a;
        });
        when(approvalRepository.countByGeneralScrutinyId(1L)).thenReturn(1L);
        when(scrutinyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MagistrateApprovalResponse res =
                service.approve("NAC-2026", mag1, "1.2.3.4");

        assertThat(res.quorumReached()).isFalse();
        assertThat(res.actaNumber()).isNull();
        assertThat(res.scrutinyStatus()).isEqualTo(GeneralScrutinyStatus.EN_APROBACION);
        verify(actaGenerationService, never()).generate(any());
    }

    @Test
    void approve_reaching_quorum_generates_acta_and_sets_oficial() {
        when(scrutinyRepository.findByScrutinyCode("NAC-2026"))
                .thenReturn(Optional.of(cerrado()));
        when(approvalRepository.existsByGeneralScrutinyIdAndMagistrateId(1L, "MAG-1"))
                .thenReturn(false);
        when(signatureService.sign(anyString(), anyString(), anyString())).thenReturn("sig");
        when(approvalRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(approvalRepository.countByGeneralScrutinyId(1L)).thenReturn(2L);
        when(actaRepository.existsByGeneralScrutinyId(1L)).thenReturn(false);
        when(actaGenerationService.generate(any())).thenReturn(
                ActaE26.builder().id(5L).actaNumber("E26-000001").build());
        when(scrutinyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MagistrateApprovalResponse res =
                service.approve("NAC-2026", mag1, "1.2.3.4");

        assertThat(res.quorumReached()).isTrue();
        assertThat(res.actaNumber()).isEqualTo("E26-000001");
        assertThat(res.scrutinyStatus()).isEqualTo(GeneralScrutinyStatus.OFICIAL);
        verify(actaGenerationService).generate(any());
    }

    @Test
    void approve_records_audit_with_ip_and_state_hash() {
        when(scrutinyRepository.findByScrutinyCode("NAC-2026"))
                .thenReturn(Optional.of(cerrado()));
        when(approvalRepository.existsByGeneralScrutinyIdAndMagistrateId(1L, "MAG-1"))
                .thenReturn(false);
        when(signatureService.sign(eq("MAG-1"), eq("statehash"), anyString()))
                .thenReturn("rsa-sig");
        when(approvalRepository.countByGeneralScrutinyId(1L)).thenReturn(1L);
        when(scrutinyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        org.mockito.ArgumentCaptor<MagistrateApproval> captor =
                org.mockito.ArgumentCaptor.forClass(MagistrateApproval.class);
        when(approvalRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        service.approve("NAC-2026", mag1, "200.1.2.3");

        MagistrateApproval saved = captor.getValue();
        assertThat(saved.getMagistrateId()).isEqualTo("MAG-1");
        assertThat(saved.getIpAddress()).isEqualTo("200.1.2.3");
        assertThat(saved.getApprovedStateHash()).isEqualTo("statehash");
        assertThat(saved.getApprovedAtUtc()).isNotNull();
        assertThat(saved.getSignature()).isEqualTo("rsa-sig");
    }

    @Test
    void approve_on_oficial_scrutiny_throws() {
        GeneralScrutiny oficial = cerrado();
        oficial.setStatus(GeneralScrutinyStatus.OFICIAL);
        when(scrutinyRepository.findByScrutinyCode("NAC-2026"))
                .thenReturn(Optional.of(oficial));

        assertThatThrownBy(() -> service.approve("NAC-2026", mag1, "1.2.3.4"))
                .isInstanceOf(InvalidScrutinyStateException.class);
        verify(approvalRepository, never()).save(any());
    }

    @Test
    void approve_duplicate_magistrate_throws() {
        when(scrutinyRepository.findByScrutinyCode("NAC-2026"))
                .thenReturn(Optional.of(cerrado()));
        when(approvalRepository.existsByGeneralScrutinyIdAndMagistrateId(1L, "MAG-1"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.approve("NAC-2026", mag1, "1.2.3.4"))
                .isInstanceOf(InvalidScrutinyStateException.class);
        verify(approvalRepository, never()).save(any());
    }

    @Test
    void approve_unknown_scrutiny_throws_not_found() {
        when(scrutinyRepository.findByScrutinyCode(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve("NOPE", mag1, "1.2.3.4"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
