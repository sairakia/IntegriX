package kopo.integrix.service;


/**
 * 텍스트 분석 기능의 Service 계약입니다. 텍스트 입력을 받아 위험도 분석 응답을 반환하는 메서드를 정의합니다.
 */
import kopo.integrix.dto.text.TextAnalysisResponseDTO;

public interface TextAnalysisService {
    TextAnalysisResponseDTO analyzeText(String text, String userId);
}
