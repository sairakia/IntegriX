package kopo.integrix.service;


/**
 * 분석 기록 조회 기능의 Service 계약입니다. 로그인 사용자의 과거 분석 결과 목록을 조회하는 메서드를 정의합니다.
 */
import kopo.integrix.dto.history.HistoryItemDTO;

import java.util.List;

public interface AnalysisHistoryService {

    List<HistoryItemDTO> getHistory(String userId);
}
