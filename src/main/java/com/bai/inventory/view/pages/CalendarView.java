package com.bai.inventory.view.pages;

import com.bai.inventory.model.CalendarEvent;
import com.bai.inventory.model.StockMovement;
import com.bai.inventory.model.User;
import com.bai.inventory.service.AuthService;
import com.bai.inventory.service.CalendarService;
import com.bai.inventory.util.AppExecutor;
import com.bai.inventory.view.Disposable;
import com.bai.inventory.view.component.AppDialog;
import com.bai.inventory.view.component.FormField;
import com.bai.inventory.view.component.PageHeader;
import com.bai.inventory.view.component.StatCard;
import com.bai.inventory.view.navigation.ViewNavigator;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

public class CalendarView extends BorderPane implements Disposable {

    private static final DateTimeFormatter MONTH_YEAR_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final DateTimeFormatter FULL_DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    private static final String SVG_CALENDAR = "M19 4h-1V2h-2v2H8V2H6v2H5c-1.11 0-1.99.9-1.99 2L3 20c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V10h14v10zm0-12H5V6h14v2z";
    private static final String SVG_PLUS = "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z";
    private static final String SVG_CHEVRON_LEFT = "M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z";
    private static final String SVG_CHEVRON_RIGHT = "M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z";
    private static final String SVG_DELETE = "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z";
    private static final String SVG_CLOCK = "M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z";
    private static final String SVG_PACKAGE = "M20 7l-8-4-8 4 8 4 8-4zm-8 6l-8-4v8l8 4 8-4v-8l-8 4z";

    private final CalendarService calendarService;
    private volatile boolean isDisposed = false;

    private YearMonth currentYearMonth;
    private LocalDate selectedDate;
    private String selectedFilterType = "ALL";

    private Label monthYearLabel;
    private GridPane calendarGrid;
    private ComboBox<String> filterCombo;

    private StatCard statTotalEventsCard;
    private StatCard statRestocksCard;
    private StatCard statAuditsCard;
    private StatCard statMovementsCard;

    private Label selectedDateHeading;
    private VBox scheduledEventsBox;
    private VBox stockMovementsBox;

    private List<CalendarEvent> currentMonthEvents = new ArrayList<>();
    private Map<LocalDate, Integer> currentMonthMovementCounts = new HashMap<>();

    public CalendarView() {
        this.calendarService = new CalendarService();
        this.currentYearMonth = YearMonth.now();
        this.selectedDate = LocalDate.now();

        buildUI();
        loadMonthData();
    }

    private void buildUI() {
        getStyleClass().add("content-pane");

        VBox rootBox = new VBox(20);
        rootBox.setPadding(new Insets(24));
        rootBox.setFillWidth(true);

        PageHeader header = new PageHeader(
                "Operations & Inventory Calendar",
                "Schedule restocks, stock audits, promotions, and track daily inventory activities."
        );

        HBox statCardsRow = buildStatCardsRow();
        HBox toolbar = buildToolbar();
        Node mainContent = buildMainContent();

        rootBox.getChildren().addAll(header, statCardsRow, toolbar, mainContent);
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        ScrollPane scrollWrapper = new ScrollPane(rootBox);
        scrollWrapper.setFitToWidth(true);
        scrollWrapper.getStyleClass().add("scroll-pane");
        scrollWrapper.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        setCenter(scrollWrapper);
    }

    private HBox buildStatCardsRow() {
        statTotalEventsCard = new StatCard("Month Events", "0", "Scheduled this month", StatCard.StatType.PRIMARY);
        statRestocksCard = new StatCard("Restocks / Orders", "0", "Deliveries planned", StatCard.StatType.SUCCESS);
        statAuditsCard = new StatCard("Stock Audits", "0", "Counts & verifications", StatCard.StatType.INFO);
        statMovementsCard = new StatCard("Stock Movements", "0", "Actual activity recorded", StatCard.StatType.WARNING);

        HBox row = new HBox(16, statTotalEventsCard, statRestocksCard, statAuditsCard, statMovementsCard);
        HBox.setHgrow(statTotalEventsCard, Priority.ALWAYS);
        HBox.setHgrow(statRestocksCard, Priority.ALWAYS);
        HBox.setHgrow(statAuditsCard, Priority.ALWAYS);
        HBox.setHgrow(statMovementsCard, Priority.ALWAYS);
        return row;
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 0, 6, 0));

        Button prevBtn = new Button();
        prevBtn.getStyleClass().addAll("btn-secondary", "btn-icon");
        SVGPath prevIcon = new SVGPath();
        prevIcon.setContent(SVG_CHEVRON_LEFT);
        prevIcon.setStyle("-fx-fill: -text-body;");
        prevBtn.setGraphic(prevIcon);
        prevBtn.setTooltip(new Tooltip("Previous Month"));
        prevBtn.setOnAction(e -> {
            currentYearMonth = currentYearMonth.minusMonths(1);
            loadMonthData();
        });

        Button nextBtn = new Button();
        nextBtn.getStyleClass().addAll("btn-secondary", "btn-icon");
        SVGPath nextIcon = new SVGPath();
        nextIcon.setContent(SVG_CHEVRON_RIGHT);
        nextIcon.setStyle("-fx-fill: -text-body;");
        nextBtn.setGraphic(nextIcon);
        nextBtn.setTooltip(new Tooltip("Next Month"));
        nextBtn.setOnAction(e -> {
            currentYearMonth = currentYearMonth.plusMonths(1);
            loadMonthData();
        });

        monthYearLabel = new Label();
        monthYearLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -text-main; -fx-min-width: 170px;");
        updateMonthYearLabel();

        Button todayBtn = new Button("Today");
        todayBtn.getStyleClass().add("btn-secondary");
        todayBtn.setOnAction(e -> {
            currentYearMonth = YearMonth.now();
            selectedDate = LocalDate.now();
            loadMonthData();
        });

        HBox navGroup = new HBox(8, prevBtn, nextBtn, todayBtn, monthYearLabel);
        navGroup.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        filterCombo = new ComboBox<>();
        filterCombo.getItems().addAll("All Events", "Restock & Delivery", "Inventory Audit", "Promotion / Sale", "Expiry Check", "General Note");
        filterCombo.setValue("All Events");
        filterCombo.setPrefWidth(170);
        filterCombo.setOnAction(e -> {
            String val = filterCombo.getValue();
            if (val == null || val.startsWith("All")) {
                selectedFilterType = "ALL";
            } else if (val.contains("Restock")) {
                selectedFilterType = CalendarEvent.TYPE_RESTOCK;
            } else if (val.contains("Audit")) {
                selectedFilterType = CalendarEvent.TYPE_AUDIT;
            } else if (val.contains("Promotion")) {
                selectedFilterType = CalendarEvent.TYPE_PROMOTION;
            } else if (val.contains("Expiry")) {
                selectedFilterType = CalendarEvent.TYPE_EXPIRY;
            } else {
                selectedFilterType = CalendarEvent.TYPE_GENERAL;
            }
            renderCalendarGrid();
        });

        Button addEventBtn = new Button("Add Event");
        addEventBtn.getStyleClass().add("btn-primary");
        SVGPath addIcon = new SVGPath();
        addIcon.setContent(SVG_PLUS);
        addIcon.setStyle("-fx-fill: white;");
        addEventBtn.setGraphic(addIcon);
        addEventBtn.setOnAction(e -> showEventDialog(null, selectedDate));

        bar.getChildren().addAll(navGroup, spacer, new Label("Filter:"), filterCombo, addEventBtn);
        return bar;
    }

    private Node buildMainContent() {
        HBox container = new HBox(20);
        container.setFillHeight(true);
        VBox.setVgrow(container, Priority.ALWAYS);

        VBox gridCard = new VBox(12);
        gridCard.getStyleClass().add("card");
        gridCard.setPadding(new Insets(16));
        HBox.setHgrow(gridCard, Priority.ALWAYS);

        GridPane daysHeader = new GridPane();
        daysHeader.setHgap(8);
        for (int i = 0; i < 7; i++) {
            DayOfWeek dow = DayOfWeek.of(i == 6 ? 7 : i + 1); 
            Label dowLabel = new Label(dow.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase());
            dowLabel.setAlignment(Pos.CENTER);
            dowLabel.setMaxWidth(Double.MAX_VALUE);
            dowLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: -text-muted; -fx-font-size: 12px; -fx-padding: 4 0;");
            GridPane.setHgrow(dowLabel, Priority.ALWAYS);
            daysHeader.add(dowLabel, i, 0);

            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 7.0);
            daysHeader.getColumnConstraints().add(col);
        }

        calendarGrid = new GridPane();
        calendarGrid.setHgap(8);
        calendarGrid.setVgap(8);
        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 7.0);
            calendarGrid.getColumnConstraints().add(col);
        }
        VBox.setVgrow(calendarGrid, Priority.ALWAYS);

        gridCard.getChildren().addAll(daysHeader, new Separator(), calendarGrid);

        Node sidePanel = buildSidePanel();

        container.getChildren().addAll(gridCard, sidePanel);
        return container;
    }

    private Node buildSidePanel() {
        VBox panel = new VBox(16);
        panel.getStyleClass().add("card");
        panel.setPadding(new Insets(20));
        panel.setMinWidth(360);
        panel.setPrefWidth(380);
        panel.setMaxWidth(420);

        HBox headerBox = new HBox(8);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label subtitle = new Label("SCHEDULE & ACTIVITIES");
        subtitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: -text-muted;");

        selectedDateHeading = new Label(selectedDate.format(FULL_DATE_FMT));
        selectedDateHeading.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: -text-main;");
        selectedDateHeading.setWrapText(true);
        titleBox.getChildren().addAll(subtitle, selectedDateHeading);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button quickAddBtn = new Button();
        quickAddBtn.getStyleClass().addAll("btn-secondary", "btn-icon");
        SVGPath addIco = new SVGPath();
        addIco.setContent(SVG_PLUS);
        addIco.setStyle("-fx-fill: -text-body;");
        quickAddBtn.setGraphic(addIco);
        quickAddBtn.setTooltip(new Tooltip("Add Event for Selected Date"));
        quickAddBtn.setOnAction(e -> showEventDialog(null, selectedDate));

        headerBox.getChildren().addAll(titleBox, spacer, quickAddBtn);

        Separator sep1 = new Separator();

        Label eventsTitle = new Label("Scheduled Events");
        eventsTitle.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: -text-main;");

        scheduledEventsBox = new VBox(10);
        scheduledEventsBox.setFillWidth(true);

        Separator sep2 = new Separator();

        Label activityTitle = new Label("Inventory Movement History");
        activityTitle.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: -text-main;");

        stockMovementsBox = new VBox(8);
        stockMovementsBox.setFillWidth(true);

        VBox contentBox = new VBox(12, eventsTitle, scheduledEventsBox, sep2, activityTitle, stockMovementsBox);

        ScrollPane sideScroll = new ScrollPane(contentBox);
        sideScroll.setFitToWidth(true);
        sideScroll.getStyleClass().add("scroll-pane");
        sideScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(sideScroll, Priority.ALWAYS);

        panel.getChildren().addAll(headerBox, sep1, sideScroll);
        return panel;
    }

    private void updateMonthYearLabel() {
        if (monthYearLabel != null && currentYearMonth != null) {
            monthYearLabel.setText(currentYearMonth.format(MONTH_YEAR_FMT));
        }
    }

    private void loadMonthData() {
        updateMonthYearLabel();
        User currentUser = AuthService.getCurrentUser();
        if (currentUser == null) return;

        int year = currentYearMonth.getYear();
        int month = currentYearMonth.getMonthValue();

        AppExecutor.execute(() -> {
            if (isDisposed) return;
            List<CalendarEvent> events = calendarService.getEventsForMonth(year, month, currentUser);
            Map<LocalDate, Integer> movements = calendarService.getMovementCountsForMonth(year, month, currentUser);

            Platform.runLater(() -> {
                if (isDisposed) return;
                this.currentMonthEvents = events != null ? events : new ArrayList<>();
                this.currentMonthMovementCounts = movements != null ? movements : new HashMap<>();

                updateKpiStats();
                renderCalendarGrid();
                loadSelectedDateDetails();
            });
        });
    }

    private void updateKpiStats() {
        int totalEvents = currentMonthEvents.size();
        long restocks = currentMonthEvents.stream().filter(e -> CalendarEvent.TYPE_RESTOCK.equalsIgnoreCase(e.getEventType())).count();
        long audits = currentMonthEvents.stream().filter(e -> CalendarEvent.TYPE_AUDIT.equalsIgnoreCase(e.getEventType())).count();
        int totalMovements = currentMonthMovementCounts.values().stream().mapToInt(Integer::intValue).sum();

        statTotalEventsCard.setValue(String.valueOf(totalEvents));
        statRestocksCard.setValue(String.valueOf(restocks));
        statAuditsCard.setValue(String.valueOf(audits));
        statMovementsCard.setValue(String.valueOf(totalMovements));
    }

    private void renderCalendarGrid() {
        calendarGrid.getChildren().clear();

        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int dayOfWeekFirst = firstOfMonth.getDayOfWeek().getValue(); // 1 = Monday, 7 = Sunday
        LocalDate gridStartDate = firstOfMonth.minusDays(dayOfWeekFirst - 1);

        LocalDate today = LocalDate.now();
        LocalDate cursor = gridStartDate;

        for (int row = 0; row < 6; row++) {
            RowConstraints rowConstraints = new RowConstraints();
            rowConstraints.setVgrow(Priority.ALWAYS);
            rowConstraints.setMinHeight(85);
            if (calendarGrid.getRowConstraints().size() <= row) {
                calendarGrid.getRowConstraints().add(rowConstraints);
            } else {
                calendarGrid.getRowConstraints().set(row, rowConstraints);
            }

            for (int col = 0; col < 7; col++) {
                final LocalDate cellDate = cursor;
                Node cellNode = buildDayCell(cellDate, today);
                calendarGrid.add(cellNode, col, row);
                cursor = cursor.plusDays(1);
            }
        }
    }

    private Node buildDayCell(LocalDate date, LocalDate today) {
        VBox cell = new VBox(4);
        cell.setPadding(new Insets(6, 8, 6, 8));
        cell.setFillWidth(true);
        cell.setPrefHeight(90);

        boolean isCurrentMonth = (date.getMonth() == currentYearMonth.getMonth() && date.getYear() == currentYearMonth.getYear());
        boolean isToday = date.equals(today);
        boolean isSelected = date.equals(selectedDate);

        StringBuilder style = new StringBuilder();
        style.append("-fx-background-radius: 8px; -fx-cursor: hand; ");

        if (isSelected) {
            style.append("-fx-background-color: #EFF6FF; -fx-border-color: #3B82F6; -fx-border-width: 2px; -fx-border-radius: 8px; ");
        } else if (isToday) {
            style.append("-fx-background-color: #F8FAFC; -fx-border-color: #93C5FD; -fx-border-width: 1.5px; -fx-border-radius: 8px; ");
        } else if (!isCurrentMonth) {
            style.append("-fx-background-color: #F8FAFC; -fx-border-color: #F1F5F9; -fx-border-width: 1px; -fx-border-radius: 8px; ");
        } else {
            style.append("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-width: 1px; -fx-border-radius: 8px; ");
        }
        cell.setStyle(style.toString());

        HBox topBar = new HBox(4);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label dateNum = new Label(String.valueOf(date.getDayOfMonth()));
        if (isToday) {
            dateNum.setStyle("-fx-font-weight: 800; -fx-font-size: 12px; -fx-text-fill: white; -fx-background-color: -primary; -fx-background-radius: 12px; -fx-padding: 1 6;");
        } else if (!isCurrentMonth) {
            dateNum.setStyle("-fx-font-weight: 500; -fx-font-size: 12px; -fx-text-fill: -text-disabled;");
        } else {
            dateNum.setStyle("-fx-font-weight: 700; -fx-font-size: 12px; -fx-text-fill: -text-main;");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar.getChildren().addAll(dateNum, spacer);

        Integer movCount = currentMonthMovementCounts.get(date);
        if (movCount != null && movCount > 0) {
            Label movePill = new Label(movCount + " mv");
            movePill.setStyle("-fx-font-size: 10px; -fx-font-weight: 600; -fx-text-fill: #475569; -fx-background-color: #F1F5F9; -fx-padding: 1 4; -fx-background-radius: 4;");
            movePill.setTooltip(new Tooltip(movCount + " stock movement(s) on " + date.format(DateTimeFormatter.ISO_LOCAL_DATE)));
            topBar.getChildren().add(movePill);
        }

        VBox eventsPillsBox = new VBox(3);
        eventsPillsBox.setFillWidth(true);

        List<CalendarEvent> dayEvents = currentMonthEvents.stream()
                .filter(e -> date.equals(e.getEventDate()))
                .filter(e -> "ALL".equals(selectedFilterType) || selectedFilterType.equalsIgnoreCase(e.getEventType()))
                .toList();

        int maxVisible = 2;
        int count = 0;
        for (CalendarEvent ev : dayEvents) {
            if (count < maxVisible) {
                Label pill = new Label(ev.getTitle());
                pill.setMaxWidth(Double.MAX_VALUE);
                pill.setStyle(String.format(
                        "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 10px; -fx-font-weight: 600; -fx-padding: 2 5; -fx-background-radius: 4px; -fx-border-color: %s; -fx-border-radius: 4px;",
                        ev.getTypeBgColor(), ev.getTypeTextColor(), ev.getTypeBorderColor()
                ));
                eventsPillsBox.getChildren().add(pill);
            }
            count++;
        }
        if (count > maxVisible) {
            Label moreLabel = new Label("+" + (count - maxVisible) + " more");
            moreLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-muted; -fx-font-weight: 600; -fx-padding: 1 3;");
            eventsPillsBox.getChildren().add(moreLabel);
        }

        cell.getChildren().addAll(topBar, eventsPillsBox);

        cell.setOnMouseClicked(e -> {
            this.selectedDate = date;
            renderCalendarGrid();
            loadSelectedDateDetails();
        });

        return cell;
    }

    private void loadSelectedDateDetails() {
        if (selectedDateHeading != null) {
            selectedDateHeading.setText(selectedDate.format(FULL_DATE_FMT));
        }

        User currentUser = AuthService.getCurrentUser();
        if (currentUser == null) return;

        AppExecutor.execute(() -> {
            if (isDisposed) return;
            List<CalendarEvent> events = calendarService.getEventsForDate(selectedDate, currentUser);
            List<StockMovement> movements = calendarService.getStockMovementsForDate(selectedDate, currentUser);

            Platform.runLater(() -> {
                if (isDisposed) return;
                renderSelectedEvents(events);
                renderSelectedMovements(movements);
            });
        });
    }

    private void renderSelectedEvents(List<CalendarEvent> events) {
        scheduledEventsBox.getChildren().clear();

        if (events == null || events.isEmpty()) {
            VBox empty = new VBox(6);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(14));
            empty.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 8; -fx-border-color: #E2E8F0; -fx-border-radius: 8;");
            Label emptyLbl = new Label("No events scheduled");
            emptyLbl.setStyle("-fx-text-fill: -text-muted; -fx-font-size: 12px;");
            Button addPromptBtn = new Button("+ Add Event");
            addPromptBtn.getStyleClass().add("btn-secondary");
            addPromptBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
            addPromptBtn.setOnAction(e -> showEventDialog(null, selectedDate));
            empty.getChildren().addAll(emptyLbl, addPromptBtn);
            scheduledEventsBox.getChildren().add(empty);
            return;
        }

        for (CalendarEvent ev : events) {
            VBox card = new VBox(6);
            card.setPadding(new Insets(10, 12, 10, 12));
            card.setStyle(String.format(
                    "-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: %s; -fx-border-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.03), 4, 0, 0, 1);",
                    ev.getTypeBorderColor()
            ));

            HBox cardTop = new HBox(6);
            cardTop.setAlignment(Pos.CENTER_LEFT);

            Label badge = new Label(ev.getTypeDisplayName());
            badge.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 2 6; -fx-background-radius: 4;",
                    ev.getTypeBgColor(), ev.getTypeTextColor()
            ));

            if (ev.getEventTime() != null && !ev.getEventTime().isBlank()) {
                Label timeLbl = new Label(ev.getEventTime());
                timeLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted; -fx-font-weight: 600;");
                cardTop.getChildren().addAll(badge, timeLbl);
            } else {
                cardTop.getChildren().add(badge);
            }

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            Button delBtn = new Button();
            delBtn.getStyleClass().addAll("btn-secondary", "btn-icon");
            delBtn.setStyle("-fx-padding: 3; -fx-min-width: 24; -fx-min-height: 24; -fx-pref-width: 24; -fx-pref-height: 24;");
            SVGPath delIcon = new SVGPath();
            delIcon.setContent(SVG_DELETE);
            delIcon.setStyle("-fx-fill: -danger;");
            delBtn.setGraphic(delIcon);
            delBtn.setTooltip(new Tooltip("Delete Event"));
            delBtn.setOnAction(e -> confirmDeleteEvent(ev));

            cardTop.getChildren().addAll(sp, delBtn);

            Label titleLabel = new Label(ev.getTitle());
            titleLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: -text-main;");
            titleLabel.setWrapText(true);

            card.getChildren().addAll(cardTop, titleLabel);

            if (ev.getDescription() != null && !ev.getDescription().isBlank()) {
                Label descLabel = new Label(ev.getDescription());
                descLabel.setStyle("-fx-text-fill: -text-body; -fx-font-size: 12px;");
                descLabel.setWrapText(true);
                card.getChildren().add(descLabel);
            }

            if (ev.getCreatorName() != null && !ev.getCreatorName().isBlank()) {
                Label creatorLbl = new Label("By " + ev.getCreatorName());
                creatorLbl.setStyle("-fx-text-fill: -text-disabled; -fx-font-size: 10px;");
                card.getChildren().add(creatorLbl);
            }

            scheduledEventsBox.getChildren().add(card);
        }
    }

    private void renderSelectedMovements(List<StockMovement> movements) {
        stockMovementsBox.getChildren().clear();

        if (movements == null || movements.isEmpty()) {
            VBox empty = new VBox(6);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(12));
            empty.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 8; -fx-border-color: #E2E8F0; -fx-border-radius: 8;");
            Label emptyLbl = new Label("No stock movements on this date");
            emptyLbl.setStyle("-fx-text-fill: -text-muted; -fx-font-size: 12px;");
            empty.getChildren().add(emptyLbl);
            stockMovementsBox.getChildren().add(empty);
            return;
        }

        int max = 8;
        int count = 0;
        for (StockMovement sm : movements) {
            if (count >= max) break;
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 8, 6, 8));
            row.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 6; -fx-border-color: #F1F5F9; -fx-border-radius: 6;");

            String type = sm.getMovementType() != null ? sm.getMovementType().toUpperCase() : "MOVE";
            Label typeTag = new Label(type);
            if ("IN".equals(type)) {
                typeTag.setStyle("-fx-background-color: #ECFDF5; -fx-text-fill: #065F46; -fx-font-size: 9px; -fx-font-weight: 700; -fx-padding: 1 5; -fx-background-radius: 3;");
            } else if ("OUT".equals(type)) {
                typeTag.setStyle("-fx-background-color: #FEF2F2; -fx-text-fill: #991B1B; -fx-font-size: 9px; -fx-font-weight: 700; -fx-padding: 1 5; -fx-background-radius: 3;");
            } else {
                typeTag.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: #1E40AF; -fx-font-size: 9px; -fx-font-weight: 700; -fx-padding: 1 5; -fx-background-radius: 3;");
            }

            VBox infoBox = new VBox(2);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            String pName = sm.getProductName() != null ? sm.getProductName() : ("Product #" + sm.getProductId());
            Label prodLabel = new Label(pName);
            prodLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 12px; -fx-text-fill: -text-main;");

            String timeStr = sm.getCreatedAt() != null ? sm.getCreatedAt().format(TIME_FMT) : "";
            String reasonStr = sm.getReason() != null ? sm.getReason() : "";
            Label subInfo = new Label(reasonStr + (timeStr.isEmpty() ? "" : " • " + timeStr));
            subInfo.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-muted;");

            infoBox.getChildren().addAll(prodLabel, subInfo);

            String qtyPrefix = "IN".equalsIgnoreCase(type) ? "+" : ("OUT".equalsIgnoreCase(type) ? "-" : "");
            Label qtyLabel = new Label(qtyPrefix + sm.getQuantity());
            qtyLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 12px; -fx-text-fill: " + ("IN".equalsIgnoreCase(type) ? "#059669" : "#DC2626") + ";");

            row.getChildren().addAll(typeTag, infoBox, qtyLabel);
            stockMovementsBox.getChildren().add(row);
            count++;
        }

        if (movements.size() > max) {
            Label extraLbl = new Label("+" + (movements.size() - max) + " more movements recorded");
            extraLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted; -fx-padding: 4 2;");
            stockMovementsBox.getChildren().add(extraLbl);
        }
    }

    private void showEventDialog(CalendarEvent existing, LocalDate defaultDate) {
        AppDialog dialog = new AppDialog(existing == null ? "New Calendar Event" : "Edit Calendar Event", 460);

        TextField titleField = new TextField();
        titleField.setPromptText("e.g. Restock shipment from Bean Supplier");
        if (existing != null) titleField.setText(existing.getTitle());
        FormField titleForm = new FormField("Event Title", titleField, true);

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Restock & Delivery", "Inventory Audit", "Promotion / Sale", "Expiry Check", "General Note");
        if (existing != null) {
            typeCombo.setValue(existing.getTypeDisplayName());
        } else {
            typeCombo.setValue("Restock & Delivery");
        }
        typeCombo.setMaxWidth(Double.MAX_VALUE);
        FormField typeForm = new FormField("Event Type", typeCombo, true);

        DatePicker datePicker = new DatePicker(existing != null ? existing.getEventDate() : (defaultDate != null ? defaultDate : LocalDate.now()));
        datePicker.setMaxWidth(Double.MAX_VALUE);
        FormField dateForm = new FormField("Event Date", datePicker, true);

        TextField timeField = new TextField();
        timeField.setPromptText("e.g. 09:30 AM or 14:00 (optional)");
        if (existing != null && existing.getEventTime() != null) timeField.setText(existing.getEventTime());
        FormField timeForm = new FormField("Time (Optional)", timeField, false, "Leave empty for all-day events");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Add details, delivery notes, contact person, or items list...");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);
        if (existing != null && existing.getDescription() != null) descArea.setText(existing.getDescription());
        FormField descForm = new FormField("Notes / Description", descArea);

        dialog.setBody(titleForm, typeForm, dateForm, timeForm, descForm);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-dialog-cancel");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button("Save Event");
        saveBtn.getStyleClass().add("btn-dialog-confirm");
        saveBtn.setOnAction(e -> {
            String title = titleField.getText();
            if (title == null || title.trim().isEmpty()) {
                titleForm.setError("Event title is required.");
                return;
            }
            titleForm.clearError();

            LocalDate evDate = datePicker.getValue();
            if (evDate == null) {
                dateForm.setError("Please select a date.");
                return;
            }
            dateForm.clearError();

            String selType = typeCombo.getValue();
            String typeCode = CalendarEvent.TYPE_GENERAL;
            if (selType != null) {
                if (selType.contains("Restock")) typeCode = CalendarEvent.TYPE_RESTOCK;
                else if (selType.contains("Audit")) typeCode = CalendarEvent.TYPE_AUDIT;
                else if (selType.contains("Promotion")) typeCode = CalendarEvent.TYPE_PROMOTION;
                else if (selType.contains("Expiry")) typeCode = CalendarEvent.TYPE_EXPIRY;
            }

            CalendarEvent ev = existing != null ? existing : new CalendarEvent();
            ev.setTitle(title.trim());
            ev.setEventType(typeCode);
            ev.setEventDate(evDate);
            ev.setEventTime(timeField.getText() != null ? timeField.getText().trim() : "");
            ev.setDescription(descArea.getText() != null ? descArea.getText().trim() : "");

            User currentUser = AuthService.getCurrentUser();
            saveBtn.setDisable(true);

            AppExecutor.execute(() -> {
                try {
                    if (existing == null) {
                        calendarService.createEvent(ev, currentUser);
                    } else {
                        calendarService.updateEvent(ev, currentUser);
                    }
                    Platform.runLater(() -> {
                        dialog.close();
                        ViewNavigator.showToast("Calendar event saved successfully.", false);
                        this.selectedDate = evDate;
                        this.currentYearMonth = YearMonth.from(evDate);
                        loadMonthData();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        saveBtn.setDisable(false);
                        ViewNavigator.showToast("Error saving event: " + ex.getMessage(), true);
                    });
                }
            });
        });

        dialog.setFooterButtons(cancelBtn, saveBtn);
        dialog.show();
    }

    private void confirmDeleteEvent(CalendarEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Event");
        alert.setHeaderText("Delete '" + event.getTitle() + "'?");
        alert.setContentText("Are you sure you want to remove this event from the calendar?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                User currentUser = AuthService.getCurrentUser();
                AppExecutor.execute(() -> {
                    try {
                        calendarService.deleteEvent(event.getId(), currentUser);
                        Platform.runLater(() -> {
                            ViewNavigator.showToast("Event deleted.", false);
                            loadMonthData();
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> ViewNavigator.showToast("Failed to delete event: " + ex.getMessage(), true));
                    }
                });
            }
        });
    }

    @Override
    public void dispose() {
        this.isDisposed = true;
    }
}
