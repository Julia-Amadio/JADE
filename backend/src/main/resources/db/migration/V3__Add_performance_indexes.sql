-- V3__Add_performance_indexes.sql
CREATE INDEX idx_monitor_history_monitor_checked
    ON monitor_history(monitor_id, checked_at DESC);

CREATE INDEX idx_incidents_monitor_status
    ON incidents(monitor_id, status);

CREATE INDEX idx_monitors_last_checked
    ON monitors(last_checked);
