package io.sourceripple.admin.api;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.sourceripple.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AdminAccessIT extends AbstractIntegrationTest {

    @Test
    void maintainerIsForbiddenAndAdminCanListUsers() throws Exception {
        mvc.perform(get("/api/v1/admin/users").cookie(registerAndLogin("MAINTAINER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        String email = uniqueEmail();
        register(email, PASSWORD, "REVIEWER").andExpect(status().isCreated());
        mvc.perform(get("/api/v1/admin/users").param("q", email).cookie(createAdminAndLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].email").value(email))
                .andExpect(jsonPath("$.items[0].roles[0]").value("REVIEWER"))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void disablingAUserEndsTheirSessionAndBlocksLogin() throws Exception {
        String email = uniqueEmail();
        String userId = idOf(register(email, PASSWORD, "MAINTAINER").andReturn().getResponse().getContentAsString());
        Cookie userSession = sessionCookie(login(email, PASSWORD));

        mvc.perform(patch("/api/v1/admin/users/" + userId)
                        .with(xsrf())
                        .cookie(createAdminAndLogin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mvc.perform(get("/api/v1/auth/me").cookie(userSession)).andExpect(status().isUnauthorized());
        login(email, PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ACCOUNT_DISABLED"));
    }

    @Test
    void replacesRolesButNeverTheAdminsOwnAdminRole() throws Exception {
        Cookie admin = createAdminAndLogin();
        String adminId = idOf(mvc.perform(get("/api/v1/auth/me").cookie(admin))
                .andReturn()
                .getResponse()
                .getContentAsString(), "$.user.id");
        String userId = idOf(register(uniqueEmail(), PASSWORD, "REVIEWER")
                .andReturn()
                .getResponse()
                .getContentAsString());

        mvc.perform(put("/api/v1/admin/users/" + userId + "/roles")
                        .with(xsrf())
                        .cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\": [\"MAINTAINER\", \"REVIEWER\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasItem("MAINTAINER")));

        mvc.perform(put("/api/v1/admin/users/" + adminId + "/roles")
                        .with(xsrf())
                        .cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\": [\"MAINTAINER\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ROLE_NOT_ALLOWED"));
    }

    @Test
    void anAdminChangingTheirOwnRolesStaysSignedInWithTheNewRoles() throws Exception {
        Cookie admin = createAdminAndLogin();
        String adminId = idOf(mvc.perform(get("/api/v1/auth/me").cookie(admin))
                .andReturn()
                .getResponse()
                .getContentAsString(), "$.user.id");
        postJson("/api/v1/organizations", admin, "{\"name\": \"Before\"}").andExpect(status().isForbidden());

        mvc.perform(put("/api/v1/admin/users/" + adminId + "/roles")
                        .with(xsrf())
                        .cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\": [\"ADMIN\", \"MAINTAINER\"]}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/auth/me").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasItem("MAINTAINER")));
        postJson("/api/v1/organizations", admin, "{\"name\": \"After\"}").andExpect(status().isCreated());
    }

    @Test
    void unknownUserIsNotFound() throws Exception {
        mvc.perform(patch("/api/v1/admin/users/00000000-0000-0000-0000-000000000001")
                        .with(xsrf())
                        .cookie(createAdminAndLogin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    private static String idOf(String json) {
        return idOf(json, "$.id");
    }

    private static String idOf(String json, String path) {
        return JsonPath.read(json, path);
    }
}
