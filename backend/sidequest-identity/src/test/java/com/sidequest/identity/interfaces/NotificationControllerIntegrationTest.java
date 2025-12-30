package com.sidequest.identity.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sidequest.identity.application.NotificationService;
import com.sidequest.identity.infrastructure.NotificationDO;
import com.sidequest.identity.interfaces.dto.UnreadCountVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.sidequest.identity.testconfig.TestSecurityConfig;
import com.sidequest.identity.infrastructure.config.WebConfig;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class)
@Import({TestSecurityConfig.class, com.sidequest.identity.infrastructure.config.WebConfig.class, com.sidequest.identity.testconfig.DisableMyBatisMappersConfig.class})
@AutoConfigureMockMvc(addFilters = true)
@org.springframework.test.context.ActiveProfiles("test")
public class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    // Mock MyBatis mapper interfaces to avoid initialization errors in slice tests
    @MockBean
    private com.sidequest.identity.infrastructure.NotificationMapper notificationMapper;

    @Test
    void getUnreadCount_withUser_shouldReturnCounts() throws Exception {
        UnreadCountVO vo = UnreadCountVO.builder().chat(2).interaction(3).system(1).build();
        when(notificationService.getUnreadCount(eq(123L))).thenReturn(vo);

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("X-User-Id", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.chat").value(2))
                .andExpect(jsonPath("$.data.interaction").value(3))
                .andExpect(jsonPath("$.data.system").value(1));

        verify(notificationService, times(1)).getUnreadCount(eq(123L));
    }

    @Test
    void markRead_withUser_shouldCallService() throws Exception {
        mockMvc.perform(post("/api/notifications/mark-read")
                        .header("X-User-Id", "123")
                        .param("type", "interaction"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(notificationService, times(1)).markAsRead(eq(123L), eq("interaction"));
    }

    @Test
    void markRead_withoutUser_shouldReturnCode401() throws Exception {
        mockMvc.perform(post("/api/notifications/mark-read")
                        .param("type", "interaction"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        verifyNoInteractions(notificationService);
    }

    @Test
    void listNotifications_withUser_shouldReturnPage() throws Exception {
        Page<NotificationDO> page = new Page<>(1, 10);
        NotificationDO n = new NotificationDO();
        n.setUserId(123L);
        n.setType("interaction");
        n.setContent("hi");
        page.setRecords(java.util.List.of(n));
        page.setTotal(1);

        when(notificationService.listNotifications(eq(123L), eq(null), eq(null), eq(1), eq(10))).thenReturn(page);

        mockMvc.perform(get("/api/notifications")
                        .header("X-User-Id", "123")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].type").value("interaction"));

        verify(notificationService, times(1)).listNotifications(eq(123L), eq(null), eq(null), eq(1), eq(10));
    }
}
