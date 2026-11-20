package io.sourceripple.workspace.api;

import static org.hamcrest.Matchers.startsWith;
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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ProjectCrudIT extends AbstractIntegrationTest {

    @Test
    void fullCycleWithTheRightCodes() throws Exception {
        Cookie maintainer = registerAndLogin("MAINTAINER");
        String organizationId = createOrganization(maintainer);

        String body = mvc.perform(post("/api/v1/organizations/" + organizationId + "/projects")
                        .with(xsrf())
                        .cookie(maintainer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Shop\", \"description\": \"Storefront\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("shop"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String projectId = JsonPath.read(body, "$.id");

        mvc.perform(post("/api/v1/organizations/" + organizationId + "/projects")
                        .with(xsrf())
                        .cookie(maintainer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Shop\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/v1/projects/")))
                .andExpect(jsonPath("$.slug").value("shop-2"));

        mvc.perform(get("/api/v1/projects/" + projectId).cookie(maintainer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Shop"));
        mvc.perform(patch("/api/v1/projects/" + projectId)
                        .with(xsrf())
                        .cookie(maintainer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Web shop\", \"description\": \"New\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Web shop"))
                .andExpect(jsonPath("$.slug").value("shop"));
        mvc.perform(get("/api/v1/projects/" + projectId + "/summary").cookie(maintainer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.watchCount").value(0));
        mvc.perform(delete("/api/v1/projects/" + projectId).with(xsrf()).cookie(maintainer))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/projects/" + projectId).cookie(maintainer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivedAt").isNotEmpty());
        mvc.perform(get("/api/v1/projects/" + UUID.randomUUID()).cookie(maintainer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void reviewerMemberReadsButCannotWriteAndOutsidersSeeNotFound() throws Exception {
        Cookie maintainer = registerAndLogin("MAINTAINER");
        String organizationId = createOrganization(maintainer);
        String projectId = JsonPath.read(
                mvc.perform(post("/api/v1/organizations/" + organizationId + "/projects")
                                .with(xsrf())
                                .cookie(maintainer)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"Mobile\"}"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id");
        String reviewerEmail = uniqueEmail();
        register(reviewerEmail, PASSWORD, "REVIEWER");
        mvc.perform(post("/api/v1/organizations/" + organizationId + "/members")
                .with(xsrf())
                .cookie(maintainer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + reviewerEmail + "\"}"));
        Cookie reviewer = sessionCookie(login(reviewerEmail, PASSWORD));

        mvc.perform(get("/api/v1/projects/" + projectId).cookie(reviewer)).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/projects/" + projectId)
                        .with(xsrf())
                        .cookie(reviewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Hijack\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/projects/" + projectId).cookie(registerAndLogin("MAINTAINER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void archivedProjectsLetTheOrganizationBeDeleted() throws Exception {
        Cookie maintainer = registerAndLogin("MAINTAINER");
        String organizationId = createOrganization(maintainer);
        String projectId = JsonPath.read(
                mvc.perform(post("/api/v1/organizations/" + organizationId + "/projects")
                                .with(xsrf())
                                .cookie(maintainer)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"Temp\"}"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id");

        mvc.perform(delete("/api/v1/organizations/" + organizationId).with(xsrf()).cookie(maintainer))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ORG_NOT_EMPTY"));
        mvc.perform(delete("/api/v1/projects/" + projectId).with(xsrf()).cookie(maintainer));
        mvc.perform(delete("/api/v1/organizations/" + organizationId).with(xsrf()).cookie(maintainer))
                .andExpect(status().isNoContent());
    }

    private String createOrganization(Cookie owner) throws Exception {
        return JsonPath.read(
                mvc.perform(post("/api/v1/organizations")
                                .with(xsrf())
                                .cookie(owner)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"Org\"}"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id");
    }
}
