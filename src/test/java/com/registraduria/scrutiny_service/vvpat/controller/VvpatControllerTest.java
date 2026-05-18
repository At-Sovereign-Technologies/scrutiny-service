package com.registraduria.scrutiny_service.vvpat.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.registraduria.scrutiny_service.vvpat.dto.VvpatScanRequest;
import com.registraduria.scrutiny_service.vvpat.entity.VvpatScanRecord;
import com.registraduria.scrutiny_service.vvpat.enums.VvpatResult;
import com.registraduria.scrutiny_service.vvpat.service.VvpatService;

@WebMvcTest(VvpatController.class)
class VvpatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VvpatService service;

    @Test
    void scan_returns_created_and_response_body() throws Exception {
        VvpatScanRequest request = new VvpatScanRequest("M-100", "J-1", 50);
        VvpatScanRecord response = VvpatScanRecord.builder()
                .id(1L)
                .mesaCode("M-100")
                .juradoId("J-1")
                .physicalVotes(50)
                .result(VvpatResult.MATCH)
                .attempt(1)
                .build();

        when(service.scan(any(VvpatScanRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/vvpat/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));
    }

    @Test
    void scan_returns_internal_server_error_when_service_throws() throws Exception {
        when(service.scan(any(VvpatScanRequest.class)))
                .thenThrow(new RuntimeException("Dispute processing failed"));

        String validPayload = "{\"mesaCode\": \"M-100\", \"juradoId\": \"J-1\", \"physicalVotes\": 50}";

        assertThatThrownBy(() -> mockMvc.perform(post("/api/v1/vvpat/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validPayload))
                .andReturn())
                .hasRootCauseMessage("Dispute processing failed");
    }

    @Test
    void scan_returns_bad_request_when_jurado_id_is_blank() throws Exception {
        String invalidPayload = "{\"mesaCode\": \"M-100\", \"juradoId\": \"\", \"physicalVotes\": 50}";

        mockMvc.perform(post("/api/v1/vvpat/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void scan_returns_bad_request_when_mesa_code_is_blank() throws Exception {
        String invalidPayload = "{\"mesaCode\": \"\", \"juradoId\": \"J-1\", \"physicalVotes\": 50}";

        mockMvc.perform(post("/api/v1/vvpat/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void scan_returns_bad_request_when_physical_votes_is_null() throws Exception {
        String invalidPayload = "{\"mesaCode\": \"M-100\", \"juradoId\": \"J-1\", \"physicalVotes\": null}";

        mockMvc.perform(post("/api/v1/vvpat/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void scan_returns_bad_request_when_input_is_invalid() throws Exception {
        String invalidPayload = "{\"mesaCode\": \"\", \"juradoId\": \"J-1\"}";

        mockMvc.perform(post("/api/v1/vvpat/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void scan_returns_bad_request_when_jurado_id_is_missing() throws Exception {
        String invalidPayload = "{\"mesaCode\": \"M-100\", \"physicalVotes\": 50}";

        mockMvc.perform(post("/api/v1/vvpat/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void scan_returns_bad_request_when_payload_is_empty() throws Exception {
        String invalidPayload = "{}";

        mockMvc.perform(post("/api/v1/vvpat/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void scan_returns_bad_request_when_invalid_json_is_sent() throws Exception {
        String invalidPayload = "{ mesaCode: 'M-100', juradoId: 'J-1' physicalVotes: 50 }";

        mockMvc.perform(post("/api/v1/vvpat/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }
}
