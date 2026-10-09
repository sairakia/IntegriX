package kopo.integrix.service.impl;

import kopo.integrix.dto.mongo.AnalysisResultDTO;
import kopo.integrix.dto.url.UrlAnalysisResponseDTO;
import kopo.integrix.repository.ReportFeedbackRepository;
import kopo.integrix.repository.mongo.AnalysisDetailRepository;
import kopo.integrix.repository.mongo.AnalysisResultRepository;
import kopo.integrix.service.RdapService;
import kopo.integrix.service.SafeBrowsingService;
import kopo.integrix.service.UrlScanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UrlAnalysisServiceImplTest {

    private SafeBrowsingService safeBrowsingService;
    private UrlScanService urlScanService;
    private RdapService rdapService;
    private AnalysisResultRepository analysisResultRepository;
    private AnalysisDetailRepository analysisDetailRepository;
    private ReportFeedbackRepository reportFeedbackRepository;
    private UrlAnalysisServiceImpl urlAnalysisService;

    @BeforeEach
    void setUp() throws Exception {
        rdapService = mock(RdapService.class);
        safeBrowsingService = mock(SafeBrowsingService.class);
        urlScanService = mock(UrlScanService.class);
        analysisResultRepository = mock(AnalysisResultRepository.class);
        analysisDetailRepository = mock(AnalysisDetailRepository.class);
        reportFeedbackRepository = mock(ReportFeedbackRepository.class);
        when(rdapService.lookupDomain(any())).thenReturn(Optional.empty());
        when(urlScanService.lookupUrl(any())).thenReturn(UrlScanService.UrlScanLookupResult.noResult());
        urlAnalysisService = new UrlAnalysisServiceImpl(
                rdapService,
                safeBrowsingService,
                urlScanService,
                analysisResultRepository,
                analysisDetailRepository,
                reportFeedbackRepository
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:8080",
            "http://test.localhost",
            "http://127.0.0.1",
            "http://0.0.0.0",
            "http://10.0.0.1",
            "http://172.16.0.1",
            "http://192.168.0.1",
            "http://169.254.169.254",
            "http://[::1]",
            "file:///etc/passwd",
            "1",
            "abc",
            "https://login",
            "https://example",
            "https://-example.com",
            "https://example-.com",
            "https://example.1"
    })
    void analyzeUrlBlocksInternalAndUnsupportedUrls(String url) {
        assertThrows(IllegalArgumentException.class, () -> urlAnalysisService.analyzeUrl(url, "user01"));

        verifyNoInteractions(safeBrowsingService);
        verifyNoInteractions(urlScanService);
        verifyNoInteractions(rdapService);
        verifyNoInteractions(analysisResultRepository);
        verifyNoInteractions(analysisDetailRepository);
    }

    @Test
    void analyzeUrlContinuesWhenDomainCannotBeResolved() throws Exception {
        when(safeBrowsingService.isUnsafeUrl(any())).thenReturn(false);
        when(analysisResultRepository.save(any(AnalysisResultDTO.class)))
                .thenAnswer(invocation -> savedResultWithId(invocation.getArgument(0)));

        UrlAnalysisResponseDTO result = urlAnalysisService.analyzeUrl("https://not-found-domain.invalid", "user01");

        assertEquals("https://not-found-domain.invalid", result.url());
        assertEquals("caution", result.trustLevel());
        assertEquals(15, result.riskScore());
        assertEquals(1, result.scoreFactors().size());
        assertEquals("DNS 조회 실패", result.scoreFactors().get(0).label());
        assertEquals(15, result.scoreFactors().get(0).score());
        assertFalse(result.analysis().httpsConnection().isEmpty());
        assertFalse(result.analysis().sslCertificate().isEmpty());

        verify(safeBrowsingService).isUnsafeUrl("https://not-found-domain.invalid");
        verify(urlScanService).lookupUrl("https://not-found-domain.invalid");
        verify(analysisResultRepository).save(any(AnalysisResultDTO.class));
        verify(analysisDetailRepository).saveAll(any());
    }

    @Test
    void analyzeUrlRemovesDuplicatedHttpScheme() throws Exception {
        when(safeBrowsingService.isUnsafeUrl(any())).thenReturn(false);
        when(analysisResultRepository.save(any(AnalysisResultDTO.class)))
                .thenAnswer(invocation -> savedResultWithId(invocation.getArgument(0)));

        UrlAnalysisResponseDTO result = urlAnalysisService.analyzeUrl("https://https://not-found-domain.invalid", "user01");

        assertEquals("https://not-found-domain.invalid", result.url());

        verify(safeBrowsingService).isUnsafeUrl("https://not-found-domain.invalid");
        verify(urlScanService).lookupUrl("https://not-found-domain.invalid");
        verify(analysisResultRepository).save(any(AnalysisResultDTO.class));
        verify(analysisDetailRepository).saveAll(any());
    }

    @Test
    void analyzeUrlReturnsDangerousWhenSafeBrowsingDetectsThreat() throws Exception {
        when(safeBrowsingService.isUnsafeUrl(any())).thenReturn(true);
        when(analysisResultRepository.save(any(AnalysisResultDTO.class)))
                .thenAnswer(invocation -> savedResultWithId(invocation.getArgument(0)));

        UrlAnalysisResponseDTO result = urlAnalysisService.analyzeUrl("https://not-found-domain.invalid", "user01");

        assertEquals("dangerous", result.trustLevel());
        assertEquals(70, result.riskScore());
        assertEquals(2, result.scoreFactors().size());
        assertEquals("Google Safe Browsing 위험 감지", result.scoreFactors().get(1).label());
        assertEquals(55, result.scoreFactors().get(1).score());

        verify(safeBrowsingService).isUnsafeUrl("https://not-found-domain.invalid");
        verify(urlScanService).lookupUrl("https://not-found-domain.invalid");
        verify(analysisResultRepository).save(any(AnalysisResultDTO.class));
        verify(analysisDetailRepository).saveAll(any());
    }

    @Test
    void analyzeUrlAddsRiskForUserInfoMarkerInsteadOfBlocking() throws Exception {
        when(safeBrowsingService.isUnsafeUrl(any())).thenReturn(false);
        when(analysisResultRepository.save(any(AnalysisResultDTO.class)))
                .thenAnswer(invocation -> savedResultWithId(invocation.getArgument(0)));

        UrlAnalysisResponseDTO result = urlAnalysisService.analyzeUrl("https://user@not-found-domain.invalid", "user01");

        assertEquals("https://user@not-found-domain.invalid", result.url());
        assertEquals("caution", result.trustLevel());
        assertEquals(35, result.riskScore());
        assertEquals(2, result.scoreFactors().size());
        assertEquals("@ 문자 포함", result.scoreFactors().get(1).label());
        assertEquals(20, result.scoreFactors().get(1).score());

        verify(safeBrowsingService).isUnsafeUrl("https://user@not-found-domain.invalid");
        verify(urlScanService).lookupUrl("https://user@not-found-domain.invalid");
        verify(analysisResultRepository).save(any(AnalysisResultDTO.class));
        verify(analysisDetailRepository).saveAll(any());
    }

    @Test
    void analyzeUrlAddsRiskWhenReportedUrlMatches() throws Exception {
        when(safeBrowsingService.isUnsafeUrl(any())).thenReturn(false);
        when(reportFeedbackRepository.existsByFeedbackTypeAndContent("URL", "https://not-found-domain.invalid"))
                .thenReturn(true);
        when(analysisResultRepository.save(any(AnalysisResultDTO.class)))
                .thenAnswer(invocation -> savedResultWithId(invocation.getArgument(0)));

        UrlAnalysisResponseDTO result = urlAnalysisService.analyzeUrl("https://not-found-domain.invalid", "user01");

        assertEquals("caution", result.trustLevel());
        assertEquals(40, result.riskScore());
        assertEquals(2, result.scoreFactors().size());
        assertEquals("신고된 URL", result.scoreFactors().get(1).label());
        assertEquals(25, result.scoreFactors().get(1).score());

        verify(reportFeedbackRepository).existsByFeedbackTypeAndContent("URL", "https://not-found-domain.invalid");
        verify(urlScanService).lookupUrl("https://not-found-domain.invalid");
        verify(analysisResultRepository).save(any(AnalysisResultDTO.class));
        verify(analysisDetailRepository).saveAll(any());
    }

    @Test
    void analyzeUrlAddsRiskWhenUrlScanDetectsMaliciousVerdict() throws Exception {
        when(safeBrowsingService.isUnsafeUrl(any())).thenReturn(false);
        when(urlScanService.lookupUrl(any())).thenReturn(new UrlScanService.UrlScanLookupResult(
                true,
                true,
                80,
                3,
                "https://not-found-domain.invalid",
                "2026-10-07T00:00:00.000Z",
                "https://urlscan.io/result/test",
                "기존 분석 기록에서 악성 verdict가 확인되었습니다."
        ));
        when(analysisResultRepository.save(any(AnalysisResultDTO.class)))
                .thenAnswer(invocation -> savedResultWithId(invocation.getArgument(0)));

        UrlAnalysisResponseDTO result = urlAnalysisService.analyzeUrl("https://not-found-domain.invalid", "user01");

        assertEquals("caution", result.trustLevel());
        assertEquals(60, result.riskScore());
        assertEquals(2, result.scoreFactors().size());
        assertEquals("urlscan.io 악성 verdict", result.scoreFactors().get(1).label());
        assertEquals(45, result.scoreFactors().get(1).score());

        verify(safeBrowsingService).isUnsafeUrl("https://not-found-domain.invalid");
        verify(urlScanService).lookupUrl("https://not-found-domain.invalid");
        verify(analysisResultRepository).save(any(AnalysisResultDTO.class));
        verify(analysisDetailRepository).saveAll(any());
    }

    private AnalysisResultDTO savedResultWithId(AnalysisResultDTO result) {
        return new AnalysisResultDTO(
                "result-id",
                result.userId(),
                result.analysisType(),
                result.inputData(),
                result.score(),
                result.resultLabel(),
                result.summary(),
                result.createdAt(),
                result.rawResult()
        );
    }
}
