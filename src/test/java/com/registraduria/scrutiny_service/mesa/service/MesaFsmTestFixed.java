package com.registraduria.scrutiny_service.mesa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.registraduria.scrutiny_service.command.repository.E14CommandRepository;
import com.registraduria.scrutiny_service.domain.entity.E14Record;
import com.registraduria.scrutiny_service.domain.enums.E14Status;
import com.registraduria.scrutiny_service.mesa.dto.CloseMesaRequest;
import com.registraduria.scrutiny_service.mesa.entity.MesaRecord;
import com.registraduria.scrutiny_service.mesa.enums.MesaStatus;
import com.registraduria.scrutiny_service.mesa.repository.MesaRepository;
import com.registraduria.scrutiny_service.pdf.E14PdfService;

@ExtendWith(MockitoExtension.class)
class MesaFsmTestFixed {

    @Mock
    MesaRepository mesaRepository;

    @Mock
    E14CommandRepository e14Repository;

    @Mock
    E14PdfService pdfService;

    @InjectMocks
    MesaCommandService service;

    MesaRecord mesaOpen;
    MesaRecord mesaClosed;
    MesaRecord mesaVerified;

    @BeforeEach
    void setUp() {
        mesaOpen = MesaRecord.builder()
                .id(1L)
                .mesaCode("M-001")
                .validVotes(0)
                .blankVotes(0)
                .nullVotes(0)
                .unmarkedVotes(0)
                .status(MesaStatus.OPEN)
                .quarantined(false)
                .build();

        mesaClosed = MesaRecord.builder()
                .id(2L)
                .mesaCode("M-002")
                .validVotes(10)
                .blankVotes(0)
                .nullVotes(0)
                .unmarkedVotes(0)
                .status(MesaStatus.CLOSED)
                .quarantined(false)
                .build();

        mesaVerified = MesaRecord.builder()
                .id(3L)
                .mesaCode("M-003")
                .validVotes(12)
                .blankVotes(0)
                .nullVotes(0)
                .unmarkedVotes(0)
                .status(MesaStatus.VERIFIED)
                .quarantined(false)
                .build();

    }

    @Test
    void closeMesa_fromOpen_createsE14AndCloses() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaOpen));
        when(mesaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(e14Repository.save(any())).thenAnswer(i -> i.getArgument(0));

        try {
            java.nio.file.Path tmp = java.nio.file.Files.createTempFile("e14-test", ".pdf");
            java.nio.file.Files.writeString(tmp, "dummy");
            when(pdfService.generatePdf(any())).thenReturn(tmp.toString());

            CloseMesaRequest req = new CloseMesaRequest(1, 2, 3, 4);
            MesaRecord result = service.closeMesa(1L, req);

            assertThat(result.getStatus()).isEqualTo(MesaStatus.CLOSED);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void closeMesa_fromClosed_throwsIllegalState() {
        when(mesaRepository.findById(2L)).thenReturn(Optional.of(mesaClosed));

        CloseMesaRequest req = new CloseMesaRequest(1, 2, 3, 4);

        assertThatThrownBy(() -> service.closeMesa(2L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid state transition");
    }

    @Test
    void verifyMesa_fromClosed_setsVerified() {
        when(mesaRepository.findById(2L)).thenReturn(Optional.of(mesaClosed));
        when(mesaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MesaRecord result = service.verifyMesa(2L);
        assertThat(result.getStatus()).isEqualTo(MesaStatus.VERIFIED);
    }

    @Test
    void sealMesa_updatesE14StatusToSealed() {
        when(mesaRepository.findById(3L)).thenReturn(Optional.of(mesaVerified));
        when(mesaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        E14Record e14 = E14Record.builder()
                .id(10L)
                .mesaCode(mesaVerified.getMesaCode())
                .pdfHash("abc")
                .status(E14Status.DRAFT)
                .build();

        when(e14Repository.findByMesaCode(mesaVerified.getMesaCode())).thenReturn(Optional.of(e14));
        when(e14Repository.save(any())).thenAnswer(i -> i.getArgument(0));

        MesaRecord result = service.sealMesa(3L);
        assertThat(result.getStatus()).isEqualTo(MesaStatus.SEALED);
        assertThat(e14.getStatus()).isEqualTo(E14Status.SEALED);
    }

    @Test
    void quarantine_fromOpen_setsQuarantined() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaOpen));
        when(mesaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MesaRecord result = service.quarantineMesa(1L, "irregularidad");
        assertThat(result.getStatus()).isEqualTo(MesaStatus.QUARANTINED);
        assertThat(result.getQuarantined()).isTrue();
    }
}
