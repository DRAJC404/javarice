package com.bai.inventory.dao;

import com.bai.inventory.config.DatabaseConfig;
import com.bai.inventory.model.CalendarEvent;
import com.bai.inventory.service.AuthService;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarEventDao {

    public CalendarEvent createEvent(CalendarEvent event) throws SQLException {
        int orgId = event.getOrganizationId() > 0 ? event.getOrganizationId() : resolveOrganizationId(0);
        String sql = "INSERT INTO calendar_events (organization_id, title, event_type, event_date, event_time, description, created_by) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id, created_at";

        try (Connection conn = DatabaseConfig.getConnection(orgId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orgId);
            stmt.setString(2, event.getTitle());
            stmt.setString(3, event.getEventType());
            stmt.setDate(4, Date.valueOf(event.getEventDate()));
            stmt.setString(5, event.getEventTime());
            stmt.setString(6, event.getDescription());
            if (event.getCreatedBy() != null && event.getCreatedBy() > 0) {
                stmt.setInt(7, event.getCreatedBy());
            } else {
                stmt.setNull(7, Types.INTEGER);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    event.setId(rs.getInt("id"));
                    event.setOrganizationId(orgId);
                    Timestamp cat = rs.getTimestamp("created_at");
                    if (cat != null) event.setCreatedAt(cat.toLocalDateTime());
                }
            }
            if (!conn.getAutoCommit()) {
                conn.commit();
            }
        }
        return event;
    }

    public boolean updateEvent(CalendarEvent event) throws SQLException {
        int orgId = event.getOrganizationId() > 0 ? event.getOrganizationId() : resolveOrganizationId(0);
        String sql = "UPDATE calendar_events SET title = ?, event_type = ?, event_date = ?, event_time = ?, description = ? " +
                     "WHERE id = ? AND organization_id = ?";

        try (Connection conn = DatabaseConfig.getConnection(orgId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, event.getTitle());
            stmt.setString(2, event.getEventType());
            stmt.setDate(3, Date.valueOf(event.getEventDate()));
            stmt.setString(4, event.getEventTime());
            stmt.setString(5, event.getDescription());
            stmt.setInt(6, event.getId());
            stmt.setInt(7, orgId);
            boolean updated = stmt.executeUpdate() > 0;
            if (!conn.getAutoCommit()) {
                conn.commit();
            }
            return updated;
        }
    }

    public boolean deleteEvent(int eventId, int organizationId) throws SQLException {
        int orgId = organizationId > 0 ? organizationId : resolveOrganizationId(0);
        String sql = "DELETE FROM calendar_events WHERE id = ? AND organization_id = ?";

        try (Connection conn = DatabaseConfig.getConnection(orgId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            stmt.setInt(2, orgId);
            boolean deleted = stmt.executeUpdate() > 0;
            if (!conn.getAutoCommit()) {
                conn.commit();
            }
            return deleted;
        }
    }

    public List<CalendarEvent> findEventsByMonth(int organizationId, int year, int month) {
        int orgId = organizationId > 0 ? organizationId : resolveOrganizationId(0);
        List<CalendarEvent> list = new ArrayList<>();
        String sql = "SELECT ce.id, ce.organization_id, ce.title, ce.event_type, ce.event_date, ce.event_time, ce.description, " +
                     "ce.created_by, u.full_name AS creator_name, ce.created_at " +
                     "FROM calendar_events ce " +
                     "LEFT JOIN users u ON ce.created_by = u.id " +
                     "WHERE ce.organization_id = ? " +
                     "  AND EXTRACT(YEAR FROM ce.event_date) = ? " +
                     "  AND EXTRACT(MONTH FROM ce.event_date) = ? " +
                     "ORDER BY ce.event_date ASC, ce.event_time ASC NULLS LAST, ce.id ASC";

        try (Connection conn = DatabaseConfig.getConnection(orgId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orgId);
            stmt.setInt(2, year);
            stmt.setInt(3, month);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[CalendarEventDao] Error querying month events: " + e.getMessage());
        }
        return list;
    }

    public List<CalendarEvent> findEventsByDate(int organizationId, LocalDate date) {
        int orgId = organizationId > 0 ? organizationId : resolveOrganizationId(0);
        List<CalendarEvent> list = new ArrayList<>();
        String sql = "SELECT ce.id, ce.organization_id, ce.title, ce.event_type, ce.event_date, ce.event_time, ce.description, " +
                     "ce.created_by, u.full_name AS creator_name, ce.created_at " +
                     "FROM calendar_events ce " +
                     "LEFT JOIN users u ON ce.created_by = u.id " +
                     "WHERE ce.organization_id = ? AND ce.event_date = ? " +
                     "ORDER BY ce.event_time ASC NULLS LAST, ce.id ASC";

        try (Connection conn = DatabaseConfig.getConnection(orgId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orgId);
            stmt.setDate(2, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[CalendarEventDao] Error querying date events: " + e.getMessage());
        }
        return list;
    }

    public Map<LocalDate, Integer> countEventsByDateInMonth(int organizationId, int year, int month) {
        int orgId = organizationId > 0 ? organizationId : resolveOrganizationId(0);
        Map<LocalDate, Integer> map = new HashMap<>();
        String sql = "SELECT event_date, COUNT(*) AS cnt " +
                     "FROM calendar_events " +
                     "WHERE organization_id = ? " +
                     "  AND EXTRACT(YEAR FROM event_date) = ? " +
                     "  AND EXTRACT(MONTH FROM event_date) = ? " +
                     "GROUP BY event_date";

        try (Connection conn = DatabaseConfig.getConnection(orgId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orgId);
            stmt.setInt(2, year);
            stmt.setInt(3, month);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Date d = rs.getDate("event_date");
                    if (d != null) {
                        map.put(d.toLocalDate(), rs.getInt("cnt"));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[CalendarEventDao] Error counting events by date: " + e.getMessage());
        }
        return map;
    }

    private CalendarEvent mapRow(ResultSet rs) throws SQLException {
        CalendarEvent e = new CalendarEvent();
        e.setId(rs.getInt("id"));
        e.setOrganizationId(rs.getInt("organization_id"));
        e.setTitle(rs.getString("title"));
        e.setEventType(rs.getString("event_type"));
        Date d = rs.getDate("event_date");
        if (d != null) e.setEventDate(d.toLocalDate());
        e.setEventTime(rs.getString("event_time"));
        e.setDescription(rs.getString("description"));
        int uid = rs.getInt("created_by");
        if (!rs.wasNull()) e.setCreatedBy(uid);
        e.setCreatorName(rs.getString("creator_name"));
        Timestamp cat = rs.getTimestamp("created_at");
        if (cat != null) e.setCreatedAt(cat.toLocalDateTime());
        return e;
    }

    private int resolveOrganizationId(int fallback) {
        com.bai.inventory.model.User user = AuthService.getCurrentUser();
        if (user != null && user.getOrganizationId() > 0) {
            return user.getOrganizationId();
        }
        return fallback > 0 ? fallback : 1;
    }
}
