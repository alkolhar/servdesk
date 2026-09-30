/**
 * Browser sessions (ADR-0005): {@code POST /api/login} establishes a
 * server-side session held in an {@code HttpOnly} cookie; logout itself is
 * Spring Security's {@code LogoutFilter}, configured in {@code SecurityConfig}.
 */
@NullMarked
package dev.alkolhar.servdesk.session;

import org.jspecify.annotations.NullMarked;
