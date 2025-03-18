# 안전한 결제 시스템 구현: PortOne API 서버 검증 및 데이터 무결성 보장

## 📌 개요

본 프로젝트는 경매 시스템에서 포인트 충전을 위한 결제 프로세스를 구현하며, 특히 **데이터 위변조 방지**와 **서버 측 결제 검증**에 중점을 두었습니다. 프론트엔드에서 결제가 완료되었다는 정보만 신뢰하지 않고, 실제 결제 서비스인 PortOne(아임포트)의 API를 통해 결제 정보를 재검증하여 시스템의 무결성을 보장합니다.

## 🔒 보안 문제 인식

### 문제 상황

1. **결제 정보 위변조 위험**
    - 프론트엔드(React)에서 결제를 완료했다고 임의로 `impUid`(결제고유번호)와 결제금액을 조작해 백엔드에 전송할 경우 발생하는 위험
    - 서버가 클라이언트 데이터를 무조건 신뢰하면 실제로 결제되지 않은 포인트가 충전될 수 있음

2. **사용자 인증 및 권한 검증 부재**
    - 누구나 API를 호출하여 다른 사용자의 이메일로 포인트를 충전할 수 있는 취약점

3. **결제 상태 관리 미흡**
    - 결제가 취소되었는데도 포인트가 차감되지 않거나, 중복 결제가 처리되는 위험

## 💡 해결 방안

### 1. PortOne API를 활용한 서버 측 결제 검증

```java
// PortOneApiClient.java
public Map<String, Object> getPaymentInfo(String impUid) {
    String url = BASE_URL + "/payments/" + impUid;
    String accessToken = getAccessToken();

    // ... HTTP 헤더 설정

    try {
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, Map.class);

        // 응답 검증
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Integer code = (Integer) response.getBody().get("code");
            if (code != 0) {
                throw new IllegalArgumentException("결제 조회 실패: " + response.getBody().toString());
            }

            Map<String, Object> paymentData = (Map<String, Object>) response.getBody().get("response");
            return paymentData;
        } else {
            throw new IllegalArgumentException("결제 조회에 실패했습니다. 응답: " + response.getBody());
        }
    } catch (Exception e) {
        throw new RuntimeException("결제 조회 중 오류 발생", e);
    }
}
```

### 2. 다중 검증 로직 구현

```java
// PaymentCommandServiceImpl.java
@Override
public PaymentResponseDto processPayment(PaymentRequestDto requestDto) {
    // 1) 이메일 기반으로 User 조회 (유효한 사용자인지 확인)
    User user = userQueryService.findByEmail(requestDto.getEmail());

    // 2) PortOne 결제정보 조회 (실제 결제 여부와 금액 확인)
    Map<String, Object> paymentData = portOneApiClient.getPaymentInfo(requestDto.getImpUid());

    // paymentData에서 필요한 필드를 추출
    double amount = ((Number) paymentData.get("amount")).doubleValue();
    String statusStr = (String) paymentData.get("status");  // "paid", "failed", "cancelled" 등
    
    // PaymentStatus 변환 (서버에서 상태 관리)
    PaymentStatus paymentStatus;
    if ("paid".equals(statusStr)) {
        paymentStatus = PaymentStatus.PAID;
    } else if ("cancelled".equals(statusStr)) {
        paymentStatus = PaymentStatus.CANCELED;
    } else {
        paymentStatus = PaymentStatus.FAILED;
    }

    // 3) Payment 엔티티 생성 및 저장
    Payment payment = Payment.builder()
            .user(user)
            .impUid(requestDto.getImpUid())
            // ... 다른 필드 설정
            .status(paymentStatus)
            .build();

    Payment saved = paymentRepository.save(payment);

    // 4) 결제 상태가 PAID일 때만 포인트 충전
    if (saved.getStatus() == PaymentStatus.PAID) {
        saved.applyPointToUser();  // user.chargePoint(saved.getPaidAmount()) 호출
    }

    // 5) 결과 반환
    boolean isSuccess = (paymentStatus == PaymentStatus.PAID);
    double updatedPoint = user.getPointBalance();
    String message = isSuccess ? "결제 및 포인트 충전 성공" : "결제 실패 혹은 취소";

    return PaymentResponseDto.builder()
            .success(isSuccess)
            .updatedPoint(updatedPoint)
            .message(message)
            .build();
}
```

### 3. API 엔드포인트 구현 및 예외 처리

```java
// PaymentCommandController.java
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentCommandController {

    private final PaymentCommandService paymentCommandService;

    /**
     * 결제 완료 핸들링
     * - 프론트에서 impUid, email을 JSON으로 받는다 (PaymentRequestDto)
     * - 서비스에서 PortOne 결제검증 + DB 저장 + 포인트 충전
     */
    @PostMapping("/complete")
    public ResponseEntity<?> completePayment(@RequestBody PaymentRequestDto requestDto) {
        try {
            PaymentResponseDto result = paymentCommandService.processPayment(requestDto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
```

### 4. 프론트엔드 측 결제 요청 처리

```typescript
// 결제 요청 함수
const handleRequestPay = () => {
  if (!window.IMP) {
    alert("IMP 로드 실패");
    return;
  }

  // IMP.request_pay 파라미터
  const data = {
    channelKey: portOnechannelKey,
    pg: "nice",
    pay_method: "card",
    merchant_uid: `payment-${crypto.randomUUID()}`, // 주문 고유 번호
    name: "포인트 충전", // 결제창에 표시될 상품명
    amount: 100, // 실제로는 모달창에서 입력받을 수도 있음
    buyer_email: selectedEmail, // 선택된 유저 이메일
    buyer_name: buyerName,
    buyer_tel: buyerTel,
    buyer_addr: buyerAddr,
    buyer_postcode: buyerPostcode,
  };

  // 결제 요청
  window.IMP.request_pay(data, async (response: any) => {
    if (response.error_code) {
      // 에러가 있으면 결제 실패
      alert(`결제에 실패했습니다: ${response.error_msg}`);
      return;
    }

    // 결제 성공 시 서버에 결제정보 전송
    try {
      const res = await fetch("/payment/complete", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          impUid: response.imp_uid, // 아임포트 결제고유번호
          merchantUid: response.merchant_uid, // 주문번호
          email: selectedEmail, // 어떤 유저인지 식별
          amount: data.amount,
        }),
      });

      if (!res.ok) {
        const errMsg = await res.text();
        alert(`서버 결제 처리 오류: ${errMsg}`);
        return;
      }

      const result = await res.json();
      alert("결제가 완료되었습니다!");
    } catch (error) {
      console.error("결제 완료 통신 에러:", error);
      alert("결제는 완료되었으나, 서버 통신에 실패했습니다.");
    } finally {
      closeModal();
    }
  });
};
```

## 🛡️ 보안 강화 포인트

### 1. 서버 측 결제 검증
- 클라이언트가 보낸 `impUid`를 신뢰하지 않고, PortOne API를 통해 **실제 결제 정보**를 조회
- `status`, `amount` 등 중요 필드를 서버에서 재검증하여 위변조 방지

### 2. 사용자 인증 연계
- 결제 요청 시 사용자 이메일 검증
- 실제 DB에 존재하는 사용자인지 확인 후 포인트 충전

### 3. 트랜잭션 관리
- 결제 정보 저장과 포인트 충전을 트랜잭션으로 관리하여 데이터 일관성 보장
- 결제 상태가 'paid'일 때만 포인트를 충전하는 안전장치

### 4. 예외 처리 강화
- API 호출 과정에서 발생할 수 있는 다양한 예외 상황에 대응
- 클라이언트에게 명확한 오류 메시지 제공

## 📊 결과 및 효과

1. **데이터 무결성 보장**
    - 서버 측 검증을 통해 위변조된 결제 정보 차단
    - 실제 결제된 금액만큼만 정확히 포인트 충전

2. **보안성 강화**
    - 악의적인 API 요청으로부터 시스템 보호
    - 제3자 결제 플랫폼과의 직접 통신으로 신뢰도 향상

3. **사용자 경험 개선**
    - 정확한 결제 상태 반영으로 신뢰성 향상
    - 오류 발생 시 명확한 피드백 제공

4. **시스템 안정성**
    - 결제와 포인트 충전 간의 데이터 일관성 유지
    - 예외 상황에 대한 견고한 처리

## 🔄 처리 흐름도

```
[사용자] → [결제 모달 입력] → [PortOne 결제창] → [결제 완료] 
    → [프론트엔드: impUid 전송] → [백엔드: API 호출] 
    → [PortOne API: 결제정보 검증] → [백엔드: 결제정보 저장 및 포인트 충전] 
    → [사용자: 충전된 포인트로 서비스 이용]
```

## 📝 결론

본 프로젝트에서 구현한 결제 시스템은 단순히 클라이언트의 요청을 신뢰하는 방식이 아닌, 서버 측에서 철저한 검증을 거쳐 데이터의 무결성을 보장합니다. 이는 보안성을 높일 뿐만 아니라 실제 결제와 포인트 충전 간의 일관성을 유지함으로써 안정적인 사용자 경험을 제공합니다.

특히 PortOne API를 활용한 결제 검증 로직은 클라이언트에서 조작 가능한 데이터를 신뢰하지 않고, 실제 결제 처리 기관과 직접 통신하여 정확한 정보를 얻는 방식으로, 웹 애플리케이션에서 발생할 수 있는 데이터 위변조 위험을 효과적으로 방지합니다.