package io.sourceripple.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.sourceripple.TestcontainersConfiguration;
import io.sourceripple.identity.domain.RoleName;
import io.sourceripple.identity.domain.RoleRepository;
import io.sourceripple.identity.domain.User;
import io.sourceripple.identity.domain.UserRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    protected static final String PASSWORD = "correct-horse-battery";

    protected static final String XSRF_TOKEN = "integration-test-xsrf-token";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    protected static RequestPostProcessor xsrf() {
        return request -> {
            List<Cookie> cookies = new ArrayList<>();
            if (request.getCookies() != null) {
                cookies.addAll(Arrays.asList(request.getCookies()));
            }
            cookies.add(new Cookie("XSRF-TOKEN", XSRF_TOKEN));
            request.setCookies(cookies.toArray(Cookie[]::new));
            request.addHeader("X-XSRF-TOKEN", XSRF_TOKEN);
            return request;
        };
    }

    protected static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    protected static String randomAddress() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return "10." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(1, 255);
    }

    protected static Cookie sessionCookie(ResultActions result) {
        Cookie cookie = result.andReturn().getResponse().getCookie("SESSION");
        if (cookie == null) {
            throw new AssertionError("The response did not set a SESSION cookie");
        }
        return cookie;
    }

    protected ResultActions register(String email, String password, String accountType) throws Exception {
        return mvc.perform(post("/api/v1/auth/register")
                .with(xsrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s", "displayName": "Test User", "accountType": "%s"}
                        """.formatted(email, password, accountType)));
    }

    protected ResultActions login(String email, String password) throws Exception {
        return loginFrom(randomAddress(), email, password);
    }

    protected ResultActions loginFrom(String clientAddress, String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login")
                .with(xsrf())
                .with(request -> {
                    request.setRemoteAddr(clientAddress);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password)));
    }

    protected Cookie createAdminAndLogin() throws Exception {
        String email = uniqueEmail();
        User admin = new User(email, passwordEncoder.encode(PASSWORD), "Admin", Instant.now());
        admin.addRole(roleRepository.findByName(RoleName.ADMIN).orElseThrow());
        userRepository.save(admin);
        return sessionCookie(login(email, PASSWORD).andExpect(status().isOk()));
    }

    protected Cookie registerAndLogin(String accountType) throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD, accountType).andExpect(status().isCreated());
        return sessionCookie(login(email, PASSWORD).andExpect(status().isOk()));
    }

    protected ResultActions postJson(String path, Cookie session, String json) throws Exception {
        return mvc.perform(post(path)
                .with(xsrf())
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    protected String createOrganization(Cookie owner, String name) throws Exception {
        ResultActions created = postJson("/api/v1/organizations", owner, "{\"name\": \"" + name + "\"}")
                .andExpect(status().isCreated());
        return idOf(created);
    }

    protected ResultActions createProjectRequest(Cookie session, String organizationId, String json)
            throws Exception {
        return postJson("/api/v1/organizations/" + organizationId + "/projects", session, json);
    }

    protected String createProject(Cookie owner, String organizationId, String name) throws Exception {
        ResultActions created = createProjectRequest(owner, organizationId, "{\"name\": \"" + name + "\"}")
                .andExpect(status().isCreated());
        return idOf(created);
    }

    protected ResultActions addMember(Cookie owner, String organizationId, String email) throws Exception {
        return postJson(
                "/api/v1/organizations/" + organizationId + "/members", owner, "{\"email\": \"" + email + "\"}");
    }

    protected static String idOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
