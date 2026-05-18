package com.registraduria.scrutiny_service.command.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.registraduria.scrutiny_service.command.dto.CreateE14Request;
import com.registraduria.scrutiny_service.command.dto.SignE14Request;
import com.registraduria.scrutiny_service.command.service.E14CommandService;
import com.registraduria.scrutiny_service.domain.entity.E14Record;
import com.registraduria.scrutiny_service.domain.enums.E14Status;

@WebMvcTest(E14CommandController.class)
class E14CommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private E14CommandService service;

    @Test
    void create_returns_created_record() throws Exception {
        CreateE14Request request = new CreateE14Request("M-500", "City", "hash-123");
        E14Record response = E14Record.builder()
                .id(1L)
                .mesaCode("M-500")
                .municipality("City")
                .pdfHash("hash-123")
                .status(E14Status.DRAFT)
                .build();

        when(service.create(any(CreateE14Request.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/e14")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));
    }

    @Test
    void sign_returns_signed_record() throws Exception {
        SignE14Request request = new SignE14Request("signer-1");
        E14Record response = E14Record.builder()
                .id(2L)
                .mesaCode("M-500")
                .status(E14Status.SIGNED)
                .build();

        when(service.sign(eq(2L), any(SignE14Request.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/e14/2/sign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));
    }

    @Test
    void publish_returns_published_record_future() throws Exception {
        E14Record response = E14Record.builder()
                .id(3L)
                .mesaCode("M-500")
                .status(E14Status.PUBLISHED)
                .build();

        when(service.publish(3L)).thenReturn(CompletableFuture.completedFuture(response));

        var result = mockMvc.perform(patch("/api/v1/e14/3/publish")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));
    }

    @Test
    void sign_returns_bad_request_for_invalid_payload() throws Exception {
        String invalidJson = "{\"digitalSignature\": \"\"}";

        mockMvc.perform(patch("/api/v1/e14/2/sign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sign_propagates_exception_when_record_not_found() throws Exception {
        SignE14Request request = new SignE14Request("signer-1");
        when(service.sign(eq(99L), any(SignE14Request.class)))
                .thenThrow(new RuntimeException("E14 record not found"));

        assertThatThrownBy(() -> mockMvc.perform(patch("/api/v1/e14/99/sign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andReturn())
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("E14 record not found");
    }

    @Test
    void publish_propagates_exception_when_quarantined() throws Exception {
        when(service.publish(4L)).thenThrow(new RuntimeException("Mesa is quarantined"));

        assertThatThrownBy(() -> mockMvc.perform(patch("/api/v1/e14/4/publish")
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn())
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Mesa is quarantined");
    }

    @Test
    void publish_propagates_exception_when_record_not_found() throws Exception {
        when(service.publish(99L)).thenThrow(new RuntimeException("E14 record not found"));

        assertThatThrownBy(() -> mockMvc.perform(patch("/api/v1/e14/99/publish")
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn())
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("E14 record not found");
    }

    @Test
    void create_returns_bad_request_for_invalid_payload() throws Exception {
        String invalidJson = "{\"mesaCode\": \"\", \"municipality\": \"City\"}";

        mockMvc.perform(post("/api/v1/e14")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns_bad_request_when_municipality_is_missing() throws Exception {
        String invalidJson = "{\"mesaCode\": \"M-500\", \"pdfHash\": \"hash-123\"}";

        mockMvc.perform(post("/api/v1/e14")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
    @Test
    void create_returns_bad_request_when_pdf_hash_is_missing() throws Exception {
        String invalidJson = "{\"mesaCode\": \"M-500\", \"municipality\": \"City\"}";

        mockMvc.perform(post("/api/v1/e14")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publish_returns_internal_server_error_when_service_throws() throws Exception {
        when(service.publish(5L)).thenThrow(new RuntimeException("Unexpected publish failure"));

        assertThatThrownBy(() -> mockMvc.perform(patch("/api/v1/e14/5/publish")
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn())
                .hasRootCauseMessage("Unexpected publish failure");
    }
}
