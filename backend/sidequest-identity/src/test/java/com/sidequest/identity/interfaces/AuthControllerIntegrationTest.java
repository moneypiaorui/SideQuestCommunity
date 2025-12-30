package com.sidequest.identity.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sidequest.identity.application.UserService;
import com.sidequest.identity.interfaces.dto.LoginVO;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({TestSecurityConfig.class, com.sidequest.identity.infrastructure.config.WebConfig.class, com.sidequest.identity.testconfig.DisableMyBatisMappersConfig.class})
@AutoConfigureMockMvc(addFilters = true)
@org.springframework.test.context.ActiveProfiles("test")
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    // Mock MyBatis mapper interfaces to prevent MapperFactoryBean initialization in the test context
    @MockBean
    private com.sidequest.identity.infrastructure.RoleMapper roleMapper;

    @MockBean
    private com.sidequest.identity.infrastructure.BanRecordMapper banRecordMapper;

    @MockBean
    private com.sidequest.identity.infrastructure.NotificationMapper notificationMapper;

    @Test
    void logout_withoutAuthorization_shouldReturnCode401() throws Exception {
        mockMvc.perform(post("/api/identity/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        verifyNoInteractions(userService);
    }

    @Test
    void logout_withAuthorization_shouldCallServiceAndReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/identity/logout")
                        .header("Authorization", "Bearer sometoken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("Logout successful"));

        verify(userService, times(1)).logout(eq("sometoken"));
    }

    @Test
    void refreshToken_shouldReturnNewLoginVO() throws Exception {
        LoginVO vo = LoginVO.builder().token("new-token").expireIn(86400L).userId(1L).nickname("nick").build();
        when(userService.refreshToken(eq("oldtoken"))).thenReturn(vo);

        String body = objectMapper.writeValueAsString(java.util.Map.of("token", "oldtoken"));

        mockMvc.perform(post("/api/identity/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("new-token"));

        verify(userService, times(1)).refreshToken(eq("oldtoken"));
    }

    @Test
    void resetPassword_withPermission_shouldCallService() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("username", "alice", "newPassword", "pwd123"));

        mockMvc.perform(post("/api/identity/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        // grant the required authority via header used by HeaderAuthenticationFilter
                        .header("X-User-Id", "1")
                        .header("X-User-Permissions", "USER_ROLE_ASSIGN")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(userService, times(1)).resetPassword(eq("alice"), eq("pwd123"));
    }

    @Test
    void resetPassword_withoutPermission_shouldReturnForbidden() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("username", "alice", "newPassword", "pwd123"));

        // No permission header -> should be blocked by method security
        mockMvc.perform(post("/api/identity/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void changePassword_withoutUser_shouldReturnCode401() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("oldPassword", "a", "newPassword", "b"));

        mockMvc.perform(post("/api/identity/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        verifyNoInteractions(userService);
    }

    @Test
    void changePassword_withUser_shouldCallService() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("oldPassword", "a", "newPassword", "b"));

        mockMvc.perform(post("/api/identity/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "42")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(userService, times(1)).updatePassword(eq(42L), eq("a"), eq("b"));
    }
}
