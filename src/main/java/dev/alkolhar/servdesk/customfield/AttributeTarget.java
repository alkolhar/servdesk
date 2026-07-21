package dev.alkolhar.servdesk.customfield;

/**
 * Which aggregate a definition applies to. Tickets only for now; CMDB
 * configuration items (#33) will add their own constant and reuse everything
 * else unchanged.
 */
public enum AttributeTarget {
	TICKET
}
