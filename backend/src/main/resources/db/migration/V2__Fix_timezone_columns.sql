-- V2__Fix_timezone_columns.sql
ALTER TABLE users
ALTER COLUMN created_at TYPE TIMESTAMPTZ
        USING created_at AT TIME ZONE 'UTC';

ALTER TABLE monitors
ALTER COLUMN created_at TYPE TIMESTAMPTZ
        USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN last_checked TYPE TIMESTAMPTZ
        USING last_checked AT TIME ZONE 'UTC';

ALTER TABLE monitor_history
ALTER COLUMN checked_at TYPE TIMESTAMPTZ
        USING checked_at AT TIME ZONE 'UTC';

ALTER TABLE incidents
ALTER COLUMN created_at TYPE TIMESTAMPTZ
        USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN ended_at TYPE TIMESTAMPTZ
        USING ended_at AT TIME ZONE 'UTC';
