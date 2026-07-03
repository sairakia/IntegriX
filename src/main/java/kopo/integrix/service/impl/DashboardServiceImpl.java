package kopo.integrix.service.impl;


/**
 * 대시보드 Service 구현체입니다. MongoDB 분석 기록을 기반으로 요약 수치, 최근 분석, 월별 추이, 위험도 분포 데이터를 계산합니다.
 */
import kopo.integrix.dto.dashboard.DashboardResponseDTO;
import kopo.integrix.dto.dashboard.DistributionDTO;
import kopo.integrix.dto.dashboard.MonthlyTrendDTO;
import kopo.integrix.dto.dashboard.RecentAnalysisDTO;
import kopo.integrix.dto.dashboard.SummaryDTO;
import kopo.integrix.dto.mongo.AnalysisResultDTO;
import kopo.integrix.repository.mongo.AnalysisResultRepository;
import kopo.integrix.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final String LABEL_SAFE = "안전";
    private static final String LABEL_CAUTION = "주의";
    private static final String LABEL_DANGEROUS = "위험";
    private static final TimeZone KOREA_TIME_ZONE = TimeZone.getTimeZone("Asia/Seoul");

    private final AnalysisResultRepository analysisResultRepository;

    @Override
    public DashboardResponseDTO getDashboard(String userId) {
        List<AnalysisResultDTO> allResults = analysisResultRepository.findAll();

        SummaryDTO summary = new SummaryDTO(
                allResults.size(),
                countByTypeAndLabel(allResults, "URL", LABEL_DANGEROUS),
                countByTypeAndLabel(allResults, "TEXT", LABEL_DANGEROUS),
                countByTypeAndLabel(allResults, "IMAGE", LABEL_DANGEROUS)
        );

        return new DashboardResponseDTO(
                summary,
                getDistribution(allResults),
                getMonthlyTrend(allResults),
                getRecentRecords(userId)
        );
    }

    private List<DistributionDTO> getDistribution(List<AnalysisResultDTO> results) {
        return List.of(
                new DistributionDTO(LABEL_SAFE, countByLabel(results, LABEL_SAFE)),
                new DistributionDTO(LABEL_CAUTION, countByLabel(results, LABEL_CAUTION)),
                new DistributionDTO(LABEL_DANGEROUS, countByLabel(results, LABEL_DANGEROUS))
        );
    }

    private long countByTypeAndLabel(List<AnalysisResultDTO> results, String type, String label) {
        return results.stream()
                .filter(result -> type.equals(result.analysisType()))
                .filter(result -> label.equals(normalizeLabel(result.resultLabel())))
                .count();
    }

    private long countByLabel(List<AnalysisResultDTO> results, String label) {
        return results.stream()
                .filter(result -> label.equals(normalizeLabel(result.resultLabel())))
                .count();
    }

    private List<MonthlyTrendDTO> getMonthlyTrend(List<AnalysisResultDTO> results) {
        if (results.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, long[]> monthMap = new LinkedHashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("M월");
        sdf.setTimeZone(KOREA_TIME_ZONE);

        results.stream()
                .filter(result -> result.createdAt() != null)
                .sorted(Comparator.comparing(AnalysisResultDTO::createdAt))
                .forEach(result -> {
                    String month = sdf.format(result.createdAt());
                    monthMap.putIfAbsent(month, new long[3]);

                    long[] counts = monthMap.get(month);
                    String label = normalizeLabel(result.resultLabel());
                    if (LABEL_SAFE.equals(label)) {
                        counts[0]++;
                    } else if (LABEL_CAUTION.equals(label)) {
                        counts[1]++;
                    } else if (LABEL_DANGEROUS.equals(label)) {
                        counts[2]++;
                    }
                });

        return monthMap.entrySet().stream()
                .map(entry -> new MonthlyTrendDTO(
                        entry.getKey(),
                        entry.getValue()[0],
                        entry.getValue()[1],
                        entry.getValue()[2]
                ))
                .toList();
    }

    private List<RecentAnalysisDTO> getRecentRecords(String userId) {
        if (userId == null || userId.isBlank()) {
            return new ArrayList<>();
        }

        List<AnalysisResultDTO> list = analysisResultRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId);
        if (list.isEmpty()) {
            return new ArrayList<>();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        sdf.setTimeZone(KOREA_TIME_ZONE);

        return list.stream()
                .map(result -> new RecentAnalysisDTO(
                        normalizeType(result.analysisType()),
                        result.inputData() != null ? result.inputData() : "",
                        normalizeLabel(result.resultLabel()),
                        result.createdAt() != null ? sdf.format(result.createdAt()) : ""
                ))
                .toList();
    }

    private String normalizeLabel(String label) {
        if (label == null || label.isBlank()) {
            return LABEL_CAUTION;
        }

        return switch (label) {
            case "safe", "reliable", "authentic", LABEL_SAFE -> LABEL_SAFE;
            case "caution", "needs-verification", LABEL_CAUTION -> LABEL_CAUTION;
            case "dangerous", "manipulated", LABEL_DANGEROUS -> LABEL_DANGEROUS;
            default -> LABEL_CAUTION;
        };
    }

    private String normalizeType(String type) {
        return switch (type == null ? "" : type) {
            case "TEXT" -> "텍스트";
            case "IMAGE" -> "이미지";
            default -> "URL";
        };
    }
}
