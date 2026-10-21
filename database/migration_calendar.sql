-- ============================================================================
-- Migration: Add calendar_events table
-- ============================================================================

CREATE TABLE IF NOT EXISTS calendar_events (
    id SERIAL PRIMARY KEY,
    organization_id INT NOT NULL DEFAULT 1 REFERENCES organizations(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    event_type VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    event_date DATE NOT NULL,
    event_time VARCHAR(20),
    description TEXT,
    created_by INT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_calendar_events_org_date ON calendar_events(organization_id, event_date);
CREATE INDEX IF NOT EXISTS idx_calendar_events_org_type ON calendar_events(organization_id, event_type);

GRANT ALL PRIVILEGES ON TABLE calendar_events TO bai_app_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO bai_app_user;
