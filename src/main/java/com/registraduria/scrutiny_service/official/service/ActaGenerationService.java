package com.registraduria.scrutiny_service.official.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.registraduria.scrutiny_service.candidate.dto.CandidateAggregateSummary;
import com.registraduria.scrutiny_service.candidate.repository.CandidateVoteRepository;
import com.registraduria.scrutiny_service.events.OfficialResultsPublishedEvent;
import com.registraduria.scrutiny_service.events.producer.OfficialResultsEventProducer;
import com.registraduria.scrutiny_service.official.dto.E26ActaContent;
import com.registraduria.scrutiny_service.official.dto.MagistrateSignatureDto;
import com.registraduria.scrutiny_service.official.entity.ActaE26;
import com.registraduria.scrutiny_service.official.entity.GeneralScrutiny;
import com.registraduria.scrutiny_service.official.entity.MagistrateApproval;
import com.registraduria.scrutiny_service.official.enums.ActaStatus;
import com.registraduria.scrutiny_service.official.repository.ActaE26Repository;
import com.registraduria.scrutiny_service.official.repository.MagistrateApprovalRepository;
import com.registraduria.scrutiny_service.portal.service.PortalService;

import lombok.RequiredArgsConstructor;

/**
 * Genera el Acta E-26 oficial e inmutable al alcanzarse el quorum (CA-2):
 * aplica el metodo electoral, construye el contenido canonico, calcula el hash
 * de integridad, emite PDF/A-3 y XML firmado, persiste el acta (Write-Once),
 * actualiza el portal (CA-4) y publica el evento de dominio.
 */
@Service
@RequiredArgsConstructor
public class ActaGenerationService {

    private final CandidateVoteRepository candidateVoteRepository;
    private final MagistrateApprovalRepository approvalRepository;
    private final ActaE26Repository actaRepository;
    private final DHondtAllocationService allocationService;
    private final E26PdfService pdfService;
    private final E26XmlSignatureService xmlSignatureService;
    private final PortalService portalService;
    private final OfficialResultsEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    @Value("${scrutiny.acta.output-dir}")
    private String outputDir;

    @Value("${scrutiny.acta.base-url}")
    private String baseUrl;

    @Transactional
    public ActaE26 generate(GeneralScrutiny scrutiny) {

        List<CandidateAggregateSummary> aggregates =
                candidateVoteRepository.aggregateAllCandidates();

        SeatAllocation allocation =
                allocationService.allocate(aggregates, scrutiny.getSeats());

        List<MagistrateSignatureDto> signatures =
                buildSignatures(scrutiny.getId());

        String generatedAtUtc = LocalDateTime.now(ZoneOffset.UTC).toString();

        E26ActaContent content = new E26ActaContent(
                scrutiny.getScrutinyCode(),
                scrutiny.getId(),
                scrutiny.getElectoralMethod(),
                allocation.totalValidVotes(),
                scrutiny.getSeats(),
                allocation.partyResults(),
                allocation.electedCandidates(),
                signatures,
                generatedAtUtc
        );

        // El hash de integridad se calcula sobre la serializacion JSON canonica
        // y se reverifica en cada lectura (CA-3).
        String contentJson = serialize(content);
        String contentHash = Sha256Util.hexDigest(contentJson);

        String actaNumber = nextActaNumber();

        String pdfPath = pdfService.generate(actaNumber, content, contentHash);
        String xmlPath = xmlSignatureService.generate(
                actaNumber, content, contentHash, outputDir);

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        ActaE26 acta = ActaE26.builder()
                .actaNumber(actaNumber)
                .generalScrutinyId(scrutiny.getId())
                .sourceScrutinyCode(scrutiny.getScrutinyCode())
                .electoralMethod(scrutiny.getElectoralMethod())
                .status(ActaStatus.OFICIAL_INMUTABLE)
                .contentJson(contentJson)
                .contentHash(contentHash)
                .pdfPath(pdfPath)
                .xmlPath(xmlPath)
                .generatedAt(now)
                .createdAt(now)
                .build();

        // Insercion unica: a partir de aqui el acta es inmutable (CA-3).
        ActaE26 saved = actaRepository.save(acta);

        String permalink = baseUrl + "/" + saved.getId();

        // CA-4: el portal pasa de PRELIMINAR a RESULTADOS OFICIALES + permalink.
        portalService.publishOfficial(scrutiny.getScrutinyCode(), permalink);

        // CA-4: notificar a otros microservicios.
        eventProducer.publish(new OfficialResultsPublishedEvent(
                scrutiny.getScrutinyCode(),
                saved.getActaNumber(),
                saved.getContentHash(),
                permalink,
                scrutiny.getElectoralMethod(),
                generatedAtUtc
        ));

        return saved;
    }

    private List<MagistrateSignatureDto> buildSignatures(Long scrutinyId) {
        return approvalRepository
                .findByGeneralScrutinyIdOrderByApprovedAtUtcAsc(scrutinyId)
                .stream()
                .map(this::toSignatureDto)
                .toList();
    }

    private MagistrateSignatureDto toSignatureDto(MagistrateApproval a) {
        return new MagistrateSignatureDto(
                a.getMagistrateId(),
                a.getMagistrateName(),
                a.getSignature(),
                a.getApprovedAtUtc().toString(),
                MagistrateSignatureService.SCHEME
        );
    }

    /** Numeracion oficial secuencial; las actas son append-only (nunca se borran). */
    private String nextActaNumber() {
        long next = actaRepository.count() + 1;
        return String.format("E26-%06d", next);
    }

    private String serialize(E26ActaContent content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (Exception e) {
            throw new IllegalStateException("Error serializando el acta.", e);
        }
    }
}
