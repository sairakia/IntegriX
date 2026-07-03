package kopo.integrix.service;


/**
 * 대시보드 통계 기능의 Service 계약입니다. 전체 분석 수, 최근 분석, 월별 추이, 위험도 분포를 조회하는 역할을 정의합니다.
 */
import kopo.integrix.dto.dashboard.DashboardResponseDTO;

public interface DashboardService {


    DashboardResponseDTO getDashboard(String userId);
}
