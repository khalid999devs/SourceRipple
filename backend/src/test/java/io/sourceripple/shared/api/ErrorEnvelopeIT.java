package io.sourceripple.shared.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.sourceripple.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.ResultActions;

class ErrorEnvelopeIT extends AbstractIntegrationTest {

    @Test
    void badRequestHasTheEnvelopeAndMatchingRequestId() throws Exception {
        expectEnvelope(register("not-an-email", "short", "MAINTAINER"), 400, "VALIDATION_FAILED");
    }

    @Test
    void notFoundHasTheEnvelopeAndMatchingRequestId() throws Exception {
        ResultActions result = mvc.perform(patch("/api/v1/admin/users/00000000-0000-0000-0000-000000000002")
                .with(xsrf())
                .cookie(createAdminAndLogin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\": true}"));

        expectEnvelope(result, 404, "NOT_FOUND");
    }

    @Test
    void conflictHasTheEnvelopeAndMatchingRequestId() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD, "MAINTAINER");

        expectEnvelope(register(email, PASSWORD, "MAINTAINER"), 409, "EMAIL_TAKEN");
    }

    @Test
    void propagatesAPlainIncomingRequestIdAndReplacesAnUnsafeOne() throws Exception {
        mvc.perform(get("/api/v1/auth/me").header("X-Request-Id", "trace-123"))
                .andExpect(header().string("X-Request-Id", "trace-123"))
                .andExpect(jsonPath("$.requestId").value("trace-123"));

        String replaced = mvc.perform(get("/api/v1/auth/me").header("X-Request-Id", "bad id\nforged"))
                .andReturn()
                .getResponse()
                .getHeader("X-Request-Id");
        assertThat(replaced).startsWith("req_");
    }

    @Test
    void malformedPathIdIsAValidationErrorWithoutAStackTrace() throws Exception {
        mvc.perform(patch("/api/v1/admin/users/not-a-uuid")
                        .with(xsrf())
                        .cookie(createAdminAndLogin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    private static void expectEnvelope(ResultActions result, int status, String error) throws Exception {
        result.andExpect(status().is(status))
                .andExpect(jsonPath("$.error").value(error))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.retryable").isBoolean())
                .andExpect(jsonPath("$.fieldErrors").isArray());
        MockHttpServletResponse response = result.andReturn().getResponse();
        String requestId = JsonPath.read(response.getContentAsString(), "$.requestId");
        assertThat(requestId).startsWith("req_").isEqualTo(response.getHeader("X-Request-Id"));
    }
}
