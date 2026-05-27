package com.registraduria.scrutiny_service.mesa.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.registraduria.scrutiny_service.command.repository.E14CommandRepository;
import com.registraduria.scrutiny_service.domain.entity.E14Record;
import com.registraduria.scrutiny_service.domain.enums.E14Status;
import com.registraduria.scrutiny_service.mesa.dto.CloseMesaRequest;
import com.registraduria.scrutiny_service.mesa.dto.CreateMesaRequest;
import com.registraduria.scrutiny_service.mesa.entity.MesaRecord;
import com.registraduria.scrutiny_service.mesa.enums.MesaStatus;
import com.registraduria.scrutiny_service.mesa.repository.MesaRepository;
import com.registraduria.scrutiny_service.pdf.E14PdfService;

@ExtendWith(MockitoExtension.class)
@DisplayName("MesaCommandService — Pruebas FSM (Finite State Machine)")
class MesaFsmTest {

    @Mock
    private MesaRepository mesaRepository;

    @Mock
    private E14CommandRepository e14Repository;

    @Mock
    private E14PdfService pdfService;

    @InjectMocks
    private MesaCommandService mesaCommandService;

    private MesaRecord mesaOpen;
    private MesaRecord mesaClosed;
    private MesaRecord mesaVerified;
    private MesaRecord mesaSealed;

    @BeforeEach
    void setUp() {
        mesaOpen = MesaRecord.builder()
                .id(1L)
                .mesaCode("BOG-001")
                .validVotes(0)
                .blankVotes(0)
                .nullVotes(0)
                .unmarkedVotes(0)
                .status(MesaStatus.OPEN)
                .quarantined(false)
                .build();

        mesaClosed = MesaRecord.builder()
                .id(2L)
                .mesaCode("BOG-002")
                .validVotes(100)
                .blankVotes(5)
                .nullVotes(3)
                .unmarkedVotes(2)
                .status(MesaStatus.CLOSED)
                .quarantined(false)
                .build();

        mesaVerified = MesaRecord.builder()
                .id(3L)
                .mesaCode("BOG-003")
                .validVotes(100)
                .blankVotes(5)
                .nullVotes(3)
                .unmarkedVotes(2)
                .status(MesaStatus.VERIFIED)
                .quarantined(false)
                .build();

        mesaSealed = MesaRecord.builder()
                .id(4L)
                .mesaCode("BOG-004")
                .validVotes(100)
                .blankVotes(5)
                .nullVotes(3)
                .unmarkedVotes(2)
                .status(MesaStatus.SEALED)
                .quarantined(false)
                .build();
    }

    private String createTempPdfPath() {
        try {
            return Files.createTempFile("e14", ".pdf").toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ============================================
    // CREATE MESA: INICIAL → OPEN (TC-SC-001-05)
    // ============================================

    @Test
    @DisplayName("TC-SC-001 | createMesa → Crea mesa en estado OPEN con votos iniciales 0")
    void tc_sc_001_createMesa_initial_state() {
        CreateMesaRequest request = new CreateMesaRequest("MED-MESA-001");
        
        MesaRecord expected = MesaRecord.builder()
                .id(100L)
                .mesaCode("MED-MESA-001")
                .validVotes(0)
                .blankVotes(0)
                .nullVotes(0)
                .unmarkedVotes(0)
                .status(MesaStatus.OPEN)
                .quarantined(false)
                .build();

        when(mesaRepository.save(any(MesaRecord.class))).thenReturn(expected);

        MesaRecord result = mesaCommandService.createMesa(request);

        assertThat(result.getStatus()).isEqualTo(MesaStatus.OPEN);
        assertThat(result.getValidVotes()).isZero();
        assertThat(result.getQuarantined()).isFalse();
    }

    @Test
    @DisplayName("TC-SC-002 | createMesa → Guarda mesaCode correctamente")
    void tc_sc_002_createMesa_mesa_code() {
        CreateMesaRequest request = new CreateMesaRequest("CALI-MESA-999");
        
        MesaRecord expected = MesaRecord.builder()
                .id(101L)
                .mesaCode("CALI-MESA-999")
                .validVotes(0)
                .blankVotes(0)
                .nullVotes(0)
                .unmarkedVotes(0)
                .status(MesaStatus.OPEN)
                .quarantined(false)
                .build();

        when(mesaRepository.save(any(MesaRecord.class))).thenReturn(expected);

        MesaRecord result = mesaCommandService.createMesa(request);

        assertThat(result.getMesaCode()).isEqualTo("CALI-MESA-999");
    }

    @Test
    @DisplayName("TC-SC-003 | createMesa → Invoke save repository")
    void tc_sc_003_createMesa_repository_save() {
        CreateMesaRequest request = new CreateMesaRequest("TEST-MESA");
        
        when(mesaRepository.save(any(MesaRecord.class))).thenReturn(mesaOpen);

        mesaCommandService.createMesa(request);

        verify(mesaRepository, times(1)).save(any(MesaRecord.class));
    }

    @Test
    @DisplayName("TC-SC-004 | createMesa → Mesa no está cuarentenada inicialmente")
    void tc_sc_004_createMesa_not_quarantined() {
        CreateMesaRequest request = new CreateMesaRequest("BOG-MESA-01");
        
        MesaRecord expected = MesaRecord.builder()
                .id(102L)
                .mesaCode("BOG-MESA-01")
                .status(MesaStatus.OPEN)
                .quarantined(false)
                .build();

        when(mesaRepository.save(any(MesaRecord.class))).thenReturn(expected);

        MesaRecord result = mesaCommandService.createMesa(request);

        assertThat(result.getQuarantined()).isFalse();
    }

    @Test
    @DisplayName("TC-SC-005 | createMesa → Multiple mesas con códigos diferentes")
    void tc_sc_005_createMesa_multiple() {
        for (int i = 0; i < 5; i++) {
            CreateMesaRequest request = new CreateMesaRequest("MESA-" + i);
            
            MesaRecord expected = MesaRecord.builder()
                    .id((long) i)
                    .mesaCode("MESA-" + i)
                    .status(MesaStatus.OPEN)
                    .build();

            when(mesaRepository.save(any(MesaRecord.class))).thenReturn(expected);

            MesaRecord result = mesaCommandService.createMesa(request);

            assertThat(result.getStatus()).isEqualTo(MesaStatus.OPEN);
        }
        
        verify(mesaRepository, times(5)).save(any(MesaRecord.class));
    }

    // ============================================
    // CLOSE MESA: OPEN → CLOSED (TC-SC-006-15)
    // ============================================

    @Test
    @DisplayName("TC-SC-006 | closeMesa → Transición OPEN → CLOSED exitosa")
    void tc_sc_006_closeMesa_open_to_closed() {
        CloseMesaRequest request = new CloseMesaRequest(100, 5, 3, 2, null);


        
        MesaRecord closed = MesaRecord.builder()
                .id(1L)
                .mesaCode("BOG-001")
                .validVotes(100)
                .blankVotes(5)
                .nullVotes(3)
                .unmarkedVotes(2)
                .status(MesaStatus.CLOSED)
                .build();

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaOpen));
        when(pdfService.generatePdf(any())).thenReturn(createTempPdfPath());
        when(mesaRepository.save(any())).thenReturn(closed);
        when(e14Repository.save(any())).thenReturn(new E14Record());

        MesaRecord result = mesaCommandService.closeMesa(1L, request);

        assertThat(result.getStatus()).isEqualTo(MesaStatus.CLOSED);
        assertThat(result.getValidVotes()).isEqualTo(100);
    }

    @Test
    @DisplayName("TC-SC-007 | closeMesa → Rechaza CLOSED → CLOSED")
    void tc_sc_007_closeMesa_already_closed() {
        CloseMesaRequest request = new CloseMesaRequest(100, 5, 3, 2, null);

        when(mesaRepository.findById(2L)).thenReturn(Optional.of(mesaClosed));

        assertThatThrownBy(() -> mesaCommandService.closeMesa(2L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid state transition: CLOSED → CLOSED");
    }

    @Test
    @DisplayName("TC-SC-008 | closeMesa → Rechaza VERIFIED → CLOSED")
    void tc_sc_008_closeMesa_verified_to_closed() {
        CloseMesaRequest request = new CloseMesaRequest(100, 5, 3, 2, null);

        when(mesaRepository.findById(3L)).thenReturn(Optional.of(mesaVerified));

        assertThatThrownBy(() -> mesaCommandService.closeMesa(3L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid state transition: VERIFIED → CLOSED");
    }

    @Test
    @DisplayName("TC-SC-009 | closeMesa → Rechaza SEALED → CLOSED")
    void tc_sc_009_closeMesa_sealed_to_closed() {
        CloseMesaRequest request = new CloseMesaRequest(100, 5, 3, 2, null);

        when(mesaRepository.findById(4L)).thenReturn(Optional.of(mesaSealed));

        assertThatThrownBy(() -> mesaCommandService.closeMesa(4L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid state transition: SEALED → CLOSED");
    }

    @Test
    @DisplayName("TC-SC-010 | closeMesa → Mesa no encontrada lanza excepción")
    void tc_sc_010_closeMesa_not_found() {
        CloseMesaRequest request = new CloseMesaRequest(100, 5, 3, 2, null);

        when(mesaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mesaCommandService.closeMesa(999L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mesa not found");
    }

    @Test
    @DisplayName("TC-SC-011 | closeMesa → Genera hash E14 SHA-256")
    void tc_sc_011_closeMesa_generates_hash() {
        CloseMesaRequest request = new CloseMesaRequest(100, 5, 3, 2, null);
        
        MesaRecord closed = MesaRecord.builder()
                .id(1L)
                .mesaCode("BOG-001")
                .status(MesaStatus.CLOSED)
                .build();

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaOpen));
        when(pdfService.generatePdf(any())).thenReturn(createTempPdfPath());
        when(mesaRepository.save(any())).thenReturn(closed);
        when(e14Repository.save(any())).thenReturn(new E14Record());

        mesaCommandService.closeMesa(1L, request);

        verify(e14Repository, times(1)).save(any(E14Record.class));
    }

    @Test
    @DisplayName("TC-SC-012 | closeMesa → Crea E14 en estado DRAFT")
    void tc_sc_012_closeMesa_e14_draft_status() {
        CloseMesaRequest request = new CloseMesaRequest(100, 5, 3, 2, null);
        
        MesaRecord closed = MesaRecord.builder()
                .id(1L)
                .mesaCode("BOG-001")
                .status(MesaStatus.CLOSED)
                .build();

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaOpen));
        when(pdfService.generatePdf(any())).thenReturn(createTempPdfPath());
        when(mesaRepository.save(any())).thenReturn(closed);
        
        E14Record e14Saved = E14Record.builder()
                .mesaCode("BOG-001")
                .status(E14Status.DRAFT)
                .build();
        when(e14Repository.save(any())).thenReturn(e14Saved);

        mesaCommandService.closeMesa(1L, request);

        verify(e14Repository).save(argThat(e14 -> e14.getStatus() == E14Status.DRAFT));
    }

    @Test
    @DisplayName("TC-SC-013 | closeMesa → Registra todos los votos (valid, blank, null, unmarked)")
    void tc_sc_013_closeMesa_all_vote_counts() {
        CloseMesaRequest request = new CloseMesaRequest(150, 10, 5, 3, null);
        
        MesaRecord closed = MesaRecord.builder()
                .id(1L)
                .mesaCode("BOG-001")
                .validVotes(150)
                .blankVotes(10)
                .nullVotes(5)
                .unmarkedVotes(3)
                .status(MesaStatus.CLOSED)
                .build();

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaOpen));
        when(pdfService.generatePdf(any())).thenReturn(createTempPdfPath());
        when(mesaRepository.save(any())).thenReturn(closed);
        when(e14Repository.save(any())).thenReturn(new E14Record());

        MesaRecord result = mesaCommandService.closeMesa(1L, request);

        assertThat(result.getValidVotes()).isEqualTo(150);
        assertThat(result.getBlankVotes()).isEqualTo(10);
        assertThat(result.getNullVotes()).isEqualTo(5);
        assertThat(result.getUnmarkedVotes()).isEqualTo(3);
    }

    @Test
    @DisplayName("TC-SC-014 | closeMesa → Suma total de votos es correcta")
    void tc_sc_014_closeMesa_vote_sum() {
        CloseMesaRequest request = new CloseMesaRequest(100, 5, 3, 2, null);
        int totalVotes = 100 + 5 + 3 + 2;

        MesaRecord closed = MesaRecord.builder()
                .id(1L)
                .mesaCode("BOG-001")
                .validVotes(100)
                .blankVotes(5)
                .nullVotes(3)
                .unmarkedVotes(2)
                .status(MesaStatus.CLOSED)
                .build();

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaOpen));
        when(pdfService.generatePdf(any())).thenReturn(createTempPdfPath());
        when(mesaRepository.save(any())).thenReturn(closed);
        when(e14Repository.save(any())).thenReturn(new E14Record());

        MesaRecord result = mesaCommandService.closeMesa(1L, request);

        int sum = result.getValidVotes() + result.getBlankVotes() + 
                  result.getNullVotes() + result.getUnmarkedVotes();
        assertThat(sum).isEqualTo(totalVotes);
    }

    @Test
    @DisplayName("TC-SC-015 | closeMesa → PDF generado para mesa cerrada")
    void tc_sc_015_closeMesa_pdf_generation() {
        CloseMesaRequest request = new CloseMesaRequest(100, 5, 3, 2, null);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaOpen));
        when(pdfService.generatePdf(any())).thenReturn(createTempPdfPath());
        when(mesaRepository.save(any())).thenReturn(mesaClosed);
        when(e14Repository.save(any())).thenReturn(new E14Record());

        mesaCommandService.closeMesa(1L, request);

        verify(pdfService, times(1)).generatePdf(any(MesaRecord.class));
    }

    // ============================================
    // VERIFY MESA: CLOSED → VERIFIED (TC-SC-016-25)
    // ============================================

    @Test
    @DisplayName("TC-SC-016 | verifyMesa → Transición CLOSED → VERIFIED exitosa")
    void tc_sc_016_verifyMesa_closed_to_verified() {
        MesaRecord verified = MesaRecord.builder()
                .id(2L)
                .mesaCode("BOG-002")
                .status(MesaStatus.VERIFIED)
                .build();

        when(mesaRepository.findById(2L)).thenReturn(Optional.of(mesaClosed));
        when(mesaRepository.save(any())).thenReturn(verified);

        MesaRecord result = mesaCommandService.verifyMesa(2L);

        assertThat(result.getStatus()).isEqualTo(MesaStatus.VERIFIED);
    }

    @Test
    @DisplayName("TC-SC-017 | verifyMesa → Rechaza OPEN → VERIFIED")
    void tc_sc_017_verifyMesa_open_to_verified() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaOpen));

        assertThatThrownBy(() -> mesaCommandService.verifyMesa(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid state transition: OPEN → VERIFIED");
    }

    @Test
    @DisplayName("TC-SC-018 | verifyMesa → Rechaza VERIFIED → VERIFIED")
    void tc_sc_018_verifyMesa_already_verified() {
        when(mesaRepository.findById(3L)).thenReturn(Optional.of(mesaVerified));

        assertThatThrownBy(() -> mesaCommandService.verifyMesa(3L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid state transition: VERIFIED → VERIFIED");
    }

    @Test
    @DisplayName("TC-SC-019 | verifyMesa → Rechaza SEALED → VERIFIED")
    void tc_sc_019_verifyMesa_sealed_to_verified() {
        when(mesaRepository.findById(4L)).thenReturn(Optional.of(mesaSealed));

        assertThatThrownBy(() -> mesaCommandService.verifyMesa(4L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid state transition: SEALED → VERIFIED");
    }

    @Test
    @DisplayName("TC-SC-020 | verifyMesa → Mesa no encontrada lanza excepción")
    void tc_sc_020_verifyMesa_not_found() {
        when(mesaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mesaCommandService.verifyMesa(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mesa not found");
    }

    @Test
    @DisplayName("TC-SC-021 | verifyMesa → Preserva votos durante verificación")
    void tc_sc_021_verifyMesa_preserves_votes() {
        MesaRecord verified = MesaRecord.builder()
                .id(2L)
                .mesaCode("BOG-002")
                .validVotes(100)
                .blankVotes(5)
                .nullVotes(3)
                .unmarkedVotes(2)
                .status(MesaStatus.VERIFIED)
                .build();

        when(mesaRepository.findById(2L)).thenReturn(Optional.of(mesaClosed));
        when(mesaRepository.save(any())).thenReturn(verified);

        MesaRecord result = mesaCommandService.verifyMesa(2L);

        assertThat(result.getValidVotes()).isEqualTo(100);
        assertThat(result.getBlankVotes()).isEqualTo(5);
    }

    @Test
    @DisplayName("TC-SC-022 | verifyMesa → Actualiza repositorio")
    void tc_sc_022_verifyMesa_repository_save() {
        when(mesaRepository.findById(2L)).thenReturn(Optional.of(mesaClosed));
        when(mesaRepository.save(any())).thenReturn(mesaVerified);

        mesaCommandService.verifyMesa(2L);

        verify(mesaRepository, times(1)).save(any(MesaRecord.class));
    }

    @Test
    @DisplayName("TC-SC-023 | verifyMesa → Múltiples mesas verificadas secuencialmente")
    void tc_sc_023_verifyMesa_multiple_sequential() {
        for (long i = 2; i <= 4; i++) {
            MesaRecord closed = MesaRecord.builder()
                    .id(i)
                    .status(MesaStatus.CLOSED)
                    .build();
            
            MesaRecord verified = MesaRecord.builder()
                    .id(i)
                    .status(MesaStatus.VERIFIED)
                    .build();

            when(mesaRepository.findById(i)).thenReturn(Optional.of(closed));
            when(mesaRepository.save(any())).thenReturn(verified);

            MesaRecord result = mesaCommandService.verifyMesa(i);

            assertThat(result.getStatus()).isEqualTo(MesaStatus.VERIFIED);
        }
        
        verify(mesaRepository, times(3)).save(any(MesaRecord.class));
    }

    @Test
    @DisplayName("TC-SC-024 | verifyMesa → Retorna mesa actualizada con nuevo estado")
    void tc_sc_024_verifyMesa_returns_updated_mesa() {
        MesaRecord expected = MesaRecord.builder()
                .id(2L)
                .mesaCode("BOG-002")
                .status(MesaStatus.VERIFIED)
                .quarantined(false)
                .build();

        when(mesaRepository.findById(2L)).thenReturn(Optional.of(mesaClosed));
        when(mesaRepository.save(any())).thenReturn(expected);

        MesaRecord result = mesaCommandService.verifyMesa(2L);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("TC-SC-025 | verifyMesa → No modifica flag quarantined")
    void tc_sc_025_verifyMesa_preserves_quarantined_flag() {
        MesaRecord verified = MesaRecord.builder()
                .id(2L)
                .status(MesaStatus.VERIFIED)
                .quarantined(false)
                .build();

        when(mesaRepository.findById(2L)).thenReturn(Optional.of(mesaClosed));
        when(mesaRepository.save(any())).thenReturn(verified);

        MesaRecord result = mesaCommandService.verifyMesa(2L);

        assertThat(result.getQuarantined()).isFalse();
    }

    // ============================================
    // SEAL MESA: VERIFIED → SEALED (TC-SC-026-35)
    // ============================================

    @Test
    @DisplayName("TC-SC-026 | sealMesa → Transición VERIFIED → SEALED exitosa")
    void tc_sc_026_sealMesa_verified_to_sealed() {
        MesaRecord sealed = MesaRecord.builder()
                .id(3L)
                .mesaCode("BOG-003")
                .status(MesaStatus.SEALED)
                .build();

        when(mesaRepository.findById(3L)).thenReturn(Optional.of(mesaVerified));
        when(mesaRepository.save(any())).thenReturn(sealed);
        when(e14Repository.findByMesaCode("BOG-003")).thenReturn(Optional.of(
            E14Record.builder().mesaCode("BOG-003").status(E14Status.DRAFT).build()
        ));
        when(e14Repository.save(any())).thenReturn(new E14Record());

        MesaRecord result = mesaCommandService.sealMesa(3L);

        assertThat(result.getStatus()).isEqualTo(MesaStatus.SEALED);
    }

    @Test
    @DisplayName("TC-SC-027 | sealMesa → Rechaza OPEN → SEALED")
    void tc_sc_027_sealMesa_open_to_sealed() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaOpen));

        assertThatThrownBy(() -> mesaCommandService.sealMesa(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid state transition: OPEN → SEALED");
    }

    @Test
    @DisplayName("TC-SC-028 | sealMesa → Rechaza CLOSED → SEALED")
    void tc_sc_028_sealMesa_closed_to_sealed() {
        when(mesaRepository.findById(2L)).thenReturn(Optional.of(mesaClosed));

        assertThatThrownBy(() -> mesaCommandService.sealMesa(2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid state transition: CLOSED → SEALED");
    }

    @Test
    @DisplayName("TC-SC-029 | sealMesa → Rechaza SEALED → SEALED")
    void tc_sc_029_sealMesa_already_sealed() {
        when(mesaRepository.findById(4L)).thenReturn(Optional.of(mesaSealed));

        assertThatThrownBy(() -> mesaCommandService.sealMesa(4L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid state transition: SEALED → SEALED");
    }

    @Test
    @DisplayName("TC-SC-030 | sealMesa → Mesa no encontrada lanza excepción")
    void tc_sc_030_sealMesa_not_found() {
        when(mesaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mesaCommandService.sealMesa(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mesa not found");
    }

    @Test
    @DisplayName("TC-SC-031 | sealMesa → Actualiza E14 a estado SEALED")
    void tc_sc_031_sealMesa_e14_sealed() {
        MesaRecord sealed = MesaRecord.builder()
                .id(3L)
                .mesaCode("BOG-003")
                .status(MesaStatus.SEALED)
                .build();

        E14Record e14 = E14Record.builder()
                .mesaCode("BOG-003")
                .status(E14Status.DRAFT)
                .build();

        when(mesaRepository.findById(3L)).thenReturn(Optional.of(mesaVerified));
        when(mesaRepository.save(any())).thenReturn(sealed);
        when(e14Repository.findByMesaCode("BOG-003")).thenReturn(Optional.of(e14));
        when(e14Repository.save(any())).thenReturn(
            E14Record.builder().status(E14Status.SEALED).build()
        );

        mesaCommandService.sealMesa(3L);

        verify(e14Repository).save(argThat(e14rec -> e14rec.getStatus() == E14Status.SEALED));
    }

    @Test
    @DisplayName("TC-SC-032 | sealMesa → E14 no encontrado lanza excepción")
    void tc_sc_032_sealMesa_e14_not_found() {
        when(mesaRepository.findById(3L)).thenReturn(Optional.of(mesaVerified));
        when(e14Repository.findByMesaCode("BOG-003")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mesaCommandService.sealMesa(3L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("E14 not found");
    }

    @Test
    @DisplayName("TC-SC-033 | sealMesa → Preserva datos de mesa durante sellado")
    void tc_sc_033_sealMesa_preserves_data() {
        MesaRecord sealed = MesaRecord.builder()
                .id(3L)
                .mesaCode("BOG-003")
                .validVotes(100)
                .blankVotes(5)
                .status(MesaStatus.SEALED)
                .build();

        when(mesaRepository.findById(3L)).thenReturn(Optional.of(mesaVerified));
        when(mesaRepository.save(any())).thenReturn(sealed);
        when(e14Repository.findByMesaCode("BOG-003")).thenReturn(Optional.of(
            E14Record.builder().mesaCode("BOG-003").build()
        ));
        when(e14Repository.save(any())).thenReturn(new E14Record());

        MesaRecord result = mesaCommandService.sealMesa(3L);

        assertThat(result.getMesaCode()).isEqualTo("BOG-003");
        assertThat(result.getValidVotes()).isEqualTo(100);
    }

    @Test
    @DisplayName("TC-SC-034 | sealMesa → Transición final (immutable)")
    void tc_sc_034_sealMesa_final_transition() {
        MesaRecord sealed = MesaRecord.builder()
                .id(3L)
                .status(MesaStatus.SEALED)
                .build();

        when(mesaRepository.findById(3L)).thenReturn(Optional.of(mesaVerified));
        when(mesaRepository.save(any())).thenReturn(sealed);
        when(e14Repository.findByMesaCode("BOG-003")).thenReturn(Optional.of(
            E14Record.builder().mesaCode("BOG-003").build()
        ));
        when(e14Repository.save(any())).thenReturn(new E14Record());

        MesaRecord result = mesaCommandService.sealMesa(3L);

        when(mesaRepository.findById(3L)).thenReturn(Optional.of(result));
        assertThatThrownBy(() -> mesaCommandService.verifyMesa(3L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("TC-SC-035 | sealMesa → Múltiples mesas selladas secuencialmente")
    void tc_sc_035_sealMesa_multiple_sequential() {
        for (long i = 3; i <= 5; i++) {
            MesaRecord verified = MesaRecord.builder()
                    .id(i)
                    .mesaCode("BOG-" + String.format("%03d", i))
                    .status(MesaStatus.VERIFIED)
                    .build();
            
            MesaRecord sealed = MesaRecord.builder()
                    .id(i)
                    .mesaCode("BOG-" + String.format("%03d", i))
                    .status(MesaStatus.SEALED)
                    .build();

            when(mesaRepository.findById(i)).thenReturn(Optional.of(verified));
            when(mesaRepository.save(any())).thenReturn(sealed);
            when(e14Repository.findByMesaCode("BOG-" + String.format("%03d", i)))
                    .thenReturn(Optional.of(E14Record.builder().build()));
            when(e14Repository.save(any())).thenReturn(new E14Record());

            MesaRecord result = mesaCommandService.sealMesa(i);

            assertThat(result.getStatus()).isEqualTo(MesaStatus.SEALED);
        }
        
        verify(mesaRepository, times(3)).save(any(MesaRecord.class));
    }

    // ============================================
    // QUARANTINE MESA: * → QUARANTINED (TC-SC-036-45)
    // ============================================

    @Test
    @DisplayName("TC-SC-036 | quarantineMesa → OPEN → QUARANTINED")
    void tc_sc_036_quarantineMesa_from_open() {
        MesaRecord quarantined = MesaRecord.builder()
                .id(1L)
                .mesaCode("BOG-001")
                .status(MesaStatus.QUARANTINED)
                .quarantined(true)
                .build();

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaOpen));
        when(mesaRepository.save(any())).thenReturn(quarantined);

        MesaRecord result = mesaCommandService.quarantineMesa(1L, "Voto fraudulento detectado");

        assertThat(result.getStatus()).isEqualTo(MesaStatus.QUARANTINED);
        assertThat(result.getQuarantined()).isTrue();
    }

    @Test
    @DisplayName("TC-SC-037 | quarantineMesa → CLOSED → QUARANTINED")
    void tc_sc_037_quarantineMesa_from_closed() {
        MesaRecord quarantined = MesaRecord.builder()
                .id(2L)
                .mesaCode("BOG-002")
                .status(MesaStatus.QUARANTINED)
                .quarantined(true)
                .build();

        when(mesaRepository.findById(2L)).thenReturn(Optional.of(mesaClosed));
        when(mesaRepository.save(any())).thenReturn(quarantined);

        MesaRecord result = mesaCommandService.quarantineMesa(2L, "Discrepancia VVPAT detectada");

        assertThat(result.getStatus()).isEqualTo(MesaStatus.QUARANTINED);
    }

    @Test
    @DisplayName("TC-SC-038 | quarantineMesa → VERIFIED → QUARANTINED")
    void tc_sc_038_quarantineMesa_from_verified() {
        MesaRecord quarantined = MesaRecord.builder()
                .id(3L)
                .mesaCode("BOG-003")
                .status(MesaStatus.QUARANTINED)
                .quarantined(true)
                .build();

        when(mesaRepository.findById(3L)).thenReturn(Optional.of(mesaVerified));
        when(mesaRepository.save(any())).thenReturn(quarantined);

        MesaRecord result = mesaCommandService.quarantineMesa(3L, "Firma inválida en acta");

        assertThat(result.getStatus()).isEqualTo(MesaStatus.QUARANTINED);
    }

    @Test
    @DisplayName("TC-SC-039 | quarantineMesa → Rechaza SEALED → QUARANTINED")
    void tc_sc_039_quarantineMesa_sealed_immutable() {
        when(mesaRepository.findById(4L)).thenReturn(Optional.of(mesaSealed));

        assertThatThrownBy(() -> mesaCommandService.quarantineMesa(4L, "Anomalía detectada"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot quarantine SEALED mesa");
    }

    @Test
    @DisplayName("TC-SC-040 | quarantineMesa → Mesa no encontrada lanza excepción")
    void tc_sc_040_quarantineMesa_not_found() {
        when(mesaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mesaCommandService.quarantineMesa(999L, "Razón"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mesa not found");
    }

    @Test
    @DisplayName("TC-SC-041 | quarantineMesa → Establece flag quarantined en true")
    void tc_sc_041_quarantineMesa_flag_true() {
        MesaRecord quarantined = MesaRecord.builder()
                .id(1L)
                .status(MesaStatus.QUARANTINED)
                .quarantined(true)
                .build();

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaOpen));
        when(mesaRepository.save(any())).thenReturn(quarantined);

        MesaRecord result = mesaCommandService.quarantineMesa(1L, "Test");

        assertThat(result.getQuarantined()).isTrue();
    }

    @Test
    @DisplayName("TC-SC-042 | quarantineMesa → Preserva datos de mesa")
    void tc_sc_042_quarantineMesa_preserves_data() {
        MesaRecord quarantined = MesaRecord.builder()
                .id(2L)
                .mesaCode("BOG-002")
                .validVotes(100)
                .status(MesaStatus.QUARANTINED)
                .build();

        when(mesaRepository.findById(2L)).thenReturn(Optional.of(mesaClosed));
        when(mesaRepository.save(any())).thenReturn(quarantined);

        MesaRecord result = mesaCommandService.quarantineMesa(2L, "Anomalía");

        assertThat(result.getMesaCode()).isEqualTo("BOG-002");
        assertThat(result.getValidVotes()).isEqualTo(100);
    }

    @Test
    @DisplayName("TC-SC-043 | quarantineMesa → Múltiples mesas en cuarentena simultáneamente")
    void tc_sc_043_quarantineMesa_multiple() {
        for (long i = 1; i <= 3; i++) {
            MesaRecord mesa = MesaRecord.builder()
                    .id(i)
                    .status(i < 4 ? MesaStatus.OPEN : MesaStatus.CLOSED)
                    .build();
            
            MesaRecord quarantined = MesaRecord.builder()
                    .id(i)
                    .status(MesaStatus.QUARANTINED)
                    .quarantined(true)
                    .build();

            when(mesaRepository.findById(i)).thenReturn(Optional.of(mesa));
            when(mesaRepository.save(any())).thenReturn(quarantined);

            MesaRecord result = mesaCommandService.quarantineMesa(i, "Irregularidad");

            assertThat(result.getStatus()).isEqualTo(MesaStatus.QUARANTINED);
        }
        
        verify(mesaRepository, times(3)).save(any(MesaRecord.class));
    }

    @Test
    @DisplayName("TC-SC-044 | quarantineMesa → Detecta anomalías en diferentes etapas")
    void tc_sc_044_quarantineMesa_various_reasons() {
        String[] reasons = {
            "Voto impugnado",
            "Discrepancia de conteo",
            "Certificado inválido",
            "Irregularidad procesal"
        };

        for (int i = 0; i < reasons.length; i++) {
            MesaRecord mesa = MesaRecord.builder()
                    .id((long) i + 1)
                    .status(MesaStatus.OPEN)
                    .build();
            
            MesaRecord quarantined = MesaRecord.builder()
                    .id((long) i + 1)
                    .status(MesaStatus.QUARANTINED)
                    .quarantined(true)
                    .build();

            when(mesaRepository.findById((long) i + 1)).thenReturn(Optional.of(mesa));
            when(mesaRepository.save(any())).thenReturn(quarantined);

            mesaCommandService.quarantineMesa((long) i + 1, reasons[i]);
        }

        verify(mesaRepository, times(reasons.length)).save(any(MesaRecord.class));
    }

    @Test
    @DisplayName("TC-SC-045 | quarantineMesa → Estado QUARANTINED es final para investigación")
    void tc_sc_045_quarantineMesa_investigative_state() {
        MesaRecord quarantined = MesaRecord.builder()
                .id(1L)
                .status(MesaStatus.QUARANTINED)
                .quarantined(true)
                .build();

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaOpen));
        when(mesaRepository.save(any())).thenReturn(quarantined);

        MesaRecord result = mesaCommandService.quarantineMesa(1L, "Pendiente investigación");

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(result));
        assertThatThrownBy(() -> mesaCommandService.sealMesa(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    // ============================================
    // INVALID TRANSITIONS (TC-SC-046-50)
    // ============================================

    @Test
    @DisplayName("TC-SC-046 | FSM → Rechaza todas las transiciones inválidas (matrix test)")
    void tc_sc_046_invalid_transitions_matrix() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaOpen));
        assertThatThrownBy(() -> mesaCommandService.verifyMesa(1L))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> mesaCommandService.sealMesa(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("TC-SC-047 | FSM → Closed loop transitions son prevenidas")
    void tc_sc_047_no_backward_transitions() {
        when(mesaRepository.findById(2L)).thenReturn(Optional.of(mesaClosed));
        
        assertThatThrownBy(() -> mesaCommandService.closeMesa(2L, new CloseMesaRequest(0, 0, 0, 0, null)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("TC-SC-048 | FSM → Saltos de estados son rechazados")
    void tc_sc_048_no_state_skipping() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaOpen));
        assertThatThrownBy(() -> mesaCommandService.sealMesa(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("TC-SC-049 | FSM → Transiciones paralelas inválidas")
    void tc_sc_049_parallel_transitions_invalid() {
        MesaRecord verified = MesaRecord.builder().id(2L).status(MesaStatus.VERIFIED).build();
        
        when(mesaRepository.findById(2L)).thenReturn(Optional.of(mesaClosed));
        when(mesaRepository.save(any())).thenReturn(verified);
        
        MesaRecord result = mesaCommandService.verifyMesa(2L);
        
        when(mesaRepository.findById(2L)).thenReturn(Optional.of(result));
        when(mesaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MesaRecord quarantined = mesaCommandService.quarantineMesa(2L, "Too late");

        assertThat(quarantined.getStatus()).isEqualTo(MesaStatus.QUARANTINED);
        assertThat(quarantined.getQuarantined()).isTrue();
    }

    @Test
    @DisplayName("TC-SC-050 | FSM → Completar flujo válido OPEN→CLOSED→VERIFIED→SEALED")
    void tc_sc_050_valid_complete_flow() {
        MesaRecord open = MesaRecord.builder()
                .id(1L)
                .mesaCode("FLOW-001")
                .status(MesaStatus.OPEN)
                .build();
        
        MesaRecord closed = MesaRecord.builder()
                .id(1L)
                .mesaCode("FLOW-001")
                .status(MesaStatus.CLOSED)
                .build();
        
        MesaRecord verified = MesaRecord.builder()
                .id(1L)
                .mesaCode("FLOW-001")
                .status(MesaStatus.VERIFIED)
                .build();
        
        MesaRecord sealed = MesaRecord.builder()
                .id(1L)
                .mesaCode("FLOW-001")
                .status(MesaStatus.SEALED)
                .build();

        when(mesaRepository.findById(1L))
                .thenReturn(Optional.of(open))
                .thenReturn(Optional.of(closed))
                .thenReturn(Optional.of(verified));
        when(mesaRepository.save(any()))
                .thenReturn(closed)
                .thenReturn(verified)
                .thenReturn(sealed);
        when(pdfService.generatePdf(any())).thenReturn(createTempPdfPath());
        when(e14Repository.save(any())).thenReturn(new E14Record());
        when(e14Repository.findByMesaCode("FLOW-001"))
                .thenReturn(Optional.of(E14Record.builder().mesaCode("FLOW-001").build()));

        CloseMesaRequest closeRequest = new CloseMesaRequest(100, 5, 3, 2, null);
        MesaRecord result1 = mesaCommandService.closeMesa(1L, closeRequest);
        assertThat(result1.getStatus()).isEqualTo(MesaStatus.CLOSED);

        MesaRecord result2 = mesaCommandService.verifyMesa(1L);
        assertThat(result2.getStatus()).isEqualTo(MesaStatus.VERIFIED);

        MesaRecord result3 = mesaCommandService.sealMesa(1L);
        assertThat(result3.getStatus()).isEqualTo(MesaStatus.SEALED);
    }
}
