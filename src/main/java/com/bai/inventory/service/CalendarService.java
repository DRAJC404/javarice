package com.bai.inventory.service;

import com.bai.inventory.dao.CalendarEventDao;
import com.bai.inventory.dao.StockMovementDao;
import com.bai.inventory.model.CalendarEvent;
import com.bai.inventory.model.StockMovement;
import com.bai.inventory.model.User;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CalendarService {

    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }

    private final CalendarEventDao calendarEventDao;
    private final StockMovementDao stockMovementDao;

    public CalendarService() {
        this(new CalendarEventDao(), new StockMovementDao());
    }

    public CalendarService(CalendarEventDao calendarEventDao, StockMovementDao stockMovementDao) {
        this.calendarEventDao = calendarEventDao;
        this.stockMovementDao = stockMovementDao;
    }

    public List<CalendarEvent> getEventsForMonth(int year, int month, User currentUser) {
        if (currentUser == null) return Collections.emptyList();
        int orgId = resolveOrgId(currentUser);
        return calendarEventDao.findEventsByMonth(orgId, year, month);
    }

    public List<CalendarEvent> getEventsForDate(LocalDate date, User currentUser) {
        if (currentUser == null || date == null) return Collections.emptyList();
        int orgId = resolveOrgId(currentUser);
        return calendarEventDao.findEventsByDate(orgId, date);
    }

    public List<StockMovement> getStockMovementsForDate(LocalDate date, User currentUser) {
        if (currentUser == null || date == null) return Collections.emptyList();
        int orgId = resolveOrgId(currentUser);
        return stockMovementDao.findMovementsByDate(orgId, date);
    }

    public Map<LocalDate, Integer> getMovementCountsForMonth(int year, int month, User currentUser) {
        if (currentUser == null) return Collections.emptyMap();
        int orgId = resolveOrgId(currentUser);
        return stockMovementDao.countMovementsByDateInMonth(orgId, year, month);
    }

    public Map<LocalDate, Integer> getEventCountsForMonth(int year, int month, User currentUser) {
        if (currentUser == null) return Collections.emptyMap();
        int orgId = resolveOrgId(currentUser);
        return calendarEventDao.countEventsByDateInMonth(orgId, year, month);
    }

    public CalendarEvent createEvent(CalendarEvent event, User currentUser) throws ValidationException, SQLException {
        if (currentUser == null) {
            throw new ValidationException("User must be logged in to create calendar events.");
        }
        validateEvent(event);
        int orgId = resolveOrgId(currentUser);
        event.setOrganizationId(orgId);
        event.setCreatedBy(currentUser.getId());
        return calendarEventDao.createEvent(event);
    }

    public boolean updateEvent(CalendarEvent event, User currentUser) throws ValidationException, SQLException {
        if (currentUser == null) {
            throw new ValidationException("User must be logged in to update calendar events.");
        }
        validateEvent(event);
        int orgId = resolveOrgId(currentUser);
        event.setOrganizationId(orgId);
        return calendarEventDao.updateEvent(event);
    }

    public boolean deleteEvent(int eventId, User currentUser) throws SQLException {
        if (currentUser == null) return false;
        int orgId = resolveOrgId(currentUser);
        return calendarEventDao.deleteEvent(eventId, orgId);
    }

    private void validateEvent(CalendarEvent event) throws ValidationException {
        if (event == null) {
            throw new ValidationException("Event details cannot be empty.");
        }
        if (event.getTitle() == null || event.getTitle().trim().isEmpty()) {
            throw new ValidationException("Event title is required.");
        }
        if (event.getTitle().length() > 150) {
            throw new ValidationException("Event title cannot exceed 150 characters.");
        }
        if (event.getEventDate() == null) {
            throw new ValidationException("Event date is required.");
        }
    }

    private int resolveOrgId(User user) {
        return user != null && user.getOrganizationId() > 0 ? user.getOrganizationId() : 1;
    }
}
