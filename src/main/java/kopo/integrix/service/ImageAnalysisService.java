package kopo.integrix.service;


/**
 * 이미지 분석 기능의 Service 계약입니다. 업로드 파일 또는 이미지 URL을 분석하고 이미지 위험도 응답을 반환하는 역할을 정의합니다.
 */
import kopo.integrix.dto.image.ImageAnalysisResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface ImageAnalysisService {
    ImageAnalysisResponseDTO analyzeImage(MultipartFile imageFile, String imageUrl, String userId) throws Exception;
}
