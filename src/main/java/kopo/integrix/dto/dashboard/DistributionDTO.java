package kopo.integrix.dto.dashboard;


/**
 * 대시보드 위험도 분포 DTO입니다. 안전, 주의, 위험 같은 결과 비율을 화면에 표시하기 위한 데이터를 담습니다.
 */
public record DistributionDTO(
        String label,
        long count
) {
}
