package dev.alkolhar.servdesk.ticket;

import jakarta.validation.constraints.NotBlank;

/**
 * {@code internal} defaults to {@code false} when omitted (Jackson's normal
 * primitive default) — the safer default, since a comment is only hidden from
 * the requester when the client explicitly asks for that.
 */
public record CommentCreateRequest(@NotBlank String body, boolean internal) {
}
