package io.sourceripple.workspace.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.sourceripple.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class OrganizationCrudIT extends AbstractIntegrationTest {

    @Test
    void ownerCreatesRenamesAndDeletesAnOrganization() throws Exception {
        Cookie owner = registerAndLogin("MAINTAINER");
        String id = createOrganization(owner, "Acme");

        mvc.perform(get("/api/v1/organizations").cookie(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("Acme"))
                .andExpect(jsonPath("$.items[0].owner").value(true));
        mvc.perform(get("/api/v1/auth/me").cookie(owner))
                .andExpect(jsonPath("$.organizations[0].id").value(id));
        mvc.perform(patch("/api/v1/organizations/" + id)
                        .with(xsrf())
                        .cookie(owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Acme Labs\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme Labs"));
        mvc.perform(delete("/api/v1/organizations/" + id).with(xsrf()).cookie(owner))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/organizations/" + id).cookie(owner)).andExpect(status().isNotFound());
    }

    @Test
    void reviewerCannotCreateAndNonMembersSeeNotFound() throws Exception {
        mvc.perform(post("/api/v1/organizations")
                        .with(xsrf())
                        .cookie(registerAndLogin("REVIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Nope\"}"))
                .andExpect(status().isForbidden());

        String id = createOrganization(registerAndLogin("MAINTAINER"), "Private");
        mvc.perform(get("/api/v1/organizations/" + id).cookie(registerAndLogin("MAINTAINER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void ownerManagesMembersByEmailAndCannotBeRemoved() throws Exception {
        Cookie owner = registerAndLogin("MAINTAINER");
        String id = createOrganization(owner, "Team");
        String memberEmail = uniqueEmail();
        register(memberEmail, PASSWORD, "REVIEWER");
        String ownerId = JsonPath.read(
                mvc.perform(get("/api/v1/auth/me").cookie(owner)).andReturn().getResponse().getContentAsString(),
                "$.user.id");

        String memberId = JsonPath.read(
                mvc.perform(post("/api/v1/organizations/" + id + "/members")
                                .with(xsrf())
                                .cookie(owner)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\": \"" + memberEmail.toUpperCase() + "\"}"))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.email").value(memberEmail))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.userId");

        Cookie member = sessionCookie(login(memberEmail, PASSWORD));
        mvc.perform(get("/api/v1/organizations/" + id + "/members").cookie(member))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2));
        mvc.perform(post("/api/v1/organizations/" + id + "/members")
                        .with(xsrf())
                        .cookie(owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + uniqueEmail() + "\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/organizations/" + id + "/members/" + ownerId).with(xsrf()).cookie(owner))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/organizations/" + id + "/members/" + memberId).with(xsrf()).cookie(member))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/organizations/" + id + "/members/" + memberId).with(xsrf()).cookie(owner))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/organizations/" + id).cookie(member)).andExpect(status().isNotFound());
    }

    @Test
    void sixthOwnedOrganizationExceedsTheQuota() throws Exception {
        Cookie owner = registerAndLogin("MAINTAINER");
        for (int i = 1; i <= 5; i++) {
            createOrganization(owner, "Org " + i);
        }

        mvc.perform(post("/api/v1/organizations")
                        .with(xsrf())
                        .cookie(owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Org 6\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("QUOTA_EXCEEDED"));
    }

    private String createOrganization(Cookie owner, String name) throws Exception {
        String body = mvc.perform(post("/api/v1/organizations")
                        .with(xsrf())
                        .cookie(owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}
