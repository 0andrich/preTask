package org.backend;

import org.backend.domain.enums.NotificationChannel;
import org.backend.domain.enums.NotificationType;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.backend.dto.request.CreateNotificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = NotificationApplication.class)
@AutoConfigureMockMvc
@Transactional
class NotificationApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateNotificationRequest requestDto;

    @BeforeEach
    void setUp() {
        requestDto = new CreateNotificationRequest(
                123L,                          // recipientId (수신자 ID)
                NotificationType.COURSE_START_DDAY,     // notificationType
                NotificationChannel.EMAIL,              // NotificationChannel
                "EVENT_001",                            // referenceId (참조 이벤트 ID)
                "LECTURE",                              // referenceType
                Map.of("lectureName", "스프링 마스터 클래스"), // extraData
                null                                    // scheduledAt (즉시 발송은 null)
        );
    }

    @Test
    @DisplayName("1. 알림 발송 요청 - 정상 접수 시 202 ACCEPTED 반환")
    void createNotification_Success() throws Exception {
        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print()) // 콘솔에 상세 요청/응답 로그 출력
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.notificationId").exists());
    }

    @Test
    @DisplayName("2. 중복 발송 방지 - 동일한 요청 연속 전송 시 409 CONFLICT 반환 (멱등성 검증)")
    void createNotification_Duplicate_Conflict() throws Exception {
        // 첫 번째 요청: 정상 접수 (202)
        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isAccepted());

        // 두 번째 동일 요청: 중복 차단 (409)
        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.isDuplicate").value(true));
    }

    @Test
    @DisplayName("3. 알림 단건 상태 조회 - 특정 알림ID로 상세 데이터가 조회되어야 한다")
    void getNotificationDetail_Success() throws Exception {
        // 먼저 테스트용 데이터 하나 접수
        String responseBody = mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andReturn().getResponse().getContentAsString();

        // 생성된 알림 ID 추출
        Long generatedId = objectMapper.readTree(responseBody).get("notificationId").asLong();

        // 단건 조회 API 호출
        mockMvc.perform(get("/notifications/{id}", generatedId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(generatedId))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("4. 사용자 알림 목록 조회 - 헤더에 유저 ID를 실어 보냈을 때 목록과 페이징 정보가 와야 한다")
    void getUserNotificationList_Success() throws Exception {
        // 가상 유저(123L) 앞으로 알림 하나 등록
        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isAccepted());

        // 목록 조회
        mockMvc.perform(get("/notifications")
                        .header("X-User-Id", 123L)
                        .param("unreadOnly", "false")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable").exists());
    }

}
