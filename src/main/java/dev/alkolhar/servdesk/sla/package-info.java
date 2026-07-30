/**
 * Service-level management (issue #31): per-priority {@code SlaPolicy} targets,
 * deadline derivation and clock-pausing implemented behind the {@code ticket}
 * package's {@code SlaHooks} interface, and the Quartz-driven breach scanner
 * publishing {@code SlaBreachedEvent}s. v1 runs a 24/7 clock — business-hours
 * calendars slot into {@code TicketSlaService} later without touching callers.
 */
@NullMarked
package dev.alkolhar.servdesk.sla;

import org.jspecify.annotations.NullMarked;
