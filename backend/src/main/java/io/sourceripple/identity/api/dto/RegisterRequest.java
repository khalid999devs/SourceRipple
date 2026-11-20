package io.sourceripple.identity.api.dto;

import io.sourceripple.identity.domain.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 10, max = 72) String password,
        @NotBlank @Size(max = 100) String displayName,
        @NotNull RoleName accountType) {

    @Override
    public String toString() {
        return "RegisterRequest[email=" + email + ", password=***, displayName=" + displayName + ", accountType="
                + accountType + "]";
    }
}
