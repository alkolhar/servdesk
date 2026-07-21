/**
 * Customer-defined custom fields (issue #29) — the product's core customization
 * mechanism per ADR-0002: admin-editable {@code AttributeDefinition}s say which
 * keys a target aggregate accepts; values live in the target's own
 * {@code attributes} jsonb column and are validated on every write by
 * {@code AttributeValidator}. Only tickets today; CMDB configuration items
 * (#33) attach to the same mechanism.
 */
@NullMarked
package dev.alkolhar.servdesk.customfield;

import org.jspecify.annotations.NullMarked;
