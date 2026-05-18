package com.registraduria.scrutiny_service.vvpat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.mockito.ArgumentCaptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.registraduria.scrutiny_service.events.VvpatMismatchEvent;
import com.registraduria.scrutiny_service.events.producer.VvpatEventProducer;
import com.registraduria.scrutiny_service.mesa.entity.MesaRecord;
import com.registraduria.scrutiny_service.mesa.enums.MesaStatus;
import com.registraduria.scrutiny_service.mesa.repository.MesaRepository;
import com.registraduria.scrutiny_service.vvpat.dto.VvpatScanRequest;
import com.registraduria.scrutiny_service.vvpat.entity.VvpatScanRecord;
import com.registraduria.scrutiny_service.vvpat.enums.VvpatResult;
import com.registraduria.scrutiny_service.vvpat.repository.VvpatRepository;

@ExtendWith(MockitoExtension.class)
class VvpatServiceTest {

    @Mock
    VvpatRepository repository;

    @Mock
    MesaRepository mesaRepository;

    @Mock
    VvpatEventProducer producer;

    @InjectMocks
    VvpatService service;

    MesaRecord mesa;

    @BeforeEach
    void setUp() {
        mesa = MesaRecord.builder()
                .id(1L)
                .mesaCode("M-100")
                .status(MesaStatus.OPEN)
                .validVotes(50)
                .build();
    }

    @Test
    void scan_when_match_no_publish_no_quarantine() {
        when(mesaRepository.findByMesaCode("M-100")).thenReturn(Optional.of(mesa));
        when(repository.findByMesaCode("M-100")).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        VvpatScanRequest req = new VvpatScanRequest("M-100", "J-1", 50);
        VvpatScanRecord scan = service.scan(req);

        assertThat(scan.getResult()).isEqualTo(VvpatResult.MATCH);
        verify(producer, never()).publishMismatch(any(VvpatMismatchEvent.class));
        verify(mesaRepository, never()).save(any());
    }

    @Test
    void scan_when_mismatch_publishes_event_but_not_quarantine_on_first_attempt() {
        when(mesaRepository.findByMesaCode("M-100")).thenReturn(Optional.of(mesa));
        when(repository.findByMesaCode("M-100")).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        VvpatScanRequest req = new VvpatScanRequest("M-100", "J-2", 49);
        VvpatScanRecord scan = service.scan(req);

        assertThat(scan.getResult()).isEqualTo(VvpatResult.MISMATCH);
        verify(producer, times(1)).publishMismatch(any(VvpatMismatchEvent.class));
        verify(mesaRepository, never()).save(any());
    }

    @Test
    void scan_when_third_mismatch_quarantines_mesa() {
        when(mesaRepository.findByMesaCode("M-100")).thenReturn(Optional.of(mesa));
        when(repository.findByMesaCode("M-100")).thenReturn(List.of(new VvpatScanRecord(), new VvpatScanRecord()));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mesaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VvpatScanRequest req = new VvpatScanRequest("M-100", "J-3", 0);
        VvpatScanRecord scan = service.scan(req);

        assertThat(scan.getResult()).isEqualTo(VvpatResult.MISMATCH);
        verify(producer, times(1)).publishMismatch(any(VvpatMismatchEvent.class));
        verify(mesaRepository, times(1)).save(any());
        assertThat(mesa.getQuarantined()).isTrue();
    }

    @Test
    void scan_when_mesa_not_found_throws() {
        when(mesaRepository.findByMesaCode("M-999")).thenReturn(Optional.empty());

        VvpatScanRequest req = new VvpatScanRequest("M-999", "J-9", 10);

        assertThatThrownBy(() -> service.scan(req))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void scan_when_second_mismatch_does_not_quarantine() {
        when(mesaRepository.findByMesaCode("M-100")).thenReturn(Optional.of(mesa));
        when(repository.findByMesaCode("M-100")).thenReturn(List.of(new VvpatScanRecord()));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        VvpatScanRequest req = new VvpatScanRequest("M-100", "J-2", 49);
        VvpatScanRecord scan = service.scan(req);

        assertThat(scan.getAttempt()).isEqualTo(2);
        assertThat(scan.getResult()).isEqualTo(VvpatResult.MISMATCH);
        verify(producer, times(1)).publishMismatch(any(VvpatMismatchEvent.class));
        verify(mesaRepository, never()).save(any());
        assertThat(mesa.getQuarantined()).isFalse();
    }

    @Test
    void scan_when_match_after_previous_mismatch_does_not_quarantine() {
        when(mesaRepository.findByMesaCode("M-100")).thenReturn(Optional.of(mesa));
        when(repository.findByMesaCode("M-100")).thenReturn(List.of(new VvpatScanRecord()));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        VvpatScanRequest req = new VvpatScanRequest("M-100", "J-3", 50);
        VvpatScanRecord scan = service.scan(req);

        assertThat(scan.getAttempt()).isEqualTo(2);
        assertThat(scan.getResult()).isEqualTo(VvpatResult.MATCH);
        verify(producer, never()).publishMismatch(any(VvpatMismatchEvent.class));
        verify(mesaRepository, never()).save(any());
        assertThat(mesa.getQuarantined()).isFalse();
    }

    @Test
    void scan_when_quarantined_mesa_matches_does_not_unquarantine() {
        mesa.setQuarantined(true);
        when(mesaRepository.findByMesaCode("M-100")).thenReturn(Optional.of(mesa));
        when(repository.findByMesaCode("M-100")).thenReturn(List.of(new VvpatScanRecord(), new VvpatScanRecord(), new VvpatScanRecord()));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        VvpatScanRequest req = new VvpatScanRequest("M-100", "J-5", 50);
        VvpatScanRecord scan = service.scan(req);

        assertThat(scan.getAttempt()).isEqualTo(4);
        assertThat(scan.getResult()).isEqualTo(VvpatResult.MATCH);
        verify(producer, never()).publishMismatch(any(VvpatMismatchEvent.class));
        verify(mesaRepository, never()).save(any());
        assertThat(mesa.getQuarantined()).isTrue();
    }

    @Test
    void scan_when_scan_saved_has_correct_attempt_and_result() {
        when(mesaRepository.findByMesaCode("M-100")).thenReturn(Optional.of(mesa));
        when(repository.findByMesaCode("M-100")).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        VvpatScanRequest req = new VvpatScanRequest("M-100", "J-1", 50);
        VvpatScanRecord scan = service.scan(req);

        assertThat(scan.getAttempt()).isEqualTo(1);
        assertThat(scan.getMesaCode()).isEqualTo("M-100");
        assertThat(scan.getJuradoId()).isEqualTo("J-1");
        assertThat(scan.getResult()).isEqualTo(VvpatResult.MATCH);
        verify(repository, times(1)).save(any(VvpatScanRecord.class));
    }

    @Test
    void scan_when_mismatch_event_contains_expected_details() {
        when(mesaRepository.findByMesaCode("M-100")).thenReturn(Optional.of(mesa));
        when(repository.findByMesaCode("M-100")).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        VvpatScanRequest req = new VvpatScanRequest("M-100", "J-5", 49);
        service.scan(req);

        ArgumentCaptor<VvpatMismatchEvent> eventCaptor = ArgumentCaptor.forClass(VvpatMismatchEvent.class);
        verify(producer, times(1)).publishMismatch(eventCaptor.capture());

        VvpatMismatchEvent event = eventCaptor.getValue();
        assertThat(event.mesaCode()).isEqualTo("M-100");
        assertThat(event.digitalVotes()).isEqualTo(50);
        assertThat(event.physicalVotes()).isEqualTo(49);
        assertThat(event.attempt()).isEqualTo(1);
    }

    @Test
    void scan_when_fourth_mismatch_keeps_quarantined() {
        when(mesaRepository.findByMesaCode("M-100")).thenReturn(Optional.of(mesa));
        when(repository.findByMesaCode("M-100")).thenReturn(List.of(new VvpatScanRecord(), new VvpatScanRecord(), new VvpatScanRecord()));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mesaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VvpatScanRequest req = new VvpatScanRequest("M-100", "J-4", 0);
        VvpatScanRecord scan = service.scan(req);

        assertThat(scan.getAttempt()).isEqualTo(4);
        assertThat(scan.getResult()).isEqualTo(VvpatResult.MISMATCH);
        verify(producer, times(1)).publishMismatch(any(VvpatMismatchEvent.class));
        verify(mesaRepository, times(1)).save(any());
        assertThat(mesa.getQuarantined()).isTrue();
    }

    @Test
    void scan_when_third_mismatch_captures_correct_mesa_save() {
        when(mesaRepository.findByMesaCode("M-100")).thenReturn(Optional.of(mesa));
        when(repository.findByMesaCode("M-100")).thenReturn(List.of(new VvpatScanRecord(), new VvpatScanRecord()));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mesaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VvpatScanRequest req = new VvpatScanRequest("M-100", "J-3", 0);
        service.scan(req);

        ArgumentCaptor<MesaRecord> mesaCaptor = ArgumentCaptor.forClass(MesaRecord.class);
        verify(mesaRepository, times(1)).save(mesaCaptor.capture());

        MesaRecord saved = mesaCaptor.getValue();
        assertThat(saved.getMesaCode()).isEqualTo("M-100");
        assertThat(saved.getQuarantined()).isTrue();
    }

    @Test
    void scan_when_mesa_already_quarantined_does_not_throw() {
        mesa.setQuarantined(true);
        when(mesaRepository.findByMesaCode("M-100")).thenReturn(Optional.of(mesa));
        when(repository.findByMesaCode("M-100")).thenReturn(List.of(new VvpatScanRecord(), new VvpatScanRecord()));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mesaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VvpatScanRequest req = new VvpatScanRequest("M-100", "J-4", 0);
        VvpatScanRecord scan = service.scan(req);

        assertThat(scan.getResult()).isEqualTo(VvpatResult.MISMATCH);
        verify(producer, times(1)).publishMismatch(any(VvpatMismatchEvent.class));
        assertThat(mesa.getQuarantined()).isTrue();
    }

    @Test
    void scan_when_previous_match_increments_attempt() {
        VvpatScanRecord previousMatch = VvpatScanRecord.builder()
                .mesaCode("M-100")
                .juradoId("J-1")
                .physicalVotes(50)
                .attempt(1)
                .result(VvpatResult.MATCH)
                .build();

        when(mesaRepository.findByMesaCode("M-100")).thenReturn(Optional.of(mesa));
        when(repository.findByMesaCode("M-100")).thenReturn(List.of(previousMatch));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        VvpatScanRequest req = new VvpatScanRequest("M-100", "J-2", 49);
        VvpatScanRecord scan = service.scan(req);

        assertThat(scan.getAttempt()).isEqualTo(2);
        assertThat(scan.getResult()).isEqualTo(VvpatResult.MISMATCH);
        verify(producer, times(1)).publishMismatch(any(VvpatMismatchEvent.class));
        verify(mesaRepository, never()).save(any());
        assertThat(mesa.getQuarantined()).isFalse();
    }

    @Test
    void scan_when_third_mismatch_after_matches_quarantines_mesa() {
        VvpatScanRecord firstMatch = VvpatScanRecord.builder()
                .mesaCode("M-100")
                .juradoId("J-1")
                .physicalVotes(50)
                .attempt(1)
                .result(VvpatResult.MATCH)
                .build();
        VvpatScanRecord secondMatch = VvpatScanRecord.builder()
                .mesaCode("M-100")
                .juradoId("J-2")
                .physicalVotes(50)
                .attempt(2)
                .result(VvpatResult.MATCH)
                .build();

        when(mesaRepository.findByMesaCode("M-100")).thenReturn(Optional.of(mesa));
        when(repository.findByMesaCode("M-100")).thenReturn(List.of(firstMatch, secondMatch));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mesaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VvpatScanRequest req = new VvpatScanRequest("M-100", "J-3", 0);
        VvpatScanRecord scan = service.scan(req);

        assertThat(scan.getAttempt()).isEqualTo(3);
        assertThat(scan.getResult()).isEqualTo(VvpatResult.MISMATCH);
        verify(producer, times(1)).publishMismatch(any(VvpatMismatchEvent.class));
        verify(mesaRepository, times(1)).save(any(MesaRecord.class));
        assertThat(mesa.getQuarantined()).isTrue();
    }

    @Test
    void scan_when_repository_save_fails_propagates_exception() {
        when(mesaRepository.findByMesaCode("M-100")).thenReturn(Optional.of(mesa));
        when(repository.findByMesaCode("M-100")).thenReturn(List.of());
        when(repository.save(any())).thenThrow(new RuntimeException("DB unavailable"));

        VvpatScanRequest req = new VvpatScanRequest("M-100", "J-1", 50);

        assertThatThrownBy(() -> service.scan(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB unavailable");

        verify(producer, never()).publishMismatch(any(VvpatMismatchEvent.class));
        verify(mesaRepository, never()).save(any());
    }

    @Test
    void scan_when_second_mismatch_event_attempt_is_two() {
        when(mesaRepository.findByMesaCode("M-100")).thenReturn(Optional.of(mesa));
        when(repository.findByMesaCode("M-100")).thenReturn(List.of(new VvpatScanRecord()));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        VvpatScanRequest req = new VvpatScanRequest("M-100", "J-2", 49);
        service.scan(req);

        ArgumentCaptor<VvpatMismatchEvent> eventCaptor = ArgumentCaptor.forClass(VvpatMismatchEvent.class);
        verify(producer).publishMismatch(eventCaptor.capture());

        VvpatMismatchEvent event = eventCaptor.getValue();
        assertThat(event.attempt()).isEqualTo(2);
        assertThat(event.digitalVotes()).isEqualTo(50);
        assertThat(event.physicalVotes()).isEqualTo(49);
    }

    @Test
    void scan_when_mismatch_record_populates_fields_for_mismatch() {
        when(mesaRepository.findByMesaCode("M-100")).thenReturn(Optional.of(mesa));
        when(repository.findByMesaCode("M-100")).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        VvpatScanRequest req = new VvpatScanRequest("M-100", "J-9", 48);
        VvpatScanRecord scan = service.scan(req);

        assertThat(scan.getMesaCode()).isEqualTo("M-100");
        assertThat(scan.getJuradoId()).isEqualTo("J-9");
        assertThat(scan.getPhysicalVotes()).isEqualTo(48);
        assertThat(scan.getResult()).isEqualTo(VvpatResult.MISMATCH);
        assertThat(scan.getAttempt()).isEqualTo(1);
    }

    @Test
    void scan_when_two_previous_mismatches_quarantines_on_third_mismatch() {
        VvpatScanRecord firstMismatch = VvpatScanRecord.builder()
                .mesaCode("M-100")
                .juradoId("J-1")
                .physicalVotes(49)
                .attempt(1)
                .result(VvpatResult.MISMATCH)
                .build();
        VvpatScanRecord secondMismatch = VvpatScanRecord.builder()
                .mesaCode("M-100")
                .juradoId("J-2")
                .physicalVotes(49)
                .attempt(2)
                .result(VvpatResult.MISMATCH)
                .build();

        when(mesaRepository.findByMesaCode("M-100")).thenReturn(Optional.of(mesa));
        when(repository.findByMesaCode("M-100")).thenReturn(List.of(firstMismatch, secondMismatch));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mesaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VvpatScanRequest req = new VvpatScanRequest("M-100", "J-3", 0);
        VvpatScanRecord scan = service.scan(req);

        assertThat(scan.getAttempt()).isEqualTo(3);
        assertThat(scan.getResult()).isEqualTo(VvpatResult.MISMATCH);
        verify(mesaRepository, times(1)).save(any(MesaRecord.class));
        assertThat(mesa.getQuarantined()).isTrue();
    }

    @Test
    void scan_when_quarantined_mesa_mismatch_keeps_quarantined() {
        mesa.setQuarantined(true);
        when(mesaRepository.findByMesaCode("M-100")).thenReturn(Optional.of(mesa));
        when(repository.findByMesaCode("M-100")).thenReturn(List.of(new VvpatScanRecord(), new VvpatScanRecord(), new VvpatScanRecord()));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mesaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VvpatScanRequest req = new VvpatScanRequest("M-100", "J-5", 0);
        VvpatScanRecord scan = service.scan(req);

        assertThat(scan.getAttempt()).isEqualTo(4);
        assertThat(scan.getResult()).isEqualTo(VvpatResult.MISMATCH);
        assertThat(mesa.getQuarantined()).isTrue();
    }
}
