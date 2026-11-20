package com.bai.inventory;

import com.bai.inventory.config.DatabaseConfig;
import com.bai.inventory.model.CalendarEvent;
import com.bai.inventory.model.Role;
import com.bai.inventory.model.User;
import com.bai.inventory.service.CalendarService;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CalendarServiceTest {

    private static CalendarService calendarService;
    private static User testUser;

    @BeforeAll
    static void setUp() {
        DatabaseConfig.initDataSource();
        calendarService = new CalendarService();

        testUser = new User();
        testUser.setId(1);
        testUser.setOrganizationId(1);
        testUser.setEmail("owner@test.com");
        testUser.setRole(Role.OWNER);
    }

    @Test
    @Order(1)
    void testCreateCalendarEvent() throws Exception {
        CalendarEvent ev = new CalendarEvent();
        ev.setTitle("JUnit Test Restock Event");
        ev.setEventType(CalendarEvent.TYPE_RESTOCK);
        ev.setEventDate(LocalDate.now());
        ev.setEventTime("10:00 AM");
        ev.setDescription("Delivery of coffee beans");

        CalendarEvent created = calendarService.createEvent(ev, testUser);
        assertNotNull(created);
        assertTrue(created.getId() > 0);
        assertEquals("JUnit Test Restock Event", created.getTitle());
        assertEquals(CalendarEvent.TYPE_RESTOCK, created.getEventType());
    }

    @Test
    @Order(2)
    void testValidationFailure() {
        CalendarEvent invalid = new CalendarEvent();
        invalid.setTitle("");
        invalid.setEventDate(LocalDate.now());

        assertThrows(CalendarService.ValidationException.class, () -> {
            calendarService.createEvent(invalid, testUser);
        });
    }

    @Test
    @Order(3)
    void testQueryEventsForDateAndMonth() {
        LocalDate today = LocalDate.now();
        List<CalendarEvent> dateEvents = calendarService.getEventsForDate(today, testUser);
        assertNotNull(dateEvents);
        assertFalse(dateEvents.isEmpty());

        List<CalendarEvent> monthEvents = calendarService.getEventsForMonth(today.getYear(), today.getMonthValue(), testUser);
        assertNotNull(monthEvents);
        assertFalse(monthEvents.isEmpty());

        Map<LocalDate, Integer> counts = calendarService.getEventCountsForMonth(today.getYear(), today.getMonthValue(), testUser);
        assertNotNull(counts);
        assertTrue(counts.containsKey(today));
    }

    @Test
    @Order(4)
    void testDeleteCalendarEvent() throws Exception {
        CalendarEvent ev = new CalendarEvent();
        ev.setTitle("Temporary Event to Delete");
        ev.setEventType(CalendarEvent.TYPE_GENERAL);
        ev.setEventDate(LocalDate.now().plusDays(5));

        CalendarEvent created = calendarService.createEvent(ev, testUser);
        assertTrue(created.getId() > 0);

        boolean deleted = calendarService.deleteEvent(created.getId(), testUser);
        assertTrue(deleted);
    }
}
