package dev.alkolhar.servdesk.customfield;

/**
 * The value types a definition can demand. Deliberately small for v1 —
 * {@code DATE} is an ISO-8601 calendar date string, {@code ENUM} a string
 * restricted to the definition's {@code enumValues}.
 */
public enum AttributeType {
	STRING, NUMBER, BOOLEAN, DATE, ENUM
}
