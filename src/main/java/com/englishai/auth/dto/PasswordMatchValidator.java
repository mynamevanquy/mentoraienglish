package com.englishai.auth.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates that {@link RegisterRequest#getPassword()} equals
 * {@link RegisterRequest#getConfirmPassword()}.
 */
public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, RegisterRequest> {

    @Override
    public boolean isValid(RegisterRequest request, ConstraintValidatorContext context) {
        if (request.getPassword() == null || request.getConfirmPassword() == null) {
            return true; // null checks handled by @NotBlank on individual fields
        }
        boolean matches = request.getPassword().equals(request.getConfirmPassword());
        if (!matches) {
            // Redirect the error to the confirmPassword field for UI binding
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("confirmPassword")
                    .addConstraintViolation();
        }
        return matches;
    }
}
