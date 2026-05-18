package com.registraduria.scrutiny_service.mesa.service;

// ============================================================
//  TIPO: Unitaria — MesaCommandService
//  Verifica: Creación de mesas, cierre de mesas, generación de PDF
//            y cálculo correcto del hash del acta E14.
//  Stack: JUnit 5 + Mockito + Java IO (Temp files)
// ============================================================

import com.registraduria.scrutiny_service.command.repository.E14CommandRepository;
import com.registraduria.scrutiny_service.domain.entity.E14Record;
import com.registraduria.scrutiny_service.domain.enums.E14Status;
import com.registraduria.scrutiny_service.mesa.dto.CloseMesaRequest;
import com.registraduria.scrutiny_service.mesa.dto.CreateMesaRequest;
import com.registraduria.scrutiny_service.mesa.entity.MesaRecord;
import com.registraduria.scrutiny_service.mesa.enums.MesaStatus;
import com.registraduria.scrutiny_service.mesa.repository.MesaRepository;
import com.registraduria.scrutiny_service.pdf.E14PdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MesaCommandService — Pruebas Unitarias")
class MesaCommandServiceTest {

    @Mock
    private MesaRepository mesaRepository;

    @Mock
    private E14CommandRepository e14Repository;

    @Mock
    private E14PdfService pdfService;

    @InjectMocks
    private MesaCommandService mesaCommandService;

    private MesaRecord mesaAbierta;
    private MesaRecord mesaCerrada;
    private MesaRecord mesaVerificada;
    private MesaRecord mesaSellada;

    @BeforeEach
    void setUp() {
        mesaAbierta = MesaRecord.builder()
                .id(10L)
                .mesaCode("BOG-MESA-01")
                .validVotes(0)
                .blankVotes(0)
                .nullVotes(0)
                .unmarkedVotes(0)
                .status(MesaStatus.OPEN)
                .build();

        mesaCerrada = MesaRecord.builder()
                .id(11L)
                .mesaCode("BOG-MESA-02")
                .validVotes(100)
                .blankVotes(5)
                .nullVotes(2)
                .unmarkedVotes(1)
                .status(MesaStatus.CLOSED)
                .build();

        mesaVerificada = MesaRecord.builder()
                .id(12L)
                .mesaCode("BOG-MESA-03")
                .validVotes(100)
                .blankVotes(5)
                .nullVotes(2)
                .unmarkedVotes(1)
                .status(MesaStatus.VERIFIED)
                .build();

        mesaSellada = MesaRecord.builder()
                .id(13L)
                .mesaCode("BOG-MESA-04")
                .validVotes(100)
                .blankVotes(5)
                .nullVotes(2)
                .unmarkedVotes(1)
                .status(MesaStatus.SEALED)
                .build();
    }

    // ------------------------------------------------------------------
    // TC-SC-001 | Crear Mesa (createMesa)
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-SC-001 | createMesa → Guarda mesa en estado OPEN con votos iniciales en 0")
    void tc_sc_001_createMesa_exito() {
        CreateMesaRequest request = new CreateMesaRequest("MED-MESA-05");
        
        MesaRecord mesaGuardada = MesaRecord.builder()
                .id(50L)
                .mesaCode("MED-MESA-05")
                .validVotes(0)
                .blankVotes(0)
                .nullVotes(0)
                .unmarkedVotes(0)
                .status(MesaStatus.OPEN)
                .build();

        when(mesaRepository.save(any(MesaRecord.class))).thenReturn(mesaGuardada);

        MesaRecord result = mesaCommandService.createMesa(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(50L);
        assertThat(result.getMesaCode()).isEqualTo("MED-MESA-05");
        assertThat(result.getStatus()).isEqualTo(MesaStatus.OPEN);
        assertThat(result.getValidVotes()).isZero();
        assertThat(result.getBlankVotes()).isZero();
        assertThat(result.getNullVotes()).isZero();
        assertThat(result.getUnmarkedVotes()).isZero();
    }

    // ------------------------------------------------------------------
    // TC-SC-002 | Cerrar Mesa (closeMesa) - Exito
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-SC-002 | closeMesa → Cierra mesa, actualiza votos, genera PDF y crea E14Record en DRAFT")
    void tc_sc_002_closeMesa_exito(@TempDir Path tempDir) throws Exception {
        // Crear un archivo PDF mock real en el directorio temporal para que Files.readAllBytes funcione
        Path mockPdf = tempDir.resolve("E14_mock.pdf");
        byte[] pdfBytes = "CONTENIDO MOCK PDF E-14 SELLO LEGITIMO 2026".getBytes();
        Files.write(mockPdf, pdfBytes);

        // Calcular el hash esperado del archivo mock
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(pdfBytes);
        StringBuilder hex = new StringBuilder();
        for (byte b : hashBytes) {
            hex.append(String.format("%02x", b));
        }
        String hashEsperado = hex.toString();

        when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesaAbierta));
        when(pdfService.generatePdf(any(MesaRecord.class))).thenReturn(mockPdf.toAbsolutePath().toString());

        CloseMesaRequest request = new CloseMesaRequest(150, 20, 5, 2);

        MesaRecord result = mesaCommandService.closeMesa(10L, request);

        // 1. Validar que la mesa se cerró con los votos del request
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(MesaStatus.CLOSED);
        assertThat(result.getValidVotes()).isEqualTo(150);
        assertThat(result.getBlankVotes()).isEqualTo(20);
        assertThat(result.getNullVotes()).isEqualTo(5);
        assertThat(result.getUnmarkedVotes()).isEqualTo(2);

        verify(mesaRepository, times(1)).save(mesaAbierta);

        // 2. Validar que el acta E14 se guardó en DRAFT con el hash correcto
        ArgumentCaptor<E14Record> e14Captor = ArgumentCaptor.forClass(E14Record.class);
        verify(e14Repository, times(1)).save(e14Captor.capture());

        E14Record e14Guardado = e14Captor.getValue();
        assertThat(e14Guardado).isNotNull();
        assertThat(e14Guardado.getMesaCode()).isEqualTo("BOG-MESA-01");
        assertThat(e14Guardado.getPdfHash()).isEqualTo(hashEsperado);
        assertThat(e14Guardado.getStatus()).isEqualTo(E14Status.DRAFT);
    }

    // ------------------------------------------------------------------
    // TC-SC-003 | Cerrar Mesa ya Cerrada (Falla)
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-SC-003 | closeMesa sobre mesa CLOSED → Lanza IllegalStateException")
    void tc_sc_003_closeMesa_ya_cerrada_falla() {
        mesaAbierta.setStatus(MesaStatus.CLOSED);

        when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesaAbierta));

        CloseMesaRequest request = new CloseMesaRequest(100, 10, 2, 1);

        assertThatThrownBy(() -> mesaCommandService.closeMesa(10L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid state transition");

        verify(mesaRepository, never()).save(any());
        verify(e14Repository, never()).save(any());
        verify(pdfService, never()).generatePdf(any());
    }

    // ------------------------------------------------------------------
    // TC-SC-004 | Cerrar Mesa inexistente (Falla)
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-SC-004 | closeMesa de id no registrado → Lanza IllegalArgumentException")
    void tc_sc_004_closeMesa_inexistente_falla() {
        when(mesaRepository.findById(999L)).thenReturn(Optional.empty());

        CloseMesaRequest request = new CloseMesaRequest(100, 10, 2, 1);

        assertThatThrownBy(() -> mesaCommandService.closeMesa(999L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Mesa not found: 999");

        verify(mesaRepository, never()).save(any());
        verify(e14Repository, never()).save(any());
        verify(pdfService, never()).generatePdf(any());
    }

    // ------------------------------------------------------------------
    // TC-SC-005 | Error al leer PDF al cerrar mesa (Falla)
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-SC-005 | Error en IO de PDF → Lanza IllegalStateException de hash")
    void tc_sc_005_error_io_pdf_falla() {
        when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesaAbierta));
        // Devolvemos una ruta a un archivo que no existe
        when(pdfService.generatePdf(any(MesaRecord.class))).thenReturn("/invalid/path/nonexistent.pdf");

        CloseMesaRequest request = new CloseMesaRequest(150, 20, 5, 2);

        assertThatThrownBy(() -> mesaCommandService.closeMesa(10L, request))
            .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error generating file hash.");

        // La mesa se guarda, pero el E14 no (debido a la excepción en el hash)
        verify(mesaRepository, times(1)).save(mesaAbierta);
        verify(e14Repository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // TC-SC-006 | Verificar Mesa (verifyMesa) - Exito
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-SC-006 | verifyMesa → CLOSED → VERIFIED exitosa")
    void tc_sc_006_verifyMesa_exito() {
        when(mesaRepository.findById(11L)).thenReturn(Optional.of(mesaCerrada));
        when(mesaRepository.save(any(MesaRecord.class))).thenReturn(mesaVerificada);

        MesaRecord result = mesaCommandService.verifyMesa(11L);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(MesaStatus.VERIFIED);
    }

    @Test
    @DisplayName("TC-SC-007 | verifyMesa → OPEN → VERIFIED falla")
    void tc_sc_007_verifyMesa_open_falla() {
        when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesaAbierta));

        assertThatThrownBy(() -> mesaCommandService.verifyMesa(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid state transition");
    }

    @Test
    @DisplayName("TC-SC-008 | verifyMesa → VERIFIED → VERIFIED falla")
    void tc_sc_008_verifyMesa_verified_falla() {
        when(mesaRepository.findById(12L)).thenReturn(Optional.of(mesaVerificada));

        assertThatThrownBy(() -> mesaCommandService.verifyMesa(12L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid state transition");
    }

    @Test
    @DisplayName("TC-SC-009 | verifyMesa → SEALED → VERIFIED falla")
    void tc_sc_009_verifyMesa_sealed_falla() {
        when(mesaRepository.findById(13L)).thenReturn(Optional.of(mesaSellada));

        assertThatThrownBy(() -> mesaCommandService.verifyMesa(13L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid state transition");
    }

    @Test
    @DisplayName("TC-SC-010 | verifyMesa → Mesa no encontrada falla")
    void tc_sc_010_verifyMesa_not_found() {
        when(mesaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mesaCommandService.verifyMesa(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mesa not found");
    }

    // ------------------------------------------------------------------
    // TC-SC-011 | Sellar Mesa (sealMesa) - Exito
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-SC-011 | sealMesa → VERIFIED → SEALED exitosa")
    void tc_sc_011_sealMesa_exito() {
        when(mesaRepository.findById(12L)).thenReturn(Optional.of(mesaVerificada));
        when(mesaRepository.save(any(MesaRecord.class))).thenReturn(mesaSellada);
        when(e14Repository.findByMesaCode("BOG-MESA-03")).thenReturn(Optional.of(
                E14Record.builder().mesaCode("BOG-MESA-03").status(E14Status.DRAFT).build()));
        when(e14Repository.save(any(E14Record.class))).thenReturn(new E14Record());

        MesaRecord result = mesaCommandService.sealMesa(12L);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(MesaStatus.SEALED);
    }

    @Test
    @DisplayName("TC-SC-012 | sealMesa → OPEN → SEALED falla")
    void tc_sc_012_sealMesa_open_falla() {
        when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesaAbierta));

        assertThatThrownBy(() -> mesaCommandService.sealMesa(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid state transition");
    }

    @Test
    @DisplayName("TC-SC-013 | sealMesa → CLOSED → SEALED falla")
    void tc_sc_013_sealMesa_closed_falla() {
        when(mesaRepository.findById(11L)).thenReturn(Optional.of(mesaCerrada));

        assertThatThrownBy(() -> mesaCommandService.sealMesa(11L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid state transition");
    }

    @Test
    @DisplayName("TC-SC-014 | sealMesa → SEALED → SEALED falla")
    void tc_sc_014_sealMesa_sealed_falla() {
        when(mesaRepository.findById(13L)).thenReturn(Optional.of(mesaSellada));

        assertThatThrownBy(() -> mesaCommandService.sealMesa(13L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid state transition");
    }

    @Test
    @DisplayName("TC-SC-015 | sealMesa → Mesa no encontrada falla")
    void tc_sc_015_sealMesa_not_found() {
        when(mesaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mesaCommandService.sealMesa(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mesa not found");
    }

    // ------------------------------------------------------------------
    // TC-SC-016 | Poner en cuarentena (quarantineMesa) - Exito
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-SC-016 | quarantineMesa → OPEN → QUARANTINED exitosa")
    void tc_sc_016_quarantineMesa_open_exito() {
        when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesaAbierta));
        when(mesaRepository.save(any(MesaRecord.class))).thenReturn(
                MesaRecord.builder().id(10L).mesaCode("BOG-MESA-01").status(MesaStatus.QUARANTINED).quarantined(true).build());

        MesaRecord result = mesaCommandService.quarantineMesa(10L, "Irregularidad");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(MesaStatus.QUARANTINED);
        assertThat(result.getQuarantined()).isTrue();
    }

    @Test
    @DisplayName("TC-SC-017 | quarantineMesa → CLOSED → QUARANTINED exitosa")
    void tc_sc_017_quarantineMesa_closed_exito() {
        when(mesaRepository.findById(11L)).thenReturn(Optional.of(mesaCerrada));
        when(mesaRepository.save(any(MesaRecord.class))).thenReturn(
                MesaRecord.builder().id(11L).mesaCode("BOG-MESA-02").status(MesaStatus.QUARANTINED).quarantined(true).build());

        MesaRecord result = mesaCommandService.quarantineMesa(11L, "Discrepancia");

        assertThat(result.getStatus()).isEqualTo(MesaStatus.QUARANTINED);
    }

    @Test
    @DisplayName("TC-SC-018 | quarantineMesa → VERIFIED → QUARANTINED exitosa")
    void tc_sc_018_quarantineMesa_verified_exito() {
        when(mesaRepository.findById(12L)).thenReturn(Optional.of(mesaVerificada));
        when(mesaRepository.save(any(MesaRecord.class))).thenReturn(
                MesaRecord.builder().id(12L).mesaCode("BOG-MESA-03").status(MesaStatus.QUARANTINED).quarantined(true).build());

        MesaRecord result = mesaCommandService.quarantineMesa(12L, "Firma inválida");

        assertThat(result.getStatus()).isEqualTo(MesaStatus.QUARANTINED);
    }

    @Test
    @DisplayName("TC-SC-019 | quarantineMesa → SEALED → QUARANTINED falla")
    void tc_sc_019_quarantineMesa_sealed_falla() {
        when(mesaRepository.findById(13L)).thenReturn(Optional.of(mesaSellada));

        assertThatThrownBy(() -> mesaCommandService.quarantineMesa(13L, "Anomalía"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot quarantine SEALED mesa");
    }

    @Test
    @DisplayName("TC-SC-020 | quarantineMesa → Mesa no encontrada falla")
    void tc_sc_020_quarantineMesa_not_found() {
        when(mesaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mesaCommandService.quarantineMesa(999L, "Razón"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mesa not found");
    }

    @Test
    @DisplayName("TC-SC-021 | sealMesa → Estado SEALED es irreversible")
    void tc_sc_021_sealMesa_irreversible() {
        when(mesaRepository.findById(13L)).thenReturn(Optional.of(mesaSellada));

        assertThatThrownBy(() -> mesaCommandService.closeMesa(13L, new CloseMesaRequest(0, 0, 0, 0)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> mesaCommandService.verifyMesa(13L))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> mesaCommandService.sealMesa(13L))
                .isInstanceOf(IllegalStateException.class);
    }
}
