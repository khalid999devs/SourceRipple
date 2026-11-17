package io.sourceripple.shared.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

/** Serves {@code index.html} for client-side routes so deep links and refreshes work. */
@Controller
public class SpaFallbackController {

    private static final String FIRST_SEGMENT =
            "{segment:^(?!api$|actuator$|v3$|swagger-ui$|internal$|mcp$|demo$|assets$|error$)[^.]*$}";

    @GetMapping({"/" + FIRST_SEGMENT, "/" + FIRST_SEGMENT + "/**"})
    public String forwardToIndex(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!SpaRoutes.isSpaPage(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "forward:/index.html";
    }
}
