-- ITIL-style starter reference data (#101, scope decided in #97): a fresh deployment derives a
-- priority and SLA deadlines from its very first ticket without any admin setup. These are ordinary
-- rows, editable and soft-deletable through the existing admin APIs like any other.
--
-- Names are numbered ("1 - High", "P1 - Critical") rather than bare ("High", "Critical"): the
-- impact/urgency/priority names are partial-unique, and the integration tests create their own
-- rows under bare names — a seeded "High" would turn those creates into 409s.
--
-- sortOrder: lower = more severe, as the contract documents.

INSERT INTO impact (name, sort_order, created_by, updated_by)
VALUES ('1 - High', 0, 'system', 'system'),
       ('2 - Medium', 1, 'system', 'system'),
       ('3 - Low', 2, 'system', 'system');

INSERT INTO urgency (name, sort_order, created_by, updated_by)
VALUES ('1 - High', 0, 'system', 'system'),
       ('2 - Medium', 1, 'system', 'system'),
       ('3 - Low', 2, 'system', 'system');

INSERT INTO priority (name, sort_order, created_by, updated_by)
VALUES ('P1 - Critical', 0, 'system', 'system'),
       ('P2 - High', 1, 'system', 'system'),
       ('P3 - Moderate', 2, 'system', 'system'),
       ('P4 - Low', 3, 'system', 'system');

-- The classic 3x3 matrix, every cell mapped:
--
--                  urgency High  Medium  Low
--   impact High             P1    P2      P3
--   impact Medium           P2    P3      P4
--   impact Low              P3    P4      P4
INSERT INTO priority_definition (impact_id, urgency_id, priority_id, created_by, updated_by)
SELECT i.id, u.id, p.id, 'system', 'system'
FROM (VALUES ('1 - High', '1 - High', 'P1 - Critical'),
             ('1 - High', '2 - Medium', 'P2 - High'),
             ('1 - High', '3 - Low', 'P3 - Moderate'),
             ('2 - Medium', '1 - High', 'P2 - High'),
             ('2 - Medium', '2 - Medium', 'P3 - Moderate'),
             ('2 - Medium', '3 - Low', 'P4 - Low'),
             ('3 - Low', '1 - High', 'P3 - Moderate'),
             ('3 - Low', '2 - Medium', 'P4 - Low'),
             ('3 - Low', '3 - Low', 'P4 - Low')) AS m (impact, urgency, priority)
         JOIN impact i ON i.name = m.impact
         JOIN urgency u ON u.name = m.urgency
         JOIN priority p ON p.name = m.priority;

-- Minutes on v1's 24/7 clock (no business-hours calendars yet): 15 min / 4 h, 1 h / 8 h,
-- 4 h / 3 days, 1 day / 5 days.
INSERT INTO sla_policy (priority_id, response_minutes, resolution_minutes, created_by, updated_by)
SELECT p.id, s.response_minutes, s.resolution_minutes, 'system', 'system'
FROM (VALUES ('P1 - Critical', 15, 240),
             ('P2 - High', 60, 480),
             ('P3 - Moderate', 240, 4320),
             ('P4 - Low', 1440, 7200)) AS s (priority, response_minutes, resolution_minutes)
         JOIN priority p ON p.name = s.priority;

INSERT INTO category (name, created_by, updated_by)
VALUES ('Access & Accounts', 'system', 'system'),
       ('Hardware', 'system', 'system'),
       ('Software', 'system', 'system'),
       ('Network', 'system', 'system'),
       ('Email & Collaboration', 'system', 'system');
