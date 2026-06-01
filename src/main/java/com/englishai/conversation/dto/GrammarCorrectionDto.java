package com.englishai.conversation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single grammar correction parsed from the AI response.
 *
 * {@code original} – the text segment containing the mistake.
 * {@code corrected} – the suggested corrected version.
 * {@code explanation} – brief bilingual explanation (EN + VI).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrammarCorrectionDto {
    private String original;
    private String corrected;
    private String explanation;
}
