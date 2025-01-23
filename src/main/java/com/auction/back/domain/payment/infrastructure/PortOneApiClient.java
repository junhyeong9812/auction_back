package com.auction.back.domain.payment.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PortOneApiClient {

    private final RestTemplate restTemplate;
    private static final String BASE_URL = "https://api.iamport.kr";

    @Value("${imp.key}")
    private String impKey;

    @Value("${imp.secret}")
    private String impSecret;

    // -------------------------------------------------------------------------
    // 1) 토큰 발급
    // -------------------------------------------------------------------------
    public String getAccessToken() {
        String url = BASE_URL + "/users/getToken";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("imp_key", impKey);
        requestBody.put("imp_secret", impSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            String jsonBody = new ObjectMapper().writeValueAsString(requestBody);
            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> responseContent = (Map<String, Object>) responseBody.get("response");
                return (String) responseContent.get("access_token"); // 발급된 토큰 반환
            } else {
                throw new IllegalArgumentException("토큰 발급에 실패했습니다. 응답: " + response.getBody());
            }
        } catch (Exception e) {
            throw new RuntimeException("토큰 발급 중 에러 발생", e);
        }
    }

    // -------------------------------------------------------------------------
    // 2) imp_uid로 결제 정보 조회 (GET /payments/{imp_uid})
    // -------------------------------------------------------------------------
    public Map<String, Object> getPaymentInfo(String impUid) {
        String url = BASE_URL + "/payments/" + impUid;
        String accessToken = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, Map.class);

            // 응답 검증
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // code == 0 → 성공
                Integer code = (Integer) response.getBody().get("code");
                if (code != 0) {
                    // 실패
                    throw new IllegalArgumentException("결제 조회 실패: " + response.getBody().toString());
                }

                // response.get("response") 안에 실제 결제 정보
                Map<String, Object> paymentData = (Map<String, Object>) response.getBody().get("response");
                return paymentData;
            } else {
                throw new IllegalArgumentException("결제 조회에 실패했습니다. 응답: " + response.getBody());
            }
        } catch (Exception e) {
            throw new RuntimeException("결제 조회 중 오류 발생", e);
        }
    }

    // -------------------------------------------------------------------------
    // 3) 환불 로직 (POST /payments/cancel) - 기존 cancelPayment 예시
    // -------------------------------------------------------------------------
    public boolean cancelPayment(String impUid, String reason) {
        String url = BASE_URL + "/payments/cancel";
        String accessToken = getAccessToken();

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("imp_uid", impUid);
        requestBody.put("reason", reason);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            String jsonBody = new ObjectMapper().writeValueAsString(requestBody);
            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK
                    && response.getBody() != null
                    && (int) response.getBody().get("code") == 0) {
                return true; // 환불 성공
            } else {
                return false; // 환불 실패
            }
        } catch (Exception e) {
            throw new RuntimeException("환불 요청 중 오류 발생: " + e.getMessage());
        }
    }
}
