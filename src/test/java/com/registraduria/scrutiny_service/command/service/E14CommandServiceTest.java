package com.registraduria.scrutiny_service.command.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.registraduria.scrutiny_service.command.dto.CreateE14Request;
import com.registraduria.scrutiny_service.command.dto.SignE14Request;
import com.registraduria.scrutiny_service.command.repository.E14CommandRepository;
import com.registraduria.scrutiny_service.domain.entity.E14Record;
import com.registraduria.scrutiny_service.domain.enums.E14Status;
import com.registraduria.scrutiny_service.infrastructure.grpc.DisputeGrpcClient;

@ExtendWith(MockitoExtension.class)
class E14CommandServiceTest {

    @Mock
    E14CommandRepository repository;

    @Mock
    DisputeGrpcClient disputeGrpcClient;

    @InjectMocks
    E14CommandService service;

    E14Record draft;

    @BeforeEach
    void setUp() {
        draft = E14Record.builder()
                .id(1L)
                .mesaCode("M-200")
                .pdfHash("abc")
                .status(E14Status.DRAFT)
                .build();
    }

    @Test
    void create_sets_draft_status_and_saves() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        CreateE14Request req = new CreateE14Request("M-200", "City", "abc");
        E14Record res = service.create(req);

        assertThat(res.getStatus()).isEqualTo(E14Status.DRAFT);
        assertThat(res.getMesaCode()).isEqualTo("M-200");
    }

    @Test
    void create_persists_municipality_and_pdf_hash() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        CreateE14Request req = new CreateE14Request("M-201", "Medellín", "hash-xyz");
        E14Record res = service.create(req);

        assertThat(res.getMunicipality()).isEqualTo("Medellín");
        assertThat(res.getPdfHash()).isEqualTo("hash-xyz");
    }

    @Test
    void create_invokes_repository_save() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.create(new CreateE14Request("M-202", "Cali", "hash-123"));

        verify(repository, times(1)).save(any(E14Record.class));
    }

    @Test
    void create_sets_created_at_timestamp() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        CreateE14Request req = new CreateE14Request("M-203", "Pasto", "hash-abc");
        E14Record res = service.create(req);

        assertThat(res.getCreatedAt()).isNotNull();
        assertThat(res.getCreatedAt()).isInstanceOf(java.time.LocalDateTime.class);
    }

    @Test
    void sign_transitions_from_draft_to_signed() {
        when(repository.findById(1L)).thenReturn(Optional.of(draft));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        SignE14Request req = new SignE14Request("signer-1");
        E14Record res = service.sign(1L, req);

        assertThat(res.getStatus()).isEqualTo(E14Status.SIGNED);
    }

    @Test
    void sign_preserves_mesa_code_and_pdf_hash() {
        when(repository.findById(1L)).thenReturn(Optional.of(draft));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        SignE14Request req = new SignE14Request("signer-2");
        E14Record res = service.sign(1L, req);

        assertThat(res.getMesaCode()).isEqualTo("M-200");
        assertThat(res.getPdfHash()).isEqualTo("abc");
    }

    @Test
    void sign_saves_updated_record() {
        when(repository.findById(1L)).thenReturn(Optional.of(draft));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        SignE14Request req = new SignE14Request("signer-3");
        E14Record res = service.sign(1L, req);

        verify(repository, times(1)).save(any(E14Record.class));
        assertThat(res.getStatus()).isEqualTo(E14Status.SIGNED);
    }

    @Test
    void sign_non_draft_throws() {
        E14Record signed = E14Record.builder().id(2L).status(E14Status.SIGNED).build();
        when(repository.findById(2L)).thenReturn(Optional.of(signed));

        assertThatThrownBy(() -> service.sign(2L, new SignE14Request("s")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only draft records can be signed");
    }

    @Test
    void publish_signed_and_not_quarantined_succeeds() throws Exception {
        E14Record signed = E14Record.builder().id(3L).mesaCode("M-300").status(E14Status.SIGNED).build();
        when(repository.findById(3L)).thenReturn(Optional.of(signed));
        when(disputeGrpcClient.getQuarantinedMesaCodes()).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        CompletableFuture<E14Record> fut = service.publish(3L);
        E14Record res = fut.get();

        assertThat(res.getStatus()).isEqualTo(E14Status.PUBLISHED);
    }

    @Test
    void publish_calls_dispute_service_and_saves_record() throws Exception {
        E14Record signed = E14Record.builder().id(6L).mesaCode("M-600").status(E14Status.SIGNED).build();
        when(repository.findById(6L)).thenReturn(Optional.of(signed));
        when(disputeGrpcClient.getQuarantinedMesaCodes()).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        CompletableFuture<E14Record> fut = service.publish(6L);
        E14Record res = fut.get();

        verify(disputeGrpcClient, times(1)).getQuarantinedMesaCodes();
        verify(repository, times(1)).save(any(E14Record.class));
        assertThat(res.getStatus()).isEqualTo(E14Status.PUBLISHED);
    }

    @Test
    void sign_throws_when_record_already_published() {
        E14Record published = E14Record.builder().id(7L).status(E14Status.PUBLISHED).build();
        when(repository.findById(7L)).thenReturn(Optional.of(published));

        assertThatThrownBy(() -> service.sign(7L, new SignE14Request("signer")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only draft records can be signed");
    }

    @Test
    void publish_throws_if_already_published() {
        E14Record published = E14Record.builder().id(8L).mesaCode("M-800").status(E14Status.PUBLISHED).build();
        when(repository.findById(8L)).thenReturn(Optional.of(published));

        assertThatThrownBy(() -> service.publish(8L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only signed records can be published");
    }

    @Test
    void publish_throws_if_quarantined() {
        E14Record signed = E14Record.builder().id(4L).mesaCode("M-400").status(E14Status.SIGNED).build();
        when(repository.findById(4L)).thenReturn(Optional.of(signed));
        when(disputeGrpcClient.getQuarantinedMesaCodes()).thenReturn(List.of("M-400"));

        assertThatThrownBy(() -> service.publish(4L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Mesa is quarantined");

        verify(repository, never()).save(any(E14Record.class));
    }

    @Test
    void publish_throws_if_not_signed() {
        E14Record draft = E14Record.builder().id(5L).status(E14Status.DRAFT).build();
        when(repository.findById(5L)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.publish(5L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only signed records can be published");
    }

    @Test
    void sign_not_found_throws() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sign(99L, new SignE14Request("s")))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void publish_not_found_throws() {
        when(repository.findById(98L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publish(98L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void create_returns_saved_record_from_repository() {
        E14Record saved = E14Record.builder()
                .id(20L)
                .mesaCode("M-250")
                .municipality("Pereira")
                .pdfHash("hash-250")
                .status(E14Status.DRAFT)
                .build();

        when(repository.save(any())).thenReturn(saved);

        CreateE14Request request = new CreateE14Request("M-250", "Pereira", "hash-250");
        E14Record result = service.create(request);

        assertThat(result).isEqualTo(saved);
    }

    @Test
    void sign_saves_signed_record() {
        when(repository.findById(1L)).thenReturn(Optional.of(draft));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        SignE14Request req = new SignE14Request("signer-xyz");
        E14Record res = service.sign(1L, req);

        verify(repository, times(1)).save(any(E14Record.class));
        assertThat(res.getStatus()).isEqualTo(E14Status.SIGNED);
    }

    @Test
    void publish_completes_future_after_success() throws Exception {
        E14Record signed = E14Record.builder().id(9L).mesaCode("M-900").status(E14Status.SIGNED).build();
        when(repository.findById(9L)).thenReturn(Optional.of(signed));
        when(disputeGrpcClient.getQuarantinedMesaCodes()).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        CompletableFuture<E14Record> future = service.publish(9L);
        assertThat(future).isNotNull();
        assertThat(future.isDone()).isTrue();
        assertThat(future.get().getStatus()).isEqualTo(E14Status.PUBLISHED);
    }

    @Test
    void publish_throws_when_dispute_service_fails() {
        E14Record signed = E14Record.builder().id(10L).mesaCode("M-1000").status(E14Status.SIGNED).build();
        when(repository.findById(10L)).thenReturn(Optional.of(signed));
        when(disputeGrpcClient.getQuarantinedMesaCodes()).thenThrow(new RuntimeException("Grpc failed"));

        assertThatThrownBy(() -> service.publish(10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Grpc failed");
    }
}
