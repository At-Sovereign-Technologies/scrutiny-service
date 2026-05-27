package com.registraduria.scrutiny_service.official.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.registraduria.scrutiny_service.candidate.dto.CandidateAggregateSummary;
import com.registraduria.scrutiny_service.candidate.repository.CandidateVoteRepository;
import com.registraduria.scrutiny_service.official.dto.CloseGeneralScrutinyRequest;
import com.registraduria.scrutiny_service.official.dto.GeneralScrutinyResponse;
import com.registraduria.scrutiny_service.official.dto.MagistrateApprovalResponse;
import com.registraduria.scrutiny_service.official.entity.ActaE26;
import com.registraduria.scrutiny_service.official.entity.GeneralScrutiny;
import com.registraduria.scrutiny_service.official.entity.MagistrateApproval;
import com.registraduria.scrutiny_service.official.enums.GeneralScrutinyStatus;
import com.registraduria.scrutiny_service.official.exception.InvalidScrutinyStateException;
import com.registraduria.scrutiny_service.official.exception.ResourceNotFoundException;
import com.registraduria.scrutiny_service.official.repository.ActaE26Repository;
import com.registraduria.scrutiny_service.official.repository.GeneralScrutinyRepository;
import com.registraduria.scrutiny_service.official.repository.MagistrateApprovalRepository;
import com.registraduria.scrutiny_service.official.security.MagistrateIdentity;
import com.registraduria.scrutiny_service.portal.service.PortalService;

import lombok.RequiredArgsConstructor;

/**
 * Orquesta la aprobacion del escrutinio general por los magistrados y la
 * generacion del Acta E-26 al alcanzarse el quorum (CA-1, CA-2).
 */
@Service
@RequiredArgsConstructor
public class GeneralScrutinyService {

    private static final Logger log =
            LoggerFactory.getLogger(GeneralScrutinyService.class);

    private final GeneralScrutinyRepository scrutinyRepository;
    private final MagistrateApprovalRepository approvalRepository;
    private final ActaE26Repository actaRepository;
    private final CandidateVoteRepository candidateVoteRepository;
    private final MagistrateSignatureService signatureService;
    private final ActaGenerationService actaGenerationService;
    private final PortalService portalService;

    /**
     * Cierra el escrutinio general (handoff desde SR-M4): congela el estado
     * agregado, calcula su hash y lo deja CERRADO, listo para aprobacion.
     */
    @Transactional
    public GeneralScrutinyResponse close(CloseGeneralScrutinyRequest request) {

        if (scrutinyRepository.existsByScrutinyCode(request.scrutinyCode())) {
            throw new InvalidScrutinyStateException(
                    "El escrutinio " + request.scrutinyCode() + " ya existe.");
        }

        String method = (request.electoralMethod() == null
                || request.electoralMethod().isBlank())
                ? DHondtAllocationService.METHOD
                : request.electoralMethod();

        List<CandidateAggregateSummary> aggregates =
                candidateVoteRepository.aggregateAllCandidates();

        String stateHash = computeStateHash(
                request.scrutinyCode(), method, request.seats(), aggregates);

        GeneralScrutiny scrutiny = GeneralScrutiny.builder()
                .scrutinyCode(request.scrutinyCode())
                .status(GeneralScrutinyStatus.CERRADO)
                .electoralMethod(method)
                .seats(request.seats())
                .requiredQuorum(request.requiredQuorum())
                .stateHash(stateHash)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        scrutiny = scrutinyRepository.save(scrutiny);

        // CA-4: el portal arranca en estado preliminar.
        portalService.markPreliminary(request.scrutinyCode());

        return toResponse(scrutiny, 0);
    }

    /**
     * Registra la aprobacion de un magistrado (CA-1) y, si se alcanza el quorum,
     * genera el Acta E-26 (CA-2).
     */
    @Transactional
    public MagistrateApprovalResponse approve(
            String scrutinyCode,
            MagistrateIdentity magistrate,
            String clientIp
    ) {

        GeneralScrutiny scrutiny = scrutinyRepository
                .findByScrutinyCode(scrutinyCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Escrutinio no encontrado: " + scrutinyCode));

        // CA-1: solo se aprueba un escrutinio CERRADO (o ya EN_APROBACION).
        if (scrutiny.getStatus() == GeneralScrutinyStatus.OFICIAL) {
            throw new InvalidScrutinyStateException(
                    "El escrutinio ya es OFICIAL; no admite nuevas aprobaciones.");
        }

        // Un magistrado no puede aprobar dos veces el mismo escrutinio.
        if (approvalRepository.existsByGeneralScrutinyIdAndMagistrateId(
                scrutiny.getId(), magistrate.magistrateId())) {
            throw new InvalidScrutinyStateException(
                    "El magistrado " + magistrate.magistrateId()
                            + " ya aprobo este escrutinio.");
        }

        LocalDateTime approvedAtUtc = LocalDateTime.now(ZoneOffset.UTC);

        // CA-2: firma digital del magistrado sobre el hash del estado aprobado.
        String signature = signatureService.sign(
                magistrate.magistrateId(),
                scrutiny.getStateHash(),
                approvedAtUtc.toString());

        // CA-1: auditoria append-only (quien, cuando UTC, desde donde, que estado).
        MagistrateApproval approval = MagistrateApproval.builder()
                .generalScrutinyId(scrutiny.getId())
                .magistrateId(magistrate.magistrateId())
                .magistrateName(magistrate.magistrateName())
                .approvedStateHash(scrutiny.getStateHash())
                .signature(signature)
                .ipAddress(clientIp)
                .approvedAtUtc(approvedAtUtc)
                .createdAt(approvedAtUtc)
                .build();

        approval = approvalRepository.save(approval);

        log.info("Aprobacion registrada: escrutinio={} magistrado={} ip={} hashEstado={}",
                scrutinyCode, magistrate.magistrateId(), clientIp, scrutiny.getStateHash());

        if (scrutiny.getStatus() == GeneralScrutinyStatus.CERRADO) {
            scrutiny.setStatus(GeneralScrutinyStatus.EN_APROBACION);
        }

        long approvalsCount = approvalRepository.countByGeneralScrutinyId(scrutiny.getId());
        boolean quorumReached = approvalsCount >= scrutiny.getRequiredQuorum();

        String actaNumber = null;
        if (quorumReached && !actaRepository.existsByGeneralScrutinyId(scrutiny.getId())) {

            ActaE26 acta = actaGenerationService.generate(scrutiny);
            actaNumber = acta.getActaNumber();

            scrutiny.setStatus(GeneralScrutinyStatus.OFICIAL);
            scrutiny.setOfficializedAt(LocalDateTime.now(ZoneOffset.UTC));

            log.info("Quorum alcanzado ({}/{}): Acta E-26 {} generada para escrutinio {}",
                    approvalsCount, scrutiny.getRequiredQuorum(), actaNumber, scrutinyCode);
        }

        scrutinyRepository.save(scrutiny);

        return new MagistrateApprovalResponse(
                approval.getId(),
                scrutinyCode,
                magistrate.magistrateId(),
                magistrate.magistrateName(),
                approvedAtUtc,
                approvalsCount,
                scrutiny.getRequiredQuorum(),
                quorumReached,
                scrutiny.getStatus(),
                actaNumber
        );
    }

    @Transactional(readOnly = true)
    public GeneralScrutinyResponse getByCode(String scrutinyCode) {
        GeneralScrutiny scrutiny = scrutinyRepository
                .findByScrutinyCode(scrutinyCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Escrutinio no encontrado: " + scrutinyCode));
        long count = approvalRepository.countByGeneralScrutinyId(scrutiny.getId());
        return toResponse(scrutiny, count);
    }

    /**
     * Hash determinista del estado agregado, ordenando los candidatos por id
     * para que el mismo conjunto de votos produzca siempre el mismo hash (CA-1).
     */
    private String computeStateHash(
            String scrutinyCode,
            String method,
            int seats,
            List<CandidateAggregateSummary> aggregates
    ) {
        String canonical = scrutinyCode + "|" + method + "|" + seats + "|"
                + aggregates.stream()
                        .sorted(Comparator.comparing(CandidateAggregateSummary::candidateId))
                        .map(a -> a.candidateId() + ":"
                                + (a.totalVotes() == null ? 0L : a.totalVotes()))
                        .collect(Collectors.joining(","));
        return Sha256Util.hexDigest(canonical);
    }

    private GeneralScrutinyResponse toResponse(GeneralScrutiny s, long approvalsCount) {
        return new GeneralScrutinyResponse(
                s.getId(),
                s.getScrutinyCode(),
                s.getStatus(),
                s.getElectoralMethod(),
                s.getSeats(),
                s.getRequiredQuorum(),
                approvalsCount,
                s.getStateHash(),
                s.getCreatedAt(),
                s.getOfficializedAt()
        );
    }
}
