package com.registraduria.scrutiny_service.official;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.registraduria.scrutiny_service.AbstractIntegrationTest;
import com.registraduria.scrutiny_service.candidate.entity.CandidateVoteRecord;
import com.registraduria.scrutiny_service.candidate.repository.CandidateVoteRepository;
import com.registraduria.scrutiny_service.official.entity.ActaE26;
import com.registraduria.scrutiny_service.official.enums.ActaStatus;
import com.registraduria.scrutiny_service.official.repository.ActaE26Repository;

/**
 * Flujo completo SR-M5: cierre del escrutinio general, aprobacion por quorum de
 * magistrados (CA-1), autogeneracion del Acta E-26 (CA-2), inmutabilidad y
 * reverificacion de hash (CA-3), y publicacion en el portal (CA-4).
 *
 * Requiere Docker (Postgres + Kafka via Testcontainers); se omite si no esta.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OfficialResultsFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired CandidateVoteRepository candidateVoteRepository;
    @Autowired ActaE26Repository actaRepository;
    @Autowired ObjectMapper objectMapper;

    @Value("${scrutiny.security.magistrate-jwt-secret}")
    String jwtSecret;

    private void seedVotes() {
        candidateVoteRepository.saveAll(List.of(
                CandidateVoteRecord.builder().mesaCode("M1").candidateId("A1")
                        .candidateName("Ana").party("A").votes(60).build(),
                CandidateVoteRecord.builder().mesaCode("M1").candidateId("A2")
                        .candidateName("Aldo").party("A").votes(45).build(),
                CandidateVoteRecord.builder().mesaCode("M1").candidateId("B1")
                        .candidateName("Beto").party("B").votes(80).build(),
                CandidateVoteRecord.builder().mesaCode("M1").candidateId("C1")
                        .candidateName("Cira").party("C").votes(30).build()
        ));
    }

    @Test
    void full_approval_flow_generates_immutable_acta_and_publishes_portal() throws Exception {
        seedVotes();

        String code = "NAC-FLOW-" + System.nanoTime();
        String tokenMag1 = TestJwt.magistrate(jwtSecret, "MAG-1", "Magistrada Uno");
        String tokenMag2 = TestJwt.magistrate(jwtSecret, "MAG-2", "Magistrado Dos");

        // 1) Cierre del escrutinio general (CERRADO), quorum = 2, 3 curules.
        String closeBody = """
                {"scrutinyCode":"%s","requiredQuorum":2,"seats":3,"electoralMethod":"DHONDT"}
                """.formatted(code);

        mockMvc.perform(post("/api/v1/general-scrutiny/close")
                        .header("Authorization", "Bearer " + tokenMag1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(closeBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CERRADO"));

        // Portal arranca PRELIMINAR.
        mockMvc.perform(get("/api/v1/portal/results/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.official").value(false))
                .andExpect(jsonPath("$.badgeText").value("PRELIMINAR"));

        // 2) Primera aprobacion: sin quorum -> EN_APROBACION.
        mockMvc.perform(post("/api/v1/general-scrutiny/" + code + "/approve")
                        .header("Authorization", "Bearer " + tokenMag1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quorumReached").value(false))
                .andExpect(jsonPath("$.scrutinyStatus").value("EN_APROBACION"));

        // 3) Segunda aprobacion: se alcanza quorum -> OFICIAL + Acta E-26.
        MvcResult approveResult = mockMvc.perform(
                        post("/api/v1/general-scrutiny/" + code + "/approve")
                                .header("Authorization", "Bearer " + tokenMag2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quorumReached").value(true))
                .andExpect(jsonPath("$.scrutinyStatus").value("OFICIAL"))
                .andExpect(jsonPath("$.actaNumber").isNotEmpty())
                .andReturn();

        JsonNode approveJson =
                objectMapper.readTree(approveResult.getResponse().getContentAsString());
        String actaNumber = approveJson.get("actaNumber").asText();

        ActaE26 acta = actaRepository.findByActaNumber(actaNumber).orElseThrow();
        assertThat(acta.getStatus()).isEqualTo(ActaStatus.OFICIAL_INMUTABLE);

        // 4) Lectura del acta: hash reverificado (CA-3) y resultados presentes.
        mockMvc.perform(get("/api/v1/actas/" + acta.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OFICIAL_INMUTABLE"))
                .andExpect(jsonPath("$.hashVerified").value(true))
                .andExpect(jsonPath("$.content.totalValidVotes").value(215))
                .andExpect(jsonPath("$.content.electedCandidates.length()").value(3))
                .andExpect(jsonPath("$.content.magistrateSignatures.length()").value(2));

        // 5) Documentos: PDF/A-3 y XML firmado descargables.
        mockMvc.perform(get("/api/v1/actas/" + acta.getId() + "/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        byte[] xml = mockMvc.perform(get("/api/v1/actas/" + acta.getId() + "/xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_XML))
                .andReturn().getResponse().getContentAsByteArray();
        // El XML lleva la firma XML-DSig enveloped.
        assertThat(new String(xml)).contains("Signature");

        // 6) Inmutabilidad: cualquier intento de borrado -> 403.
        mockMvc.perform(delete("/api/v1/actas/" + acta.getId()))
                .andExpect(status().isForbidden());

        // 7) Portal ya OFICIAL con badge verde y permalink.
        mockMvc.perform(get("/api/v1/portal/results/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.official").value(true))
                .andExpect(jsonPath("$.badgeText").value("RESULTADOS OFICIALES"))
                .andExpect(jsonPath("$.badgeColorHex").value("#1a7f37"))
                .andExpect(jsonPath("$.actaPermalink").isNotEmpty());
    }

    @Test
    void approve_without_token_is_unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/general-scrutiny/ANY/approve"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void approve_with_wrong_role_is_forbidden() throws Exception {
        String token = TestJwt.sign(jwtSecret, java.util.Map.of(
                "sub", "USER-1", "role", "OPERADOR"));

        mockMvc.perform(post("/api/v1/general-scrutiny/ANY/approve")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
