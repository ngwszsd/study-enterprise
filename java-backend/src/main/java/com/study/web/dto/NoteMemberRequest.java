package com.study.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record NoteMemberRequest(
        @NotNull Long userId,
        @Pattern(regexp = "EDITOR|VIEWER", message = "角色只能是 EDITOR 或 VIEWER") String role
) {
}
