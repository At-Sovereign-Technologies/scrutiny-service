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
    @DisplayName("TC-SC-003 | closeMesa sobre mesa CLOSED → Lanza RuntimeException")
    void tc_sc_003_closeMesa_ya_cerrada_falla() {
        mesaAbierta.setStatus(MesaStatus.CLOSED);

        when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesaAbierta));

        CloseMesaRequest request = new CloseMesaRequest(100, 10, 2, 1);

        assertThatThrownBy(() -> mesaCommandService.closeMesa(10L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Mesa already closed.");

        verify(mesaRepository, never()).save(any());
        verify(e14Repository, never()).save(any());
        verify(pdfService, never()).generatePdf(any());
    }

    // ------------------------------------------------------------------
    // TC-SC-004 | Cerrar Mesa inexistente (Falla)
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-SC-004 | closeMesa de id no registrado → Lanza NoSuchElementException")
    void tc_sc_004_closeMesa_inexistente_falla() {
        when(mesaRepository.findById(999L)).thenReturn(Optional.empty());

        CloseMesaRequest request = new CloseMesaRequest(100, 10, 2, 1);

        assertThatThrownBy(() -> mesaCommandService.closeMesa(999L, request))
                .isInstanceOf(java.util.NoSuchElementException.class);

        verify(mesaRepository, never()).save(any());
        verify(e14Repository, never()).save(any());
        verify(pdfService, never()).generatePdf(any());
    }

    // ------------------------------------------------------------------
    // TC-SC-005 | Error al leer PDF al cerrar mesa (Falla)
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-SC-005 | Error en IO de PDF → Lanza RuntimeException de hash")
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
}
