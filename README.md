## 프로젝트 개요
멱등성, 재시도 정책, 운영에 중점을 두는 "알림 발송 시스템"
## 기술 스택
Spring Boot <br>
JPA <br>
MySQL
## 실행 방법
application.properties에 mysql 설정 후
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/스키마명?useSSL=false&characterEncoding=UTF-8&serverTimezone=UTC
spring.datasource.username=사용자명
spring.datasource.password=비밀번호
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
```
<br>
api는 notification, 배치는 batch 모듈 실행

## 요구사항 해석 및 가정
- 수강 신청 완료, 결제 확정, 강의 시작 D-1, 취소 처리 등 다양한 이벤트가 발생
- 이벤트 발생 시 사용자에게 이메일 또는 인앱 알림을 발송
- 알림 처리 실패가 비즈니스 트랜잭션에 영향을 주어서는 안 됨
- 네트워크 장애, 외부 이메일 서버 오류 등 일시적 장애에 대비해 재시도가 가능
- 동일한 이벤트에 대해 알림이 중복 발송되면 안 됨

- 실제 메시지 브로커 미사용
## 설계 결정과 이유
API와 알림 발송을 구분하고 추후 Kafka와 같은 분산 스트리밍 도입을 고려하여 "멀티 모듈"로 구성 
<br><br>
고유 해시 키를 생성하고 DB Unique 제약조건을 통해 동시성 이슈 및 중복 발송을 원천 차단
<br><br>
트랜잭션 단위로 관리하여 데이터의 무결성과 일관성을 유지
## 미구현 / 제약사항
- 발송 스케줄링(배치)
- 알림 템플릿 관리
- 여러 기기에서 동시에 읽음 처리 요청
- 최종 실패 알림 보관 및 수동 재시도
## AI 활용 범위
멱등성 키 해싱 유틸리티 코드 생성 보조

## API 목록 및 예시
1. 알림 발송 요청 등록 (POST /notifications)
```json
// 성공 시
{
  "notificationId": 7,
  "message": "알림 요청이 접수되었습니다.",
  "isDuplicate": false,
  "currentStatus": "PENDING"
}
// 실패 시
{
  "notificationId": 7,
  "message": "이미 접수된 알림 요청입니다.",
  "isDuplicate": true,
  "currentStatus": "PENDING"
}
```
2. 알림 상태 조회 (GET /notifications/{id})
```json
{
  "id": 8,
  "recipientId": 123,
  "notificationType": "COURSE_START_DDAY",
  "channel": "EMAIL",
  "status": "PENDING",
  "retryCount": 0,
  "maxRetryCount": 3,
  "createdAt": "2026-05-24T17:49:36.658509",
  "histories": []
}
```
3. 사용자 알림 목록 조회
```json
{
  "content": [
    {
      "id": 9,
      "recipientId": 123,
      "notificationType": "COURSE_START_DDAY",
      "channel": "EMAIL",
      "status": "PENDING",
      "createdAt": "2026-05-24T17:49:36.769542"
    }
  ],
  "pageable": { "pageNumber": 0, "pageSize": 10 },
  "totalPages": 1,
  "totalElements": 1,
  "last": true
}
```
## 데이터 모델 설명
core의 domain부분에 공통 엔티티 처리
<br>
notification에서 core의 의존성 주입을 한 dto를 통해 service와 controller 처리로 api 제공
## 테스트 실행 방법
notification.src.test.java.org.backend.NotificationApplicationTests 실행