package controllers;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.ResourceBundle;

import client.BParkClientApp;
import entities.DashboardData;
import entities.Message;
import entities.Message.MessageType;
import entities.ParkingOrder;
import entities.ParkingReport;
import entities.ParkingSubscriber;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class ManagerController implements Initializable {

	// Dashboard Labels
	@FXML
	private Label lblTotalSpots;
	@FXML
	private Label lblOccupied;
	@FXML
	private Label lblAvailable;
	@FXML
	private Label lblReservations;
	@FXML
	private Label lblSystemStatus;
	@FXML
	private Label lblManagerInfo;
	@FXML
	private Label lblLastUpdate;

	@FXML
	private TableView<ParkingSubscriber> tableSubscribers;
	@FXML
	private TableColumn<ParkingSubscriber, String> colSubName;
	@FXML
	private TableColumn<ParkingSubscriber, String> colSubPhone;
	@FXML
	private TableColumn<ParkingSubscriber, String> colSubEmail;
	@FXML
	private TableColumn<ParkingSubscriber, String> colSubCar;
	@FXML
	private TableColumn<ParkingSubscriber, String> colSubUsername;

	@FXML
	private TableView<ParkingOrder> tableActiveParkings;
	@FXML
	private TableColumn<ParkingOrder, String> colParkingCode;
	@FXML
	private TableColumn<ParkingOrder, String> colSubscriberName;
	@FXML
	private TableColumn<ParkingOrder, String> colSpot;
	@FXML
	private TableColumn<ParkingOrder, String> colEntryTime;
	@FXML
	private TableColumn<ParkingOrder, String> colExpectedExit;
	@FXML
	private TableColumn<ParkingOrder, String> colType;

	private ObservableList<ParkingOrder> activeParkings = FXCollections.observableArrayList();

	// Charts
	@FXML
	private LineChart<String, Number> occupancyChart;
	@FXML
	private PieChart parkingTypesChart;
	@FXML
	private BarChart<String, Number> parkingTimeChart;
	@FXML
	private AreaChart<String, Number> subscriberActivityChart;

	// HBox container for the custom legend displayed below the PieChart, holding
	// color indicators and labels
	@FXML
	private HBox legendBox;

	// Report Controls
	@FXML
	private ComboBox<String> comboReportType;
	@FXML
	private DatePicker datePickerFrom;
	@FXML
	private DatePicker datePickerTo;

	// Report Labels
	@FXML
	private Label lblAvgDuration;
	@FXML
	private Label lblTotalParkings;
	@FXML
	private Label lblLateExits;
	@FXML
	private Label lblExtensions;
	@FXML
	private Label lblActiveSubscribers;
	@FXML
	private Label lblTotalOrders;
	@FXML
	private Label lblReservationCount;
	@FXML
	private Label lblCancelled;

	// System Management
	@FXML
	private Label lblAutoCancelStatus;
	@FXML
	private ComboBox<String> comboMonth;
	@FXML
	private ComboBox<String> comboYear;
	@FXML
	private Label lblTotalUsers;
	@FXML
	private Label lblPeakHours;
	@FXML
	private Label lblAvgDailyUsage;

	// Embedded Attendant Controller (if using include)
	@FXML
	private AttendantController attendantController;

	private Timeline refreshTimeline;
	private ObservableList<ParkingReport> currentReports = FXCollections.observableArrayList();

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		BParkClientApp.setManagerController(this);
		setupUI();
		loadInitialData();
		startAutoRefresh();
		loadSubscribers();

		Message msg = new Message(Message.MessageType.DASHBOARD_DATA_REQUEST, null);
		BParkClientApp.sendMessage(msg);

	}

	private void setupUI() {
		setupTableColumns();

		// Initialize report types
		if (comboReportType != null) {
			comboReportType.getItems().addAll("Parking Time Report", "Subscriber Status Report", "All Reports");
			comboReportType.setValue("All Reports");
		}

		// Initialize month/year combos for monthly reports
		if (comboMonth != null && comboYear != null) {
			comboMonth.getItems().addAll("January", "February", "March", "April", "May", "June", "July", "August",
					"September", "October", "November", "December");

			int currentYear = LocalDate.now().getYear();
			for (int year = currentYear - 2; year <= currentYear; year++) {
				comboYear.getItems().add(String.valueOf(year));
			}

			comboMonth.setValue(LocalDate.now().getMonth().toString());
			comboYear.setValue(String.valueOf(currentYear));
		}

		// Set date picker defaults
		if (datePickerFrom != null && datePickerTo != null) {
			datePickerTo.setValue(LocalDate.now());
			datePickerFrom.setValue(LocalDate.now().minusDays(30));
		}

		// Initialize charts
		initializeCharts();

		// Set manager info
		if (lblManagerInfo != null) {
			lblManagerInfo.setText("Manager: " + BParkClientApp.getCurrentUser());
		}

		// Set static values
		if (lblTotalSpots != null) {
			lblTotalSpots.setText("100");
		}
		if (tableSubscribers != null) {
			colSubName.setCellValueFactory(
					cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFirstName()));
			colSubPhone.setCellValueFactory(
					cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPhoneNumber()));
			colSubEmail.setCellValueFactory(
					cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEmail()));
			colSubCar.setCellValueFactory(
					cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCarNumber()));
			colSubUsername.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
					cellData.getValue().getSubscriberCode()));
		}
	}

	private void initializeCharts() {
		// Initialize occupancy chart with sample data
		if (occupancyChart != null) {
			XYChart.Series<String, Number> series = new XYChart.Series<>();
			series.setName("Occupancy");

			// Add hourly data points
			for (int hour = 6; hour <= 22; hour++) {
				series.getData().add(new XYChart.Data<>(hour + ":00", 0));
			}

			occupancyChart.getData().add(series);
			occupancyChart.setCreateSymbols(false);
		}

		// Initialize parking types pie chart
		if (parkingTypesChart != null) {
			ObservableList<PieChart.Data> pieChartData = FXCollections
					.observableArrayList(new PieChart.Data("Immediate", 60), new PieChart.Data("Reserved", 40));
			parkingTypesChart.setData(pieChartData);
		}
	}

	private void loadInitialData() {
		// Load parking availability
		checkParkingStatus();

		// Load initial reports
		loadReports("ALL");

		// Update timestamp
		updateLastRefreshTime();
	}

//	private void startAutoRefresh() {
//		// Refresh dashboard every 30 seconds
//		refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(30), event -> {
//			checkParkingStatus();
//			updateLastRefreshTime();
//		}));
//		refreshTimeline.setCycleCount(Timeline.INDEFINITE);
//		refreshTimeline.play();
//	}
	
	
	/**
	 * Starts an automatic refresh mechanism using JavaFX Timeline.
	 * Every 30 seconds, it:
	 *  - Requests parking availability status.
	 *  - Requests the latest dashboard data from the server.
	 *  - Updates the UI with the current timestamp of the last refresh.
	 */
	private void startAutoRefresh() {
	    refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(30), event -> {
	        // Request updated parking availability from the server
	        checkParkingStatus();

	        // Request updated dashboard data from the server
	        Message msg = new Message(MessageType.DASHBOARD_DATA_REQUEST, null);
	        BParkClientApp.sendMessage(msg);

	        // Update the last refresh timestamp label in the UI
	        updateLastRefreshTime();
	    }));
	    refreshTimeline.setCycleCount(Timeline.INDEFINITE); // Repeat indefinitely
	    refreshTimeline.play(); // Start the timeline
	}

	// ===== Action Handlers =====

	@FXML
	private void handleGenerateReports() {
		loadReports("ALL");
	}

	@FXML
	private void handleGenerateSelectedReport() {
		String reportType = comboReportType.getValue();
		LocalDate fromDate = datePickerFrom.getValue();
		LocalDate toDate = datePickerTo.getValue();

		if (reportType == null) {
			showAlert("Error", "Please select a report type");
			return;
		}

		String type = reportType.contains("Time") ? "PARKING_TIME"
				: reportType.contains("Subscriber") ? "SUBSCRIBER_STATUS" : "ALL";

		loadReports(type);
	}

	@FXML
	private void handleGenerateMonthlyReports() {
		String month = comboMonth.getValue();
		String year = comboYear.getValue();

		if (month == null || year == null) {
			showAlert("Error", "Please select month and year");
			return;
		}

		// Convert month name to number
		int monthNum = comboMonth.getSelectionModel().getSelectedIndex() + 1;
		String monthYear = String.format("%s-%02d", year, monthNum);

		Message msg = new Message(MessageType.GENERATE_MONTHLY_REPORTS, monthYear);
		BParkClientApp.sendMessage(msg);
	}

	@FXML
	private void checkParkingStatus() {
//		Message msg = new Message(MessageType.CHECK_PARKING_AVAILABILITY, null);
//		BParkClientApp.sendMessage(msg); delete the request to a popup alert

		// Also get active parkings for statistics
		Message activeMsg = new Message(MessageType.GET_ACTIVE_PARKINGS, null);
		BParkClientApp.sendMessage(activeMsg);
	}

	private void loadReports(String type) {
		Message msg = new Message(MessageType.MANAGER_GET_REPORTS, type);
		BParkClientApp.sendMessage(msg);
	}

	// ===== UI Update Methods =====

	public void updateParkingStatus(int availableSpots) {
		Platform.runLater(() -> {
			int occupied = 100 - availableSpots;

			if (lblOccupied != null) {
				lblOccupied.setText(String.valueOf(occupied));
			}

			if (lblAvailable != null) {
				lblAvailable.setText(String.valueOf(availableSpots));
			}

			// Update system status based on availability
			if (lblSystemStatus != null) {
				if (availableSpots < 10) {
					lblSystemStatus.setText("System Status: Nearly Full");
					lblSystemStatus.setStyle("-fx-text-fill: #E74C3C;");
				} else if (availableSpots < 40) {
					lblSystemStatus.setText("System Status: Limited Availability");
					lblSystemStatus.setStyle("-fx-text-fill: #F39C12;");
				} else {
					lblSystemStatus.setText("System Status: Operational");
					lblSystemStatus.setStyle("-fx-text-fill: #27AE60;");
				}
			}

			updateOccupancyChart(occupied);
		});
	}

	public void updateReports(ArrayList<ParkingReport> reports) {
		Platform.runLater(() -> {
			currentReports.clear();
			currentReports.addAll(reports);

			for (ParkingReport report : reports) {
				if (report.getReportType().equals("PARKING_TIME")) {
					updateParkingTimeReport(report);
				} else if (report.getReportType().equals("SUBSCRIBER_STATUS")) {
					updateSubscriberStatusReport(report);
				}
			}
		});
	}

	private void updateParkingTimeReport(ParkingReport report) {
		if (lblAvgDuration != null) {
			lblAvgDuration.setText(report.getFormattedAverageParkingTime());
		}
		if (lblTotalParkings != null) {
			lblTotalParkings.setText(String.valueOf(report.getTotalParkings()));
		}
		if (lblLateExits != null) {
			lblLateExits.setText(String.format("%d (%.1f%%)", report.getLateExits(), report.getLateExitPercentage()));
		}
		if (lblExtensions != null) {
			lblExtensions
					.setText(String.format("%d (%.1f%%)", report.getExtensions(), report.getExtensionPercentage()));
		}

		// Update parking time chart
		updateParkingTimeChart(report);
	}

	private void updateSubscriberStatusReport(ParkingReport report) {
		if (lblActiveSubscribers != null) {
			lblActiveSubscribers.setText(String.valueOf(report.getActiveSubscribers()));
		}
		if (lblTotalOrders != null) {
			lblTotalOrders.setText(String.valueOf(report.getTotalOrders()));
		}
		if (lblReservationCount != null) {
			lblReservationCount
					.setText(String.format("%d (%.1f%%)", report.getReservations(), report.getReservationPercentage()));
		}
		if (lblCancelled != null) {
			lblCancelled.setText(String.valueOf(report.getCancelledReservations()));
		}

		// Update subscriber activity chart
		updateSubscriberActivityChart(report);
	}

	public void updateActiveParkings(ArrayList<ParkingOrder> activeParkings) {
		Platform.runLater(() -> {
			// Count reservations
			long reservationCount = activeParkings.stream().filter(p -> "ordered".equals(p.getOrderType())).count();

			if (lblReservations != null) {
				lblReservations.setText(String.valueOf(reservationCount));
			}

			// Update parking types pie chart
			updateParkingTypesChart(activeParkings);

			// Calculate peak hours
			calculatePeakHours(activeParkings);
		});
	}

	private void updateOccupancyChart(int currentOccupancy) {
		if (occupancyChart != null && !occupancyChart.getData().isEmpty()) {
			XYChart.Series<String, Number> series = occupancyChart.getData().get(0);

			// Update current hour's data
			LocalDateTime now = LocalDateTime.now();
			String hourLabel = now.getHour() + ":00";

			// Find and update the data point
			for (XYChart.Data<String, Number> data : series.getData()) {
				if (data.getXValue().equals(hourLabel)) {
					data.setYValue(currentOccupancy);
					break;
				}
			}
		}
	}

	private void updateParkingTimeChart(ParkingReport report) {
		if (parkingTimeChart != null) {
			parkingTimeChart.getData().clear();

			XYChart.Series<String, Number> series = new XYChart.Series<>();
			series.setName("Average Duration");

			// Add sample data (would be populated from actual report data)
			series.getData().add(new XYChart.Data<>("Today", report.getAverageParkingTime()));

			parkingTimeChart.getData().add(series);
		}
	}

	private void updateSubscriberActivityChart(ParkingReport report) {
		if (subscriberActivityChart != null) {
			subscriberActivityChart.getData().clear();

			XYChart.Series<String, Number> series = new XYChart.Series<>();
			series.setName("Daily Activity");

			// Add sample data
			series.getData().add(new XYChart.Data<>("Today", report.getTotalOrders()));

			subscriberActivityChart.getData().add(series);
		}
	}

	private void updateParkingTypesChart(ArrayList<ParkingOrder> activeParkings) {
		if (parkingTypesChart != null) {
			long immediate = activeParkings.stream().filter(p -> "not ordered".equals(p.getOrderType())).count();
			long reserved = activeParkings.stream().filter(p -> "ordered".equals(p.getOrderType())).count();

			ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
					new PieChart.Data("Immediate", immediate), new PieChart.Data("Reserved", reserved));
			parkingTypesChart.setData(pieChartData);
		}
	}

	private void calculatePeakHours(ArrayList<ParkingOrder> parkings) {
		// Simple peak hour calculation
		// In real implementation, would analyze entry times
		if (lblPeakHours != null) {
			lblPeakHours.setText("9:00-11:00, 14:00-16:00");
		}
	}

	private void updateLastRefreshTime() {
		if (lblLastUpdate != null) {
			String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
			lblLastUpdate.setText("Last Update: " + timestamp);
		}
	}

	// ===== Utility Methods =====

	private void showAlert(String title, String content) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}

	private void showError(String title, String content) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}

	@FXML
	private void handleLogout() {
		BParkClientApp.disconnect();
		System.exit(0);
	}

	@FXML
	private void loadActiveParkings() {
		Message msg = new Message(MessageType.GET_ACTIVE_PARKINGS, null);
		BParkClientApp.sendMessage(msg);
	}

	@FXML
	private void handleViewSubscriberDetails() {
		ParkingOrder selectedOrder = tableActiveParkings.getSelectionModel().getSelectedItem();
		if (selectedOrder != null) {
			String subscriberName = selectedOrder.getSubscriberName();
			Message msg = new Message(MessageType.GET_SUBSCRIBER_BY_NAME, subscriberName);
			BParkClientApp.sendMessage(msg);
		} else {
			showAlert("Selection Required", "Please select a parking session from the table");
		}
	}

	public void showSubscriberDetails(ParkingSubscriber parkingSubscriber) {
		Platform.runLater(() -> {
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			alert.setTitle("Subscriber Details");
			alert.setHeaderText("Details for " + parkingSubscriber.getFirstName());

			String details = String.format("User ID: %s\nName: %s\nPhone: %s\nEmail: %s\nCar num: %s\ntype: %s",
					parkingSubscriber.getSubscriberID(), parkingSubscriber.getFirstName(),
					parkingSubscriber.getPhoneNumber(), parkingSubscriber.getEmail(), parkingSubscriber.getCarNumber(),
					parkingSubscriber.getUserType());

			alert.setContentText(details);
			alert.showAndWait();
		});
	}

	public void updateActiveParkings(ObservableList<ParkingOrder> parkings) {
		Platform.runLater(() -> {
			tableActiveParkings.setItems(parkings);
		});
	}

	private void setupTableColumns() {
		colParkingCode.setCellValueFactory(
				cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getParkingCode()));

		colSubscriberName.setCellValueFactory(
				cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSubscriberName()));

		colSpot.setCellValueFactory(
				cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSpotNumber()));

		colEntryTime.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
				cellData.getValue().getFormattedEntryTime()));

		colExpectedExit.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
				cellData.getValue().getFormattedExpectedExitTime()));

		colType.setCellValueFactory(
				cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getOrderType()));
	}

	@FXML
	private void loadSubscribers() {
		Message msg = new Message(MessageType.GET_ALL_SUBSCRIBERS, null);
		BParkClientApp.sendMessage(msg);
	}

	public void updateSubscriberTable(java.util.List<ParkingSubscriber> subscribers) {
		Platform.runLater(() -> {
			ObservableList<ParkingSubscriber> list = FXCollections.observableArrayList(subscribers);
			tableSubscribers.setItems(list);
		});
	}

	/**
	 * Updates the dashboard UI elements with the latest data.
	 *
	 * @param data DashboardData object containing updated metrics: - total spots -
	 *             occupied spots - available spots - active reservations -
	 *             reservation types counts for the pie chart
	 */
	public void updateDashboard(DashboardData data) {
		// Update the labels with numeric data from DashboardData
		lblTotalSpots.setText(String.valueOf(data.getTotalSpots()));
		lblOccupied.setText(String.valueOf(data.getOccupied()));
		lblAvailable.setText(String.valueOf(data.getAvailable()));
		lblReservations.setText(String.valueOf(data.getActiveReservations()));

		// Prepare PieChart data from reservation types and counts

		ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
		data.getReservationTypes().forEach((type, count) -> pieData.add(new PieChart.Data(type, count)));

		// Set the PieChart data and hide the default legend since a custom legend is
		// used
		parkingTypesChart.setData(pieData);
		parkingTypesChart.setLegendVisible(false);
		
		
		 // Update pie slice colors on the JavaFX Application Thread
		Platform.runLater(() -> {
			for (PieChart.Data d : pieData) {
				String label = d.getName();
				String color;
				switch (label) {
				case "Available":
					color = "#27ae60";
					break; // green
				case "Reserved":
					color = "#2980b9";
					break; // blue
				case "Immediate":
					color = "#e74c3c";
					break; // red
				default:
					color = "gray";
					break;
				}
				 // Apply the color style to the pie slice node
				d.getNode().setStyle("-fx-pie-color: " + color + ";");

			}
		});
	}

}