package kopo.integrix.service.impl;


/**
 * 이미지 분석 비즈니스 로직입니다. 이미지 파일 또는 URL을 LLM으로 분석하고, 조작/AI 생성 의심 신호를 점수와 상세 항목으로 변환합니다.
 */
import kopo.integrix.dto.image.ImageAnalysisResponseDTO;
import kopo.integrix.dto.mongo.AnalysisDetailDTO;
import kopo.integrix.dto.mongo.AnalysisResultDTO;
import kopo.integrix.repository.mongo.AnalysisDetailRepository;
import kopo.integrix.repository.mongo.AnalysisResultRepository;
import kopo.integrix.service.ExternalAnalysisException;
import kopo.integrix.service.ImageAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ImageAnalysisServiceImpl implements ImageAnalysisService {

    private static final long MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    private static final int SAMPLE_STEP = 12;

    private final AnalysisResultRepository analysisResultRepository;
    private final AnalysisDetailRepository analysisDetailRepository;
    private final ImageLlmAnalysisClient imageLlmAnalysisClient;

    @Override
    public ImageAnalysisResponseDTO analyzeImage(MultipartFile imageFile, String imageUrl, String userId) throws Exception {
        // 파일 업로드와 URL 입력을 하나의 ImageSource로 통일해 이후 분석 로직을 공통 처리합니다.
        ImageSource source = loadImageSource(imageFile, imageUrl);
        // LLM에 넘기기 전 서버에서 읽을 수 있는 이미지 형식, 크기, 투명도, 복잡도를 먼저 계산합니다.
        ImageMetadata metadata = readImage(source);

        // 외부 LLM이 참고할 수 있도록 기본 메타데이터 기반의 로컬 분석 결과를 먼저 만듭니다.
        AnalysisResult localAnalysis = analyzeMetadata(source, metadata);
        AnalysisResult analysis = imageLlmAnalysisClient.analyze(
                        source.inputName(),
                        source.contentType(),
                        source.bytes(),
                        metadata.format(),
                        metadata.width(),
                        metadata.height(),
                        metadata.hasAlpha(),
                        metadata.complexity(),
                        localAnalysis
                )
                .map(this::fromLlmResult)
                .orElseThrow(() -> imageLlmAnalysisClient.isConfigured()
                        // LLM이 설정된 운영 환경에서는 실패를 안전/0점으로 처리하지 않고 명확한 실패로 반환합니다.
                        ? new ExternalAnalysisException("이미지 외부 분석 시간이 초과되었거나 실패했습니다. 잠시 후 다시 시도해주세요.")
                        : new ExternalAnalysisException("이미지 외부 분석 설정이 비활성화되어 분석을 완료할 수 없습니다."));
        // confidence는 신뢰도이므로 화면 위험 점수는 100 - confidence로 계산합니다.
        ImageAnalysisResponseDTO response = new ImageAnalysisResponseDTO(
                source.inputName(),
                analysis.credibility(),
                analysis.confidence(),
                100 - analysis.confidence(),
                new ImageAnalysisResponseDTO.Analysis(
                        analysis.metadata(),
                        analysis.manipulationIndicators(),
                        analysis.recommendations()
                )
        );
        // 분석 결과와 상세 항목은 마이페이지 분석 기록에서 재조회할 수 있도록 MongoDB에 저장합니다.
        saveAnalysis(source, analysis, userId, response);
        return response;
    }

    private ImageSource loadImageSource(MultipartFile imageFile, String imageUrl) throws Exception {
        // 사용자는 파일 업로드 또는 이미지 URL 중 하나만 제공하면 됩니다.
        boolean hasFile = imageFile != null && !imageFile.isEmpty();
        boolean hasUrl = imageUrl != null && !imageUrl.trim().isBlank();

        if (!hasFile && !hasUrl) {
            throw new IllegalArgumentException("이미지 파일을 선택하거나 이미지 URL을 입력해주세요.");
        }

        if (hasFile) {
            // 서버 저장/분석 비용과 악성 파일 위험을 줄이기 위해 크기와 MIME 타입을 제한합니다.
            if (imageFile.getSize() > MAX_IMAGE_BYTES) {
                throw new IllegalArgumentException("이미지 파일은 10MB 이하만 업로드할 수 있습니다.");
            }
            String contentType = imageFile.getContentType();
            if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                throw new IllegalArgumentException("이미지 파일만 분석할 수 있습니다.");
            }
            String filename = imageFile.getOriginalFilename() == null ? "uploaded image" : imageFile.getOriginalFilename();
            return new ImageSource(filename, imageFile.getBytes(), imageFile.getSize(), contentType);
        }

        // URL 입력은 프로토콜이 없으면 https를 붙여 사용자가 도메인만 입력해도 분석되게 합니다.
        String normalizedUrl = normalizeImageUrl(imageUrl.trim());
        URI uri = new URI(normalizedUrl);
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("이미지 URL은 http 또는 https 주소만 사용할 수 있습니다.");
        }
        // 서버가 내부망 URL을 호출하는 SSRF 위험을 막기 위해 공개 호스트인지 검증합니다.
        validatePublicHost(uri.getHost());

        // 이미지 URL은 직접 열리는 이미지 리소스여야 하므로 HTTP 상태와 content type을 확인합니다.
        URLConnection connection = uri.toURL().openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent", "IntegriX Image Analyzer/1.0");
        connection.setRequestProperty("Accept", "image/avif,image/webp,image/png,image/jpeg,image/*,*/*;q=0.8");

        if (connection instanceof HttpURLConnection httpConnection) {
            httpConnection.setInstanceFollowRedirects(true);
            int statusCode = httpConnection.getResponseCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new IllegalArgumentException("이미지 URL에 접속할 수 없습니다. HTTP 상태 코드: " + statusCode);
            }
        }

        byte[] bytes;
        try (InputStream inputStream = connection.getInputStream()) {
            bytes = inputStream.readNBytes((int) MAX_IMAGE_BYTES + 1);
        }
        if (bytes.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("이미지 파일은 10MB 이하만 분석할 수 있습니다.");
        }

        String contentType = connection.getContentType();
        if (!isSupportedImagePayload(bytes, contentType)) {
            throw new IllegalArgumentException("입력한 URL에서 분석 가능한 이미지 파일을 찾을 수 없습니다. 이미지가 직접 열리는 주소를 입력해주세요.");
        }

        return new ImageSource(normalizedUrl, bytes, bytes.length, normalizeImageContentType(contentType, bytes));
    }

    private String normalizeImageUrl(String imageUrl) {
        if (imageUrl.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) {
            return imageUrl;
        }
        return "https://" + imageUrl;
    }

    private boolean isReadableImage(byte[] bytes) {
        try {
            return ImageIO.read(new ByteArrayInputStream(bytes)) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSupportedImagePayload(byte[] bytes, String contentType) {
        if (isReadableImage(bytes)) {
            return true;
        }
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return true;
        }
        return inferImageContentType(bytes) != null;
    }

    private String normalizeImageContentType(String contentType, byte[] bytes) {
        String inferred = inferImageContentType(bytes);
        if (inferred != null) {
            return inferred;
        }
        if (contentType == null || contentType.isBlank()) {
            return "image/unknown";
        }
        return contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }

    private String inferImageContentType(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xff) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4e
                && bytes[3] == 0x47) {
            return "image/png";
        }
        if (bytes.length >= 12
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P') {
            return "image/webp";
        }
        if (bytes.length >= 12
                && bytes[4] == 'f'
                && bytes[5] == 't'
                && bytes[6] == 'y'
                && bytes[7] == 'p') {
            String brand = new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).toLowerCase(Locale.ROOT);
            if (brand.startsWith("avif")) {
                return "image/avif";
            }
        }
        if (bytes.length >= 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return "image/gif";
        }
        return null;
    }

    private ImageMetadata readImage(ImageSource source) throws Exception {
        String format = detectFormat(source.bytes());
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(source.bytes()));
        if (image == null) {
            return new ImageMetadata(formatFromContentType(source.contentType()), 0, 0, false, 0);
        }
        return new ImageMetadata(format, image.getWidth(), image.getHeight(), image.getColorModel().hasAlpha(), estimateComplexity(image));
    }

    private String formatFromContentType(String contentType) {
        if (contentType == null) {
            return "UNKNOWN";
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg", "image/jpg" -> "JPEG";
            case "image/png" -> "PNG";
            case "image/webp" -> "WEBP";
            case "image/avif" -> "AVIF";
            case "image/gif" -> "GIF";
            default -> "UNKNOWN";
        };
    }

    private String detectFormat(byte[] bytes) throws Exception {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            return readers.hasNext() ? readers.next().getFormatName().toUpperCase(Locale.ROOT) : "UNKNOWN";
        }
    }

    private AnalysisResult analyzeMetadata(ImageSource source, ImageMetadata metadata) {
        // 로컬 메타데이터 분석은 LLM 실패 시 대체가 아니라, LLM 판단의 참고 정보로 사용됩니다.
        List<String> metadataItems = new ArrayList<>();
        List<String> indicators = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        metadataItems.add("분석한 파일은 " + source.inputName() + "입니다.");
        metadataItems.add("이미지 크기는 " + metadata.width() + " x " + metadata.height() + "입니다.");
        metadataItems.add("AI 생성 또는 합성 흔적을 찾기 위한 기본 검사를 진행했습니다.");
        metadataItems.add(metadata.hasAlpha()
                ? "투명 배경이 있어 합성 소재로 쓰였을 가능성도 함께 확인해야 합니다."
                : "투명 배경은 확인되지 않았습니다.");

        int riskScore = 0;
        boolean dimensionsKnown = metadata.width() > 0 && metadata.height() > 0;
        if (!dimensionsKnown) {
            // 서버에서 이미지 크기를 읽지 못하면 단정하지 않고 확인 필요 수준으로만 안내합니다.
            indicators.add("서버에서 이미지 크기와 세부 메타데이터를 직접 읽지는 못했지만, 이미지 바이트는 분석 대상으로 확인되었습니다.");
            recommendations.add("브라우저에서 이미지가 정상 표시되면 원본 링크와 함께 분석 결과를 참고하세요.");
            return new AnalysisResult("needs-verification", 80, metadataItems.stream().limit(4).toList(), indicators, recommendations);
        }

        // 해상도, 비율, 압축률, 단순도를 이용해 LLM이 보기 전 기본 주의 신호를 계산합니다.
        int pixels = metadata.width() * metadata.height();
        double bytesPerPixel = pixels == 0 ? 0 : (double) source.size() / pixels;
        double aspectRatio = metadata.height() == 0 ? 0 : (double) metadata.width() / metadata.height();

        if (metadata.width() < 200 || metadata.height() < 200) {
            riskScore += 12;
            indicators.add("이미지 크기가 작아 AI 생성 흔적이나 합성 경계를 자세히 보기 어렵습니다.");
        }

        if (aspectRatio > 4.0 || aspectRatio < 0.25) {
            riskScore += 10;
            indicators.add("이미지가 지나치게 길거나 넓어 편집 또는 캡처 과정에서 일부가 잘렸을 수 있습니다.");
        }

        if ("JPEG".equals(metadata.format()) && bytesPerPixel < 0.08) {
            riskScore += 12;
            indicators.add("압축이 강해 세부 질감, 경계선, 합성 흔적을 확인하기 어렵습니다.");
        }

        if (metadata.complexity() < 8.0) {
            riskScore += 8;
            indicators.add("사진보다는 로고, 아이콘, 스크린샷처럼 단순한 이미지에 가까워 AI 생성 여부를 단정하기 어렵습니다.");
        }

        if (source.inputName().startsWith("http")) {
            recommendations.add("원본 게시물이나 원본 파일이 있는지 확인하세요.");
        }

        if (indicators.isEmpty()) {
            indicators.add("파일 자체에서 뚜렷한 합성 또는 AI 생성 주의 신호는 낮게 보입니다.");
            recommendations.add("중요한 이미지라면 원본 출처와 촬영 또는 제작 맥락을 함께 확인하세요.");
        } else {
            recommendations.add("주의 신호는 참고용이므로 원본 이미지와 비교해 확인하세요.");
            recommendations.add("증거 자료로 사용할 이미지라면 역이미지 검색, 원본 파일, 게시자 정보를 확인하세요.");
        }

        int confidence = Math.max(0, 100 - Math.min(100, riskScore));
        String credibility = confidence >= 75
                ? "authentic"
                : confidence >= 45 ? "needs-verification" : "manipulated";

        return new AnalysisResult(credibility, confidence, metadataItems.stream().limit(4).toList(), indicators, recommendations);
    }

    private double estimateComplexity(BufferedImage image) {
        // 일정 간격의 픽셀 차이를 샘플링해 이미지가 단순한 그래픽인지 복잡한 사진인지 추정합니다.
        long totalDiff = 0;
        long samples = 0;

        for (int y = SAMPLE_STEP; y < image.getHeight(); y += SAMPLE_STEP) {
            for (int x = SAMPLE_STEP; x < image.getWidth(); x += SAMPLE_STEP) {
                int current = image.getRGB(x, y);
                int previous = image.getRGB(x - SAMPLE_STEP, y - SAMPLE_STEP);
                totalDiff += colorDistance(current, previous);
                samples++;
            }
        }

        return samples == 0 ? 0 : (double) totalDiff / samples;
    }

    private int colorDistance(int rgbA, int rgbB) {
        int red = Math.abs(((rgbA >> 16) & 0xff) - ((rgbB >> 16) & 0xff));
        int green = Math.abs(((rgbA >> 8) & 0xff) - ((rgbB >> 8) & 0xff));
        int blue = Math.abs((rgbA & 0xff) - (rgbB & 0xff));
        return (red + green + blue) / 3;
    }

    private void validatePublicHost(String host) throws Exception {
        // 이미지 URL 분석에서 서버가 localhost나 내부망 주소에 접근하지 못하도록 차단합니다.
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("이미지 URL의 도메인을 확인할 수 없습니다.");
        }

        String lowerHost = host.toLowerCase(Locale.ROOT);
        if ("localhost".equals(lowerHost) || lowerHost.endsWith(".localhost")) {
            throw new IllegalArgumentException("localhost 이미지 URL은 분석할 수 없습니다.");
        }

        for (InetAddress address : InetAddress.getAllByName(host)) {
            if (address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()
                    || address.isMulticastAddress()) {
                throw new IllegalArgumentException("내부망 또는 비공개 IP의 이미지 URL은 분석할 수 없습니다.");
            }
        }
    }

    private void saveAnalysis(ImageSource source,
                              AnalysisResult analysis,
                              String userId,
                              ImageAnalysisResponseDTO response) {
        // MongoDB 목록에는 요약 점수와 원본 응답을 저장합니다.
        Date now = new Date();
        int riskScore = 100 - analysis.confidence();
        String resultLabel = switch (analysis.credibility()) {
            case "authentic" -> "safe";
            case "needs-verification" -> "caution";
            default -> "dangerous";
        };

        AnalysisResultDTO result = analysisResultRepository.save(new AnalysisResultDTO(
                null,
                userId != null && !userId.isBlank() ? userId : "guest",
                "IMAGE",
                source.inputName(),
                riskScore,
                resultLabel,
                "\uC774\uBBF8\uC9C0 \uC758\uC2EC\uB3C4 \uC810\uC218 " + riskScore + "\uC810.",
                now,
                response
        ));

        List<AnalysisDetailDTO> details = new ArrayList<>();
        // 조작 의심 근거는 위험도에 따라 상세 기록의 severity를 다르게 저장합니다.
        for (String indicator : analysis.manipulationIndicators()) {
            String severity = isNonScoringIndicator(indicator) ? "info" : (riskScore >= 55 ? "high" : "warning");
            details.add(new AnalysisDetailDTO(null, result.resultId(), indicator, severity, now));
        }
        for (String recommendation : analysis.recommendations()) {
            details.add(new AnalysisDetailDTO(null, result.resultId(), recommendation, "info", now));
        }
        analysisDetailRepository.saveAll(details);
    }

    private AnalysisResult fromLlmResult(ImageLlmAnalysisClient.LlmImageAnalysisResult llmResult) {
        // LLM 응답 신뢰도와 라벨을 내부 기준으로 정규화한 뒤 실제 위험 점수를 다시 계산합니다.
        int confidence = Math.max(0, Math.min(100, llmResult.confidence()));
        String credibility = normalizeCredibility(llmResult.credibility(), confidence);
        int adjustedRiskScore = calculateLlmRiskScore(confidence, credibility, llmResult.manipulationIndicators());
        int adjustedConfidence = 100 - adjustedRiskScore;

        return new AnalysisResult(
                credibilityFromRisk(adjustedRiskScore, credibility),
                adjustedConfidence,
                nonEmpty(llmResult.metadata(), "\uC774\uBBF8\uC9C0\uC758 AI \uC0DD\uC131 \uB610\uB294 \uD569\uC131 \uC758\uC2EC \uC5EC\uBD80\uB97C \uD655\uC778\uD588\uC2B5\uB2C8\uB2E4."),
                nonEmpty(llmResult.manipulationIndicators(), "\uB208\uC5D0 \uB744\uB294 AI \uC0DD\uC131 \uB610\uB294 \uD569\uC131 \uC8FC\uC758 \uC2E0\uD638\uB294 \uBCF4\uC774\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."),
                nonEmpty(llmResult.recommendations(), "\uC774\uBBF8\uC9C0\uB97C \uC2E0\uB8B0\uD558\uAE30 \uC804 \uC6D0\uBCF8 \uCD9C\uCC98\uC640 \uC81C\uC791 \uB9E5\uB77D\uC744 \uD655\uC778\uD558\uC138\uC694.")
        );
    }

    private String normalizeCredibility(String credibility, int confidence) {
        if ("authentic".equals(credibility) || "needs-verification".equals(credibility) || "manipulated".equals(credibility)) {
            return credibility;
        }
        if (confidence >= 75) {
            return "authentic";
        }
        if (confidence >= 45) {
            return "needs-verification";
        }
        return "manipulated";
    }

    private int calculateLlmRiskScore(int confidence, String credibility, List<String> manipulationIndicators) {
        // “주의 신호 없음” 같은 비위험 문구는 점수에 반영하지 않고 실제 의심 근거만 카운트합니다.
        int effectiveIndicatorCount = countEffectiveIndicators(manipulationIndicators);
        int confidenceRisk = Math.max(0, 100 - Math.max(0, Math.min(100, confidence)));

        if (effectiveIndicatorCount == 0) {
            if ("manipulated".equals(credibility)) {
                return 30;
            }
            return 0;
        }

        int adjustedRiskScore = switch (credibility) {
            case "manipulated" -> 50;
            case "needs-verification" -> 20;
            default -> 10;
        };

        adjustedRiskScore += switch (credibility) {
            case "manipulated" -> Math.min(35, effectiveIndicatorCount * 10);
            case "needs-verification" -> Math.min(25, effectiveIndicatorCount * 8);
            default -> Math.min(15, effectiveIndicatorCount * 5);
        };

        if ("authentic".equals(credibility)) {
            adjustedRiskScore = Math.min(adjustedRiskScore, 25);
        }
        if ("needs-verification".equals(credibility)) {
            adjustedRiskScore = Math.min(adjustedRiskScore, 45);
        }
        if ("manipulated".equals(credibility) && effectiveIndicatorCount < 2) {
            adjustedRiskScore = Math.min(adjustedRiskScore, 55);
        }
        if (confidenceRisk >= 50 && effectiveIndicatorCount >= 3 && "manipulated".equals(credibility)) {
            adjustedRiskScore += 5;
        }

        // 의심 근거가 하나라도 있으면 최소 주의 구간으로 올려 화면에서 안전으로 보이지 않게 합니다.
        adjustedRiskScore = Math.max(adjustedRiskScore, 21);
        return Math.min(100, adjustedRiskScore);
    }

    private int countEffectiveIndicators(List<String> manipulationIndicators) {
        // LLM이 반환한 문장 중 실제 점수에 반영할 근거만 세기 위한 필터링 단계입니다.
        if (manipulationIndicators == null || manipulationIndicators.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (String indicator : manipulationIndicators) {
            if (indicator != null && !indicator.isBlank() && !isNonScoringIndicator(indicator)) {
                count++;
            }
        }
        return count;
    }

    private boolean isNonScoringIndicator(String indicator) {
        // 압축/저해상도처럼 분석 한계에 가까운 문구는 위험 점수에서 제외합니다.
        if (isCompressionLimitationIndicator(indicator) || isAnalysisLimitationIndicator(indicator)) {
            return true;
        }
        if (hasScoringRiskCue(indicator)) {
            return false;
        }
        return isNoSuspicionIndicator(indicator) || isWeakVisualContextIndicator(indicator);
    }

    private boolean isAnalysisLimitationIndicator(String indicator) {
        String normalized = indicator.toLowerCase(Locale.ROOT);
        boolean mentionsLimitation = normalized.contains("\uD655\uC778\uC774 \uC5B4\uB824")
                || normalized.contains("\uD655\uC778\uD558\uAE30 \uC5B4\uB824")
                || normalized.contains("\uD310\uBCC4\uD558\uAE30 \uC5B4\uB824")
                || normalized.contains("\uD310\uBCC4\uC774 \uC5B4\uB824")
                || normalized.contains("\uAC00\uB824\uC9C8 \uC218")
                || normalized.contains("\uBCF4\uAE30 \uC5B4\uB835")
                || normalized.contains("\uB0AE\uC740 \uD574\uC0C1\uB3C4")
                || normalized.contains("\uD574\uC0C1\uB3C4\uAC00 \uB0AE")
                || normalized.contains("\uC774\uBBF8\uC9C0 \uD06C\uAE30\uAC00 \uC791")
                || normalized.contains("\uBBF8\uC138\uD55C \uC778\uACF5\uBB3C \uD655\uC778\uC774 \uC5B4\uB824")
                || normalized.contains("hard to verify")
                || normalized.contains("hard to determine")
                || normalized.contains("difficult to verify")
                || normalized.contains("difficult to determine")
                || normalized.contains("low resolution");
        boolean hasConcreteRisk = normalized.contains("\uACBD\uACC4\uAC00 \uC5B4\uC0C9")
                || normalized.contains("\uBA85\uD655\uD55C \uD569\uC131")
                || normalized.contains("\uB69C\uB837\uD55C \uD569\uC131")
                || normalized.contains("\uBD99\uC5EC\uB123")
                || normalized.contains("\uBD88\uC77C\uCE58\uAC00 \uBCF4")
                || normalized.contains("\uC65C\uACE1\uC774 \uBCF4")
                || normalized.contains("\uAE68\uC9D0\uC774 \uBCF4")
                || normalized.contains("clearly")
                || normalized.contains("obvious")
                || normalized.contains("visible mismatch");

        return mentionsLimitation && !hasConcreteRisk;
    }

    private boolean isCompressionLimitationIndicator(String indicator) {
        String normalized = indicator.toLowerCase(Locale.ROOT);
        boolean mentionsCompression = normalized.contains("jpeg")
                || normalized.contains("jpg")
                || normalized.contains("\uC555\uCD95")
                || normalized.contains("\uBE14\uB85D \uB178\uC774\uC988")
                || normalized.contains("\uB178\uC774\uC988")
                || normalized.contains("\uD654\uC9C8 \uC190\uC2E4")
                || normalized.contains("\uD654\uC9C8 \uC800\uD558")
                || normalized.contains("compression")
                || normalized.contains("block noise")
                || normalized.contains("quality loss");
        boolean mentionsLocalizedMismatch = normalized.contains("\uD2B9\uC815 \uC601\uC5ED")
                || normalized.contains("\uC77C\uBD80 \uC601\uC5ED")
                || normalized.contains("\uBD88\uC77C\uCE58")
                || normalized.contains("\uB2E4\uB974\uAC8C")
                || normalized.contains("\uCC28\uC774")
                || normalized.contains("\uACBD\uACC4\uAC00 \uC5B4\uC0C9")
                || normalized.contains("\uBD99\uC5EC\uB123")
                || normalized.contains("localized")
                || normalized.contains("inconsistent")
                || normalized.contains("mismatch")
                || normalized.contains("pasted");

        return mentionsCompression && !mentionsLocalizedMismatch;
    }

    private boolean hasScoringRiskCue(String indicator) {
        String normalized = indicator.toLowerCase(Locale.ROOT);

        boolean hasRiskCue = normalized.contains("\uD569\uC131 \uD754\uC801")
                || normalized.contains("ai \uC0DD\uC131 \uD754\uC801")
                || normalized.contains("ai \uC0DD\uC131 \uAC00\uB2A5")
                || normalized.contains("ai \uBCF4\uC815")
                || normalized.contains("ai \uC0DD\uC131 \uC5EC\uBD80")
                || normalized.contains("\uBC30\uC81C\uD560 \uC218 \uC5C6")
                || normalized.contains("\uBC30\uC81C\uD558\uAE30 \uC5B4\uB824")
                || normalized.contains("\uBCF4\uC815")
                || normalized.contains("\uB9AC\uD130\uCE6D")
                || normalized.contains("\uD3B8\uC9D1 \uAC00\uB2A5")
                || normalized.contains("\uACFC\uB3C4\uD55C")
                || normalized.contains("\uC0DD\uC131\uD615 ai")
                || normalized.contains("\uC5B4\uC0C9")
                || normalized.contains("\uC65C\uACE1")
                || normalized.contains("\uD750\uB9BF")
                || normalized.contains("\uACBD\uACC4")
                || normalized.contains("\uBD88\uC77C\uCE58")
                || normalized.contains("\uAE68\uC9D0")
                || normalized.contains("\uB4A4\uD2C0")
                || normalized.contains("\uBC18\uBCF5")
                || normalized.contains("\uBD99\uC5EC\uB123")
                || normalized.contains("\uC870\uBA85 \uCC28\uC774")
                || normalized.contains("\uADF8\uB9BC\uC790 \uCC28\uC774")
                || normalized.contains("\uC9C8\uAC10 \uCC28\uC774")
                || normalized.contains("artifact")
                || normalized.contains("warped")
                || normalized.contains("distorted")
                || normalized.contains("inconsistent")
                || normalized.contains("pasted")
                || normalized.contains("blurred")
                || normalized.contains("blurry");

        if (hasRiskCue) {
            return true;
        }

        return false;
    }

    private boolean isNoSuspicionIndicator(String indicator) {
        String normalized = indicator.toLowerCase(Locale.ROOT);
        return normalized.contains("\uBCF4\uC774\uC9C0 \uC54A")
                || normalized.contains("\uC5C6\uC74C")
                || normalized.contains("\uC5C6\uC2B5\uB2C8\uB2E4")
                || normalized.contains("\uAD00\uCC30\uB418\uC9C0 \uC54A")
                || normalized.contains("\uBC1C\uACAC\uB418\uC9C0")
                || normalized.contains("\uAC10\uC9C0\uB418\uC9C0 \uC54A")
                || normalized.contains("\uB69C\uB837\uD558\uC9C0 \uC54A")
                || normalized.contains("\uBA85\uD655\uD558\uC9C0 \uC54A")
                || normalized.contains("\uB69C\uB837\uD55C") && normalized.contains("\uC5C6")
                || normalized.contains("\uC758\uC2EC \uC2E0\uD638\uB294 \uB0AE")
                || normalized.contains("no strong")
                || normalized.contains("no visible")
                || normalized.contains("not visible")
                || normalized.contains("not observed")
                || normalized.contains("no obvious")
                || normalized.contains("not detected")
                || normalized.contains("no clear");
    }

    private boolean isWeakVisualContextIndicator(String indicator) {
        String normalized = indicator.toLowerCase(Locale.ROOT);
        return normalized.contains("\uD770 \uBC30\uACBD")
                || normalized.contains("\uB2E8\uC77C \uBB3C\uCCB4")
                || normalized.contains("\uC81C\uD488 \uC0AC\uC9C4")
                || normalized.contains("\uC2A4\uD1A1")
                || normalized.contains("\uC2A4\uD29C\uB514\uC624")
                || normalized.contains("\uBC30\uACBD\uC774 \uB2E8\uC21C")
                || normalized.contains("\uAD6C\uB3C4\uAC00 \uB2E8\uC21C")
                || normalized.contains("\uBE44\uAD50 \uD655\uC778")
                || normalized.contains("\uCD9C\uCC98 \uD655\uC778")
                || normalized.contains("stock")
                || normalized.contains("product photo")
                || normalized.contains("plain background")
                || normalized.contains("single object");
    }

    private String credibilityFromRisk(int riskScore, String originalCredibility) {
        if (riskScore >= 65) {
            return "manipulated";
        }
        if (riskScore >= 21) {
            return "needs-verification";
        }
        return "manipulated".equals(originalCredibility) ? "needs-verification" : "authentic";
    }

    private List<String> nonEmpty(List<String> values, String fallback) {
        if (values == null || values.isEmpty()) {
            return List.of(fallback);
        }
        return values;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        return String.format(Locale.ROOT, "%.1f MB", bytes / 1024.0 / 1024.0);
    }

    private record ImageSource(String inputName, byte[] bytes, long size, String contentType) {
    }

    private record ImageMetadata(String format, int width, int height, boolean hasAlpha, double complexity) {
    }

    record AnalysisResult(
            String credibility,
            int confidence,
            List<String> metadata,
            List<String> manipulationIndicators,
            List<String> recommendations
    ) {
    }
}
