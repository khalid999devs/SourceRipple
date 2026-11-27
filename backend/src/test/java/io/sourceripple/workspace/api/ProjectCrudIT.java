package io.sourceripple.workspace.api;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.sourceripple.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class ProjectCrudIT extends AbstractIntegrationTest {

    @Test
    void fullCycleWithTheRightCodes() throws Exception {
        Cookie maintainer = registerAndLogin("MAINTAINER");
        String organizationId = createOrganization(maintainer, "Org");

        String projectId = createsProjectsWithUniqueSlugs(maintainer, organizationId);
        readsAndUpdatesTheProject(maintainer, projectId);
        archivesTheProject(maintainer, projectId);

        mvc.perform(get("/api/v1/projects/" + UUID.randomUUID()).cookie(maintainer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void reviewerMemberReadsButCannotWriteAndOutsidersSeeNotFound() throws Exception {
        Cookie maintainer = registerAndLogin("MAINTAINER");
        String organizationId = createOrganization(maintainer, "Org");
        String projectId = createProject(maintainer, organizationId, "Mobile");
        String reviewerEmail = uniqueEmail();
        register(reviewerEmail, PASSWORD, "REVIEWER").andExpect(status().isCreated());
        addMember(maintainer, organizationId, reviewerEmail).andExpect(status().isCreated());
        Cookie reviewer = sessionCookie(login(reviewerEmail, PASSWORD).andExpect(status().isOk()));

        mvc.perform(get("/api/v1/projects/" + projectId).cookie(reviewer)).andExpect(status().isOk());
        rename(reviewer, projectId, "Hijack").andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/projects/" + projectId).cookie(registerAndLogin("MAINTAINER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void membersOfAnotherOrganizationSeeNotFoundEverywhere() throws Exception {
        Cookie ownerB = registerAndLogin("MAINTAINER");
        String organizationB = createOrganization(ownerB, "Org B");
        String projectB = createProject(ownerB, organizationB, "Payments");
        Cookie memberA = registerAndLogin("MAINTAINER");
        createOrganization(memberA, "Org A");

        rename(memberA, projectB, "Taken").andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/projects/" + projectB).with(xsrf()).cookie(memberA))
                .andExpect(status().isNotFound());
        createProjectRequest(memberA, organizationB, "{\"name\": \"Intruder\"}").andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/organizations/" + organizationB + "/members").cookie(memberA))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/v1/projects/" + projectB).cookie(ownerB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Payments"))
                .andExpect(jsonPath("$.archivedAt").doesNotExist());
    }

    @Test
    void archivedProjectsLetTheOrganizationBeDeleted() throws Exception {
        Cookie maintainer = registerAndLogin("MAINTAINER");
        String organizationId = createOrganization(maintainer, "Org");
        String projectId = createProject(maintainer, organizationId, "Temp");

        mvc.perform(delete("/api/v1/organizations/" + organizationId).with(xsrf()).cookie(maintainer))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ORG_NOT_EMPTY"));

        mvc.perform(delete("/api/v1/projects/" + projectId).with(xsrf()).cookie(maintainer))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/organizations/" + organizationId).with(xsrf()).cookie(maintainer))
                .andExpect(status().isNoContent());
    }

    private String createsProjectsWithUniqueSlugs(Cookie maintainer, String organizationId) throws Exception {
        ResultActions first = createProjectRequest(
                        maintainer, organizationId, "{\"name\": \"Shop\", \"description\": \"Storefront\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("shop"));

        createProjectRequest(maintainer, organizationId, "{\"name\": \"Shop\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/v1/projects/")))
                .andExpect(jsonPath("$.slug").value("shop-2"));
        return idOf(first);
    }

    private void readsAndUpdatesTheProject(Cookie maintainer, String projectId) throws Exception {
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
    }

    private void archivesTheProject(Cookie maintainer, String projectId) throws Exception {
        mvc.perform(delete("/api/v1/projects/" + projectId).with(xsrf()).cookie(maintainer))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/projects/" + projectId).cookie(maintainer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivedAt").isNotEmpty());
    }

    private ResultActions rename(Cookie session, String projectId, String name) throws Exception {
        return mvc.perform(patch("/api/v1/projects/" + projectId)
                .with(xsrf())
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"" + name + "\"}"));
    }
}
