#!/usr/bin/env bash
#
# Seeds the fixture graph the contract-tests gate needs in order to reach anything (issue #69).
#
# Without this, every {id} in the contract is an unbounded int64 that Hypothesis draws across the
# full 64-bit range, so 28 operations spent an entire run returning 404 and their success responses
# were never schema-checked once. Each id created here is exported to $GITHUB_ENV and referenced
# from schemathesis.toml as ${SERVDESK_*}; an unset variable is a hard error there, so a break in
# this wiring fails the run instead of quietly reverting to 404s.
#
# Two rows per aggregate, and the distinction matters:
#
#   *_ID         durable  — pinned for GET and PUT, must survive the whole run
#   *_DOOMED_ID  sacrificial — pinned for DELETE, and REFERENCED BY NOTHING
#
# The second rule is load-bearing. Person carries @SQLRestriction("deleted_at IS NULL"), which
# applies to association fetches too, so deleting a row something else points at can strand the
# referrer — see #75, filed rather than risked here.
#
# Fails on the first error: a half-seeded graph would produce a confusing run rather than no run.
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
AUTH="${AUTH:-admin:admin123}"

# POSTs $2 to $1 and echoes the created id. Any non-2xx aborts the script with the body, since a
# missing fixture surfaces later as an inscrutable 404 storm rather than an obvious failure.
create() {
	local path="$1" body="$2" response http_code
	response=$(curl -sS -u "$AUTH" -X POST "$BASE_URL$path" \
		-H "Content-Type: application/json" -d "$body" -w $'\n%{http_code}')
	http_code=$(tail -n1 <<<"$response")
	response=$(sed '$d' <<<"$response")
	if [[ ! $http_code =~ ^2 ]]; then
		echo "seed: POST $path returned $http_code: $response" >&2
		exit 1
	fi
	jq -er '.id' <<<"$response"
}

# Exports NAME=value for later workflow steps and echoes it into the log, so a failing run shows
# which ids the config was pinned to.
export_id() {
	echo "$1=$2" >>"${GITHUB_ENV:-/dev/stdout}"
	echo "  $1=$2"
}

echo "Seeding contract-test fixtures against $BASE_URL"

# --- directory -------------------------------------------------------------------------------
# The requester every seeded ticket points at. A CUSTOMER, not an Agent: it must never be a
# candidate for #63's last-login-capable-Agent invariant.
PERSON_ID=$(create /api/persons \
	'{"role":"CUSTOMER","name":"Fixture Requester","email":"fixture-requester@example.com"}')
PERSON_DOOMED_ID=$(create /api/persons \
	'{"role":"CUSTOMER","name":"Fixture Doomed","email":"fixture-doomed@example.com"}')
export_id SERVDESK_PERSON_ID "$PERSON_ID"
export_id SERVDESK_PERSON_DOOMED_ID "$PERSON_DOOMED_ID"

# --- classification --------------------------------------------------------------------------
CATEGORY_ID=$(create /api/categories '{"name":"Fixture Category"}')
CATEGORY_DOOMED_ID=$(create /api/categories '{"name":"Fixture Category (doomed)"}')
export_id SERVDESK_CATEGORY_ID "$CATEGORY_ID"
export_id SERVDESK_CATEGORY_DOOMED_ID "$CATEGORY_DOOMED_ID"

# Three priorities: one carries the durable SLA policy, one carries the doomed policy (a policy is
# partial-unique per priority, so they cannot share), one is left unreferenced to be deleted.
PRIORITY_ID=$(create /api/priorities '{"name":"Fixture Priority","sortOrder":100}')
PRIORITY_ALT_ID=$(create /api/priorities '{"name":"Fixture Priority Alt","sortOrder":101}')
PRIORITY_DOOMED_ID=$(create /api/priorities '{"name":"Fixture Priority (doomed)","sortOrder":102}')
export_id SERVDESK_PRIORITY_ID "$PRIORITY_ID"
export_id SERVDESK_PRIORITY_ALT_ID "$PRIORITY_ALT_ID"
export_id SERVDESK_PRIORITY_DOOMED_ID "$PRIORITY_DOOMED_ID"

# Likewise for the matrix axes: the doomed impact/urgency appear in no PriorityDefinition, which is
# what keeps them safe to delete.
IMPACT_ID=$(create /api/impacts '{"name":"Fixture Impact","sortOrder":100}')
IMPACT_ALT_ID=$(create /api/impacts '{"name":"Fixture Impact Alt","sortOrder":101}')
IMPACT_DOOMED_ID=$(create /api/impacts '{"name":"Fixture Impact (doomed)","sortOrder":102}')
export_id SERVDESK_IMPACT_ID "$IMPACT_ID"
export_id SERVDESK_IMPACT_ALT_ID "$IMPACT_ALT_ID"
export_id SERVDESK_IMPACT_DOOMED_ID "$IMPACT_DOOMED_ID"

URGENCY_ID=$(create /api/urgencies '{"name":"Fixture Urgency","sortOrder":100}')
URGENCY_DOOMED_ID=$(create /api/urgencies '{"name":"Fixture Urgency (doomed)","sortOrder":101}')
export_id SERVDESK_URGENCY_ID "$URGENCY_ID"
export_id SERVDESK_URGENCY_DOOMED_ID "$URGENCY_DOOMED_ID"

# The two cells differ by impact, so the (impact, urgency) partial-unique index is satisfied while
# the doomed urgency stays out of both.
PRIORITY_DEFINITION_ID=$(create /api/priority-definitions \
	"{\"impactId\":$IMPACT_ID,\"urgencyId\":$URGENCY_ID,\"priorityId\":$PRIORITY_ID}")
PRIORITY_DEFINITION_DOOMED_ID=$(create /api/priority-definitions \
	"{\"impactId\":$IMPACT_ALT_ID,\"urgencyId\":$URGENCY_ID,\"priorityId\":$PRIORITY_ALT_ID}")
export_id SERVDESK_PRIORITY_DEFINITION_ID "$PRIORITY_DEFINITION_ID"
export_id SERVDESK_PRIORITY_DEFINITION_DOOMED_ID "$PRIORITY_DEFINITION_DOOMED_ID"

# --- SLA -------------------------------------------------------------------------------------
SLA_POLICY_ID=$(create /api/sla-policies \
	"{\"priorityId\":$PRIORITY_ID,\"responseMinutes\":30,\"resolutionMinutes\":240}")
SLA_POLICY_DOOMED_ID=$(create /api/sla-policies \
	"{\"priorityId\":$PRIORITY_ALT_ID,\"responseMinutes\":60,\"resolutionMinutes\":480}")
export_id SERVDESK_SLA_POLICY_ID "$SLA_POLICY_ID"
export_id SERVDESK_SLA_POLICY_DOOMED_ID "$SLA_POLICY_DOOMED_ID"

# --- custom fields ---------------------------------------------------------------------------
# required:false deliberately. A required attribute definition would make every subsequent ticket
# create fail write-time validation, which would defeat the ticket fixtures below.
#
# The keys are snake_case because AttributeDefinitionCreateRequest constrains them to a machine name
# (@Pattern("[a-z][a-z0-9_]*")) — camelCase is a 400.
ATTRIBUTE_DEFINITION_ID=$(create /api/attribute-definitions \
	'{"target":"TICKET","key":"fixture_field","label":"Fixture Field","type":"STRING","required":false}')
ATTRIBUTE_DEFINITION_DOOMED_ID=$(create /api/attribute-definitions \
	'{"target":"TICKET","key":"fixture_doomed_field","label":"Fixture Doomed Field","type":"STRING","required":false}')
export_id SERVDESK_ATTRIBUTE_DEFINITION_ID "$ATTRIBUTE_DEFINITION_ID"
export_id SERVDESK_ATTRIBUTE_DEFINITION_DOOMED_ID "$ATTRIBUTE_DEFINITION_DOOMED_ID"

# --- tickets ----------------------------------------------------------------------------------
# Each subtype shares its id with the underlying Ticket row (@OneToOne @MapsId, ADR-0001), so the
# durable incident's id is also the id /api/tickets/{id} and the comment endpoints are pinned to.
# Sets SEEDED_SUBTYPE_ID rather than echoing it: the function also logs, and a caller capturing
# stdout would have to separate the two by position.
seed_subtype() {
	local path="$1" name="$2" id doomed_id
	id=$(create "$path" "{\"subject\":\"Fixture $name\",\"requesterId\":$PERSON_ID,\"categoryId\":$CATEGORY_ID,\"impactId\":$IMPACT_ID,\"urgencyId\":$URGENCY_ID}")
	doomed_id=$(create "$path" "{\"subject\":\"Fixture $name (doomed)\",\"requesterId\":$PERSON_ID}")
	export_id "SERVDESK_${name}_ID" "$id"
	export_id "SERVDESK_${name}_DOOMED_ID" "$doomed_id"
	SEEDED_SUBTYPE_ID="$id"
}

seed_subtype /api/incidents INCIDENT
INCIDENT_ID="$SEEDED_SUBTYPE_ID"
seed_subtype /api/problems PROBLEM
seed_subtype /api/changes CHANGE
seed_subtype /api/service-requests SERVICE_REQUEST

export_id SERVDESK_TICKET_ID "$INCIDENT_ID"

echo "Seeding complete."
