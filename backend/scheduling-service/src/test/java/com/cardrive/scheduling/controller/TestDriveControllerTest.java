package com.cardrive.scheduling.controller;

import com.cardrive.scheduling.dto.CreateTestDriveRequest;
import com.cardrive.scheduling.dto.TestDriveResponse;
import com.cardrive.scheduling.domain.entity.TestDriveStatus;
import com.cardrive.scheduling.exception.ConflictException;
import com.cardrive.scheduling.exception.ForbiddenException;
import com.cardrive.scheduling.security.CurrentUser;
import com.cardrive.scheduling.security.CurrentUserProvider;
import com.cardrive.scheduling.security.Role;
import com.cardrive.scheduling.service.TestDriveService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer test: HTTP status codes, request validation, and JSON shape.
 * Business logic is exercised in TestDriveServiceTest - here the service is
 * mocked and the security filter chain is disabled (addFilters = false) so
 * the test can focus purely on the controller/exception-handler contract,
 * with a fixed CurrentUser substituted in place of a real JWT.
 */
@WebMvcTest(controllers = TestDriveController.class)
@AutoConfigureMockMvc(addFilters = false)
class TestDriveControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TestDriveService testDriveService;
    @MockBean CurrentUserProvider currentUserProvider;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void createReturns201WithBody() throws Exception {
        CurrentUser buyer = new CurrentUser("buyer.joe", Set.of(Role.CUSTOMER));
        when(currentUserProvider.getCurrentUser()).thenReturn(buyer);

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        var request = new CreateTestDriveRequest(100L, start);
        var response = new TestDriveResponse(1L, 100L, "seller.jane", "buyer.joe", start, start.plusMinutes(45),
                TestDriveStatus.PENDING, null, null, java.time.Instant.now(), java.time.Instant.now());

        when(testDriveService.create(eq(buyer), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/test-drives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.sellerUsername").value("seller.jane"))
                .andExpect(jsonPath("$.buyerUsername").value("buyer.joe"));
    }

    @Test
    void createWithMissingCarIdReturns400() throws Exception {
        when(currentUserProvider.getCurrentUser()).thenReturn(new CurrentUser("buyer.joe", Set.of(Role.CUSTOMER)));

        String badJson = """
                {"startTime": "%s"}
                """.formatted(LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/api/v1/test-drives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void conflictExceptionMapsTo409() throws Exception {
        CurrentUser buyer = new CurrentUser("buyer.joe", Set.of(Role.CUSTOMER));
        when(currentUserProvider.getCurrentUser()).thenReturn(buyer);
        when(testDriveService.create(eq(buyer), any()))
                .thenThrow(new ConflictException("overlaps an existing booking"));

        var request = new CreateTestDriveRequest(100L, LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/api/v1/test-drives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SCHEDULING_CONFLICT"));
    }

    @Test
    void confirmBySomeoneWhoDoesNotOwnTheCarReturns403() throws Exception {
        CurrentUser seller = new CurrentUser("seller.other", Set.of(Role.SELLER));
        when(currentUserProvider.getCurrentUser()).thenReturn(seller);
        when(testDriveService.confirm(eq(seller), eq(1L)))
                .thenThrow(new ForbiddenException("You do not own this car"));

        mockMvc.perform(post("/api/v1/test-drives/1/confirm"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }
}
