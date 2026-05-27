package com.registraduria.scrutiny_service.official.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.registraduria.scrutiny_service.official.dto.ActaE26Response;
import com.registraduria.scrutiny_service.official.dto.E26ActaContent;
import com.registraduria.scrutiny_service.official.enums.ActaStatus;
import com.registraduria.scrutiny_service.official.service.ActaDownload;
import com.registraduria.scrutiny_service.official.service.ActaQueryService;

/**
 * CA-3: la lectura del acta funciona, pero todo intento de modificacion o
 * borrado responde 403 FORBIDDEN sin tocar la capa de servicio.
 */
@WebMvcTest(ActaE26Controller.class)
class ActaE26ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActaQueryService service;

    private ActaE26Response sampleActa() {
        E26ActaContent content = new E26ActaContent(
                "NAC-2026", 1L, "DHONDT", 100L, 1,
                List.of(), List.of(), List.of(), "2026-05-27T00:00:00Z");
        return new ActaE26Response(
                1L, "E26-000001", "NAC-2026", ActaStatus.OFICIAL_INMUTABLE,
                "DHONDT", "hash", true, "p.pdf", "p.xml",
                "http://localhost/api/v1/actas/1", LocalDateTime.now(), content);
    }

    @Test
    void get_returns_acta_with_verified_hash() throws Exception {
        when(service.getById(1L)).thenReturn(sampleActa());

        mockMvc.perform(get("/api/v1/actas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actaNumber").value("E26-000001"))
                .andExpect(jsonPath("$.status").value("OFICIAL_INMUTABLE"))
                .andExpect(jsonPath("$.hashVerified").value(true));
    }

    @Test
    void pdf_is_served_as_application_pdf() throws Exception {
        when(service.downloadPdf(1L))
                .thenReturn(new ActaDownload("E26-000001.pdf", new byte[]{1, 2, 3}));

        mockMvc.perform(get("/api/v1/actas/1/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "inline; filename=\"E26-000001.pdf\""));
    }

    @Test
    void xml_is_served_as_application_xml_attachment() throws Exception {
        when(service.downloadXml(1L))
                .thenReturn(new ActaDownload("E26-000001.xml", "<x/>".getBytes()));

        mockMvc.perform(get("/api/v1/actas/1/xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_XML))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"E26-000001.xml\""));
    }

    @Test
    void put_is_forbidden() throws Exception {
        mockMvc.perform(put("/api/v1/actas/1"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void patch_is_forbidden() throws Exception {
        mockMvc.perform(patch("/api/v1/actas/1"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void delete_is_forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/actas/1"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
