/**
 * Read-only cross-subtype ticket overview ({@code GET /api/tickets}) — see
 * issue #30 and the amendment note on ADR-0001. Lives in its own subpackage
 * (not {@code ticket}) because it depends on every subtype package to resolve a
 * ticket's type and display number, and {@code ticket.incident} etc. already
 * depend on {@code ticket} — putting this in {@code ticket} would create the
 * package cycle {@code ArchitectureTest} forbids. Writes stay per-subtype.
 */
@NullMarked
package dev.alkolhar.servdesk.ticket.overview;

import org.jspecify.annotations.NullMarked;
