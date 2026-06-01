package com.englishai.user.dto;

import com.englishai.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminUserUpdateRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 150, message = "Họ tên không vượt quá 150 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 255, message = "Email không vượt quá 255 ký tự")
    private String email;

    @NotNull(message = "Vui lòng chọn vai trò")
    private Role role;

    private boolean enabled;

    public AdminUserUpdateRequest(String fullName, String email, Role role, boolean enabled) {
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.enabled = enabled;
    }
}
