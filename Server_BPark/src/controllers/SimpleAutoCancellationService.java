package controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import server.DBController;
import services.EmailService;

/**
 * Enhanced Automatic Service for: 1. Reservation Cancellation (15-minute rule
 * for preorders) 2. Late Pickup Monitoring (15-minute rule for active parkings)
 * 
 * Runs every minute to check both conditions and send email notifications
 */
public class SimpleAutoCancellationService {

	/** Parking controller associated with the service */
	private final ParkingController parkingController;
	/** Scheduled task executor for periodic checks */
	private final ScheduledExecutorService scheduler;
	/** Late threshold in minutes (used for both preorders and pickups) */
	private static final int LATE_THRESHOLD_MINUTES = 15;
	
	private static final int LATE_PICKUP_THRESHOLD_MINUTES = 0;
	/** Indicates if the service is currently running */
	private boolean isRunning = false;

	/**
	 * Constructor for the service.
	 * 
	 * @param parkingController reference to the main ParkingController.
	 */
	public SimpleAutoCancellationService(ParkingController parkingController) {
		this.parkingController = parkingController;
		this.scheduler = Executors.newScheduledThreadPool(1);
	}

	/**
	 * Start the automatic monitoring service Runs every minute to check for: 1.
	 * Late preorder reservations (auto-cancel) 2. Late active parkings (mark as
	 * late and notify)
	 */
	public void startService() {
		if (isRunning) {
			System.out.println("Auto-monitoring service is already running");
			return;
		}

		isRunning = true;
		System.out.println("Starting automatic monitoring service...");
		System.out.println("Checking every minute for:");
		System.out.println("  - Late preorder reservations (15+ min late = auto-cancel)");
		System.out.println("  - Late active parkings (15+ min late = notify customer)");

		// Schedule to run every minute
		scheduler.scheduleWithFixedDelay(() -> {
			long startTime = System.currentTimeMillis();
			try {
				System.out.println("[" + getCurrentTimestamp() + "] Starting monitoring cycle...");
				checkAndCancelLatePreorders();
				checkAndNotifyLatePickups();
				long executionTime = System.currentTimeMillis() - startTime;
				System.out.println(
						"[" + getCurrentTimestamp() + "] Monitoring cycle completed in " + executionTime + "ms");
			} catch (Exception e) {
				long executionTime = System.currentTimeMillis() - startTime;
				System.err.println("[" + getCurrentTimestamp() + "] Error in auto-monitoring service after "
						+ executionTime + "ms: " + e.getMessage());
				e.printStackTrace(); // Print full stack trace for debugging
			}
		}, 0, 30, TimeUnit.SECONDS);
	}

	/**
	 * Stop the automatic monitoring service
	 */
	public void stopService() {
		if (!isRunning) {
			return;
		}

		isRunning = false;
		scheduler.shutdown();
		System.out.println("Auto-monitoring service stopped");
	}

	/**
	 * Checks and cancels late preorder reservations that exceeded 15 minutes delay.
	 * Also sends email notifications to the customers.
	 */
	private void checkAndCancelLatePreorders() {
		long startTime = System.currentTimeMillis();
		Connection conn = null;

		try {
			conn = DBController.getInstance().getConnection();
			if (conn == null) {
				System.err.println("Failed to get database connection for preorder check");
				return;
			}
			String query = """
					SELECT
					    pi.ParkingInfo_ID,
					    pi.User_ID,
					    pi.ParkingSpot_ID,
					    u.UserName,
					    u.Email,
					    u.Name,
					    u.Phone,
					    TIMESTAMPDIFF(MINUTE, pi.Estimated_start_time, NOW()) as minutes_late,
					    pi.Estimated_start_time
					FROM parkinginfo pi
					JOIN users u ON pi.User_ID = u.User_ID
					WHERE pi.statusEnum = 'preorder'
					AND DATE(pi.Estimated_start_time) = CURDATE()
					AND pi.ParkingSpot_ID IS NOT NULL
					AND pi.Estimated_start_time IS NOT NULL
					AND TIMESTAMPDIFF(MINUTE, pi.Estimated_start_time, NOW()) >= ?
					""";

			try (PreparedStatement stmt = conn.prepareStatement(query)) {
				stmt.setInt(1, LATE_THRESHOLD_MINUTES);

				try (ResultSet rs = stmt.executeQuery()) {
					int cancelledCount = 0;

					while (rs.next()) {
						int reservationCode = rs.getInt("ParkingInfo_ID");
						int spotId = rs.getInt("ParkingSpot_ID");
						String userName = rs.getString("UserName");
						String userEmail = rs.getString("Email");
						String fullName = rs.getString("Name");
						int minutesLate = rs.getInt("minutes_late");

						if (cancelLateReservation(reservationCode, spotId)) {
							cancelledCount++;

							// Send email notification for auto-cancellation
							if (userEmail != null && fullName != null) {
								EmailService.sendReservationCancelled(userEmail, fullName,
										String.valueOf(reservationCode));
							}

							System.out.println(String.format(
									"✅ AUTO-CANCELLED: Reservation %d for %s (Spot %d) - %d minutes late - Email sent",
									reservationCode, userName, spotId, minutesLate));
						}
					}

					if (cancelledCount > 0) {

						System.out.println(String.format("[%s] Auto-cancellation: %d preorder reservations cancelled",
								getCurrentTimestamp(), cancelledCount));

					}
				}

			}
			long executionTime = System.currentTimeMillis() - startTime;
			if (executionTime > 5000) { // Log if operation takes more than 5 seconds
				System.out.println("WARNING: Preorder check took " + executionTime + "ms");
			}
		} catch (SQLException e) {
			long executionTime = System.currentTimeMillis() - startTime;
			System.err.println(
					"Database error during auto-cancellation (after " + executionTime + "ms): " + e.getMessage());
		} catch (Exception e) {
			long executionTime = System.currentTimeMillis() - startTime;
			System.err.println(
					"Unexpected error during auto-cancellation (after " + executionTime + "ms): " + e.getMessage());
		} finally {
			if (conn != null) {
				DBController.getInstance().releaseConnection(conn);
			}
		}
	}

	/**
	 * Checks for late pickups in active parking sessions and sends notifications.
	 */
	private void checkAndNotifyLatePickups() {
		long startTime = System.currentTimeMillis();
		Connection conn = null;

		try {
			conn = DBController.getInstance().getConnection();
			if (conn == null) {
				System.err.println("Failed to get database connection for late pickup check");
				return;
			}

			String query = """
					SELECT
					    pi.ParkingInfo_ID,
					    pi.User_ID,
					    pi.ParkingSpot_ID,
					    u.UserName,
					    u.Email,
					    u.Name,
					    u.Phone,
					    TIMESTAMPDIFF(MINUTE, pi.Estimated_end_time, NOW()) as minutes_late,
					    pi.Estimated_end_time,
					    pi.IsLate
					FROM parkinginfo pi
					JOIN users u ON pi.User_ID = u.User_ID
					WHERE pi.statusEnum = 'active'
					AND pi.Actual_end_time IS NULL
					AND pi.Estimated_end_time IS NOT NULL
					AND TIMESTAMPDIFF(MINUTE, pi.Estimated_end_time, NOW()) >= ?
					AND pi.IsLate = 'no'
					""";

			try (PreparedStatement stmt = conn.prepareStatement(query)) {
				stmt.setInt(1, LATE_PICKUP_THRESHOLD_MINUTES);

				try (ResultSet rs = stmt.executeQuery()) {
					int notifiedCount = 0;

					while (rs.next()) {
						int parkingInfoId = rs.getInt("ParkingInfo_ID");
						String userName = rs.getString("UserName");
						String userEmail = rs.getString("Email");
						String fullName = rs.getString("Name");
						int spotId = rs.getInt("ParkingSpot_ID");
						int minutesLate = rs.getInt("minutes_late");

						if (markAsLateAndNotify(parkingInfoId, userEmail, fullName)) {
							notifiedCount++;

							System.out.println(String.format(
									"⏰ LATE PICKUP: Parking %d for %s (Spot %d) - %d minutes late - Email sent",
									parkingInfoId, userName, spotId, minutesLate));
						}
					}

					if (notifiedCount > 0) {

						System.out.println(String.format("[%s] Late pickup monitoring: %d customers notified",
								getCurrentTimestamp(), notifiedCount));
					}
				}

				long executionTime = System.currentTimeMillis() - startTime;
				if (executionTime > 5000) { // Log if operation takes more than 5 seconds
					System.out.println("WARNING: Late pickup check took " + executionTime + "ms");
				}
			}
		} catch (SQLException e) {
			long executionTime = System.currentTimeMillis() - startTime;
			System.err.println(
					"Database error during late pickup check (after " + executionTime + "ms): " + e.getMessage());
		} catch (Exception e) {
			long executionTime = System.currentTimeMillis() - startTime;
			System.err.println(
					"Unexpected error during late pickup check (after " + executionTime + "ms): " + e.getMessage());
		} finally {
			if (conn != null) {
				DBController.getInstance().releaseConnection(conn);
			}
		}
	}

	/**
	 * Marks a parking session as late and sends email notification to the user.
	 * 
	 * @param parkingInfoId The parking session ID.
	 * @param userEmail     User's email address.
	 * @param fullName      User's full name.
	 * @return true if update and email were successful, false otherwise.
	 */
	private boolean markAsLateAndNotify(int parkingInfoId, String userEmail, String fullName) {
		Connection conn = DBController.getInstance().getConnection();

		try {
			// Update IsLate to 'yes'
			String updateQuery = """
					UPDATE parkinginfo
					SET IsLate = 'yes'
					WHERE ParkingInfo_ID = ? AND statusEnum = 'active' AND IsLate = 'no'
					""";

			int updated = 0;
			try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
				stmt.setInt(1, parkingInfoId);
				updated = stmt.executeUpdate();
			}

			if (updated > 0) {
				// Send late pickup email notification
				if (userEmail != null && fullName != null) {
					EmailService.sendLatePickupNotification(userEmail, fullName);
				}
				return true;
			}
			return false;

		} catch (SQLException e) {
			System.err.println("Error marking parking as late: " + e.getMessage());
			return false;
		} finally {
			DBController.getInstance().releaseConnection(conn);
		}

	}

	/**
	 * Cancels a specific preorder reservation and releases its assigned parking
	 * spot.
	 * 
	 * @param reservationCode The reservation ID to cancel.
	 * @param spotId          The parking spot ID to free.
	 * @return true if successfully canceled, false otherwise.
	 */
	private boolean cancelLateReservation(int reservationCode, int spotId) {
		Connection conn = DBController.getInstance().getConnection();

		try {
			conn.setAutoCommit(false);

			// 1. Cancel the reservation (change status from preorder to cancelled)
			String cancelQuery = """
					UPDATE parkinginfo
					SET statusEnum = 'cancelled'
					WHERE ParkingInfo_ID = ? AND statusEnum = 'preorder'
					""";

			int updatedReservations = 0;
			try (PreparedStatement stmt = conn.prepareStatement(cancelQuery)) {
				stmt.setInt(1, reservationCode);
				updatedReservations = stmt.executeUpdate();
			}

			if (updatedReservations == 0) {
				conn.rollback();
				return false; // Reservation was already cancelled or doesn't exist
			}

			// 2. Free up the parking spot
			String freeSpotQuery = """
					UPDATE parkingspot
					SET isOccupied = FALSE
					WHERE ParkingSpot_ID = ?
					""";

			try (PreparedStatement stmt = conn.prepareStatement(freeSpotQuery)) {
				stmt.setInt(1, spotId);
				stmt.executeUpdate();
			}

			conn.commit();
			return true;

		} catch (SQLException e) {
			try {
				conn.rollback();
			} catch (SQLException rollbackEx) {
				System.err.println("Failed to rollback transaction: " + rollbackEx.getMessage());
			}
			System.err.println("Failed to cancel reservation " + reservationCode + ": " + e.getMessage());
			return false;
		} finally {
			try {
				conn.setAutoCommit(true);
			} catch (SQLException e) {
				System.err.println("Failed to reset auto-commit: " + e.getMessage());
			} finally {
				DBController.getInstance().releaseConnection(conn);
			}

		}
	}

	/**
	 * Activates a preorder reservation when the user arrives.
	 * 
	 * @param reservationCode The reservation ID.
	 * @return true if activation succeeded, false otherwise.
	 */
	public boolean activateReservation(int reservationCode) {
		String query = """
				UPDATE parkinginfo
				SET statusEnum = 'active', Actual_start_time = NOW()
				WHERE ParkingInfo_ID = ? AND statusEnum = 'preorder'
				""";
		Connection conn = DBController.getInstance().getConnection();

		try (PreparedStatement stmt = conn.prepareStatement(query)) {
			stmt.setInt(1, reservationCode);
			int updated = stmt.executeUpdate();

			if (updated > 0) {
				System.out.println("Reservation " + reservationCode + " activated (preorder → active)");
				return true;
			}
			return false;

		} catch (SQLException e) {
			System.err.println("Error activating reservation: " + e.getMessage());
			return false;
		} finally {
			DBController.getInstance().releaseConnection(conn);
		}
	}

	/**
	 * Completes a reservation session, marks it as finished, and releases the spot.
	 * 
	 * @param reservationCode The reservation ID.
	 * @param spotId          The parking spot ID.
	 * @return true if successfully finished, false otherwise.
	 */
	public boolean finishReservation(int reservationCode, int spotId) {
		Connection conn = DBController.getInstance().getConnection();

		try {
			conn.setAutoCommit(false);

			// 1. Update reservation status to finished and set actual end time
			String finishQuery = """
					UPDATE parkinginfo
					SET statusEnum = 'finished', Actual_end_time = NOW()
					WHERE ParkingInfo_ID = ? AND statusEnum = 'active'
					""";

			int updated = 0;
			try (PreparedStatement stmt = conn.prepareStatement(finishQuery)) {
				stmt.setInt(1, reservationCode);
				updated = stmt.executeUpdate();
			}

			if (updated == 0) {
				conn.rollback();
				return false;
			}

			// 2. Free up the parking spot
			String freeSpotQuery = """
					UPDATE parkingspot
					SET isOccupied = FALSE
					WHERE ParkingSpot_ID = ?
					""";

			try (PreparedStatement stmt = conn.prepareStatement(freeSpotQuery)) {
				stmt.setInt(1, spotId);
				stmt.executeUpdate();
			}

			conn.commit();
			System.out.println("Reservation " + reservationCode + " finished and spot " + spotId + " freed");
			return true;

		} catch (SQLException e) {
			try {
				conn.rollback();
			} catch (SQLException rollbackEx) {
				System.err.println("Failed to rollback: " + rollbackEx.getMessage());
			}
			System.err.println("Error finishing reservation: " + e.getMessage());
			return false;
		} finally {
			try {
				conn.setAutoCommit(true);
			} catch (SQLException e) {
				System.err.println("Failed to reset auto-commit: " + e.getMessage());
			} finally {
				DBController.getInstance().releaseConnection(conn);
			}

		}
	}

	/**
	 * Gets the current timestamp formatted as string for logging.
	 * 
	 * @return Current timestamp as a string.
	 */
	private String getCurrentTimestamp() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
	}

	/**
	 * Checks whether the service is currently running.
	 * 
	 * @return true if running, false otherwise.
	 */
	public boolean isRunning() {
		return isRunning;
	}

	/**
	 * Safely shuts down the service, attempting graceful termination first.
	 */
	public void shutdown() {
		stopService();
		try {
			if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
				scheduler.shutdownNow();
			}
		} catch (InterruptedException e) {
			scheduler.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}
}