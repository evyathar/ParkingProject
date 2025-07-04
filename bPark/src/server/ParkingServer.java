package server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import controllers.ParkingController;
import controllers.ReportController;
import entities.DashboardData;
import entities.Message;
import entities.Message.MessageType;
import entities.ParkingOrder;
import entities.ParkingReport;
import entities.ParkingSubscriber;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;
import serverGUI.ServerPortFrame;

/**
 * ParkingServer - Main server for the ParkB automatic parking management system
 * Now includes auto-cancellation service shutdown
 */
public class ParkingServer extends AbstractServer {
	// Class variables *************************************************

	/**
	 * The default port to listen on.
	 */
	final public static Integer DEFAULT_PORT = 5555;

	// Controllers (following your pattern)
	public static ParkingController parkingController;
	public static ReportController reportController;
	public static ServerPortFrame spf;

	// Connection management
	public Map<ConnectionToClient, String> clientsMap = new HashMap<>();
	public static String serverIp;

	// Connection pool with timer for cleanup
	private ScheduledExecutorService connectionPoolTimer;
	private final int POOL_SIZE = 5;
	private final int TIMER_INTERVAL = 30; // 30 seconds

	// Constructors ****************************************************

	/**
	 * Constructs an instance of the parking server.
	 * 
	 * @param port The port number to connect on.
	 */
	public ParkingServer(int port) {
		super(port);
		try {
			serverIp = InetAddress.getLocalHost().getHostAddress() + ":" + port; // IP:PORT
		} catch (Exception e) {
			e.printStackTrace();
		}
		initializeConnectionPool();
	}

	/**
	 * Initialize connection pool with timer for monitoring
	 */
	private void initializeConnectionPool() {
		connectionPoolTimer = Executors.newScheduledThreadPool(POOL_SIZE);

		// Start connection pool monitoring timer
		connectionPoolTimer.scheduleAtFixedRate(() -> {
			synchronized (clientsMap) {
				System.out.println("Connection Pool Status - Active connections: " + clientsMap.size());
				cleanupInactiveConnections();
			}
		}, 0, TIMER_INTERVAL, TimeUnit.SECONDS);
	}

	/**
	 * Cleanup inactive connections
	 */
	private synchronized void cleanupInactiveConnections() {
		clientsMap.entrySet().removeIf(entry -> {
			ConnectionToClient client = entry.getKey();
			return !client.isAlive();
		});
	}

	// Instance methods ************************************************

	/**
	 * This method handles any messages received from the client. Following your
	 * exact handleMessageFromClient pattern
	 */
	public synchronized void handleMessageFromClient(Object msg, ConnectionToClient client) {

		try {
			// If the message is a byte array, deserialize it
			if (msg instanceof byte[]) {
				msg = deserialize(msg);
			}

			// Print the actual deserialized message
			if (msg instanceof Message) {
				Message message = (Message) msg;
				System.out.println("Message received: " + message.getType() + " from " + client);
				handleMessageObject(message, client);
			} else if (msg instanceof String) {
				System.out.println("Message received: " + msg + " from " + client);
				handleStringMessage((String) msg, client);
			} else {
				System.out.println("Unknown message type from " + client);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Handle Message objects (following your Message handling pattern)
	 */
	private synchronized void handleMessageObject(Message message, ConnectionToClient client) {
		Message ret;

		try {
			switch (message.getType()) {
			case SUBSCRIBER_LOGIN:
				String[] loginParts = ((String) message.getContent()).split(",");
				if (loginParts.length < 2) {
					ret = new Message(MessageType.SUBSCRIBER_LOGIN_RESPONSE, "ERROR: Missing username or user code");
					client.sendToClient(serialize(ret));
					break;
				}

				String username = loginParts[0].trim();
				String userCode = loginParts[1].trim();

				ParkingSubscriber subscriber = parkingController.getUserInfo(username);

				if (subscriber != null && String.valueOf(subscriber.getSubscriberID()).equals(userCode)) {
					ret = new Message(MessageType.SUBSCRIBER_LOGIN_RESPONSE, subscriber);
				} else {
					ret = new Message(MessageType.SUBSCRIBER_LOGIN_RESPONSE, null);
				}

				client.sendToClient(serialize(ret));
				break;

			case CHECK_PARKING_AVAILABILITY:
				int availableSpots = parkingController.getAvailableParkingSpots();
				ret = new Message(MessageType.PARKING_AVAILABILITY_RESPONSE, availableSpots);
				client.sendToClient(serialize(ret));
				break;

			case RESERVE_PARKING:
				String[] reservationData = ((String) message.getContent()).split(",");
				String reservationUserName = reservationData[0]; // ← RENAMED
				String reservationDate = reservationData[1];
				String reservationResult = parkingController.makeReservation(reservationUserName, reservationDate);
				ret = new Message(MessageType.RESERVATION_RESPONSE, reservationResult);
				client.sendToClient(serialize(ret));
				break;

			case REGISTER_SUBSCRIBER:
				// Expected format: "attendantUserName,name,phone,email,carNumber,userName"
				String registrationData = (String) message.getContent();
				String[] regParts = registrationData.split(",");

				if (regParts.length >= 6) {
					String attendantUserName = regParts[0].trim();
					String name = regParts[1].trim();
					String phone = regParts[2].trim();
					String email = regParts[3].trim();
					String carNumber = regParts[4].trim();
					String subscriberUserName = regParts[5].trim(); // ← RENAMED

					String registrationResult = parkingController.registerNewSubscriber(attendantUserName, name, phone,
							email, carNumber, subscriberUserName);
					ret = new Message(MessageType.REGISTRATION_RESPONSE, registrationResult);
				} else {
					ret = new Message(MessageType.REGISTRATION_RESPONSE, "ERROR: Invalid registration data format");
				}
				client.sendToClient(serialize(ret));
				break;

			case REQUEST_LOST_CODE:
				String lostCodeUserName = (String) message.getContent(); // ← RENAMED
				String lostCodeResult = parkingController.sendLostParkingCode(lostCodeUserName);
				ret = new Message(MessageType.LOST_CODE_RESPONSE, lostCodeResult);
				client.sendToClient(serialize(ret));
				break;

			case GET_PARKING_HISTORY:
				String historyUserName = (String) message.getContent(); // ← RENAMED
				ArrayList<ParkingOrder> history = parkingController.getParkingHistory(historyUserName);
				ret = new Message(MessageType.PARKING_HISTORY_RESPONSE, history);
				client.sendToClient(serialize(ret));
				break;

			case MANAGER_GET_REPORTS:
				String reportType = (String) message.getContent();
				ArrayList<ParkingReport> reports = reportController.getParkingReports(reportType);
				ret = new Message(MessageType.MANAGER_SEND_REPORTS, reports);
				client.sendToClient(serialize(ret));
				break;

			case GET_ACTIVE_PARKINGS:
				ArrayList<ParkingOrder> activeParkings = parkingController.getActiveParkings();
				ret = new Message(MessageType.ACTIVE_PARKINGS_RESPONSE, activeParkings);
				client.sendToClient(serialize(ret));
				break;

			case UPDATE_SUBSCRIBER_INFO:
				String updateResult = parkingController.updateSubscriberInfo((String) message.getContent());
				ret = new Message(MessageType.UPDATE_SUBSCRIBER_RESPONSE, updateResult);
				client.sendToClient(serialize(ret));
				break;

			case GENERATE_MONTHLY_REPORTS:
				String monthYear = (String) message.getContent();
				ArrayList<ParkingReport> monthlyReports = reportController.generateMonthlyReports(monthYear);
				ret = new Message(MessageType.MONTHLY_REPORTS_RESPONSE, monthlyReports);
				client.sendToClient(serialize(ret));
				break;

			case ACTIVATE_RESERVATION:
				// Expected format: "userName,reservationCode"
				String[] activateData = ((String) message.getContent()).split(",", 2);
				if (activateData.length != 2) {
					ret = new Message(MessageType.ACTIVATION_RESPONSE, "ERROR: Invalid activation data format");
				} else {
					try {
						String activateUserName = activateData[0].trim();
						int reservationCode = Integer.parseInt(activateData[1].trim());
						String activateResult = parkingController.activateReservation(activateUserName,
								reservationCode);
						ret = new Message(MessageType.ACTIVATION_RESPONSE, activateResult);
					} catch (NumberFormatException e) {
						ret = new Message(MessageType.ACTIVATION_RESPONSE, "ERROR: Invalid reservation code format");
					}
				}
				client.sendToClient(serialize(ret));
				break;

			case CANCEL_RESERVATION:
				// Expected format: "userName,reservationCode"
				String[] cancelData = ((String) message.getContent()).split(",", 2);
				if (cancelData.length != 2) {
					ret = new Message(MessageType.CANCELLATION_RESPONSE, "ERROR: Invalid cancellation data format");
				} else {
					try {
						String cancelUserName = cancelData[0].trim();
						int reservationCode = Integer.parseInt(cancelData[1].trim());
						String cancelResult = parkingController.cancelReservation(cancelUserName, reservationCode);
						ret = new Message(MessageType.CANCELLATION_RESPONSE, cancelResult);
					} catch (NumberFormatException e) {
						ret = new Message(MessageType.CANCELLATION_RESPONSE, "ERROR: Invalid reservation code format");
					}
				}
				client.sendToClient(serialize(ret));
				break;

			case GET_SUBSCRIBER_BY_NAME:
				String subscriberName = (String) message.getContent();
				subscriber = parkingController.getSubscriberByName(subscriberName);
				ret = new Message(MessageType.SHOW_SUBSCRIBER_DETAILS, subscriber);
				client.sendToClient(serialize(ret));
				break;

			case GET_ALL_SUBSCRIBERS:
				List<ParkingSubscriber> allSubs = parkingController.getAllSubscribers();
				Message response = new Message(MessageType.SHOW_ALL_SUBSCRIBERS, (Serializable) allSubs);
				client.sendToClient(serialize(response)); // ← נכון
				break;

			case REQUEST_EXTENSION:
				try {
					String[] parts = ((String) message.getContent()).split(",");
					if (parts.length != 2) {
						ret = new Message(MessageType.EXTENSION_RESPONSE, "Invalid extension format.");
					} else {
						String parkingCode = parts[0].trim();
						int additionalHours = Integer.parseInt(parts[1].trim());
						String result = parkingController.extendParkingTime(parkingCode, additionalHours);

						ret = new Message(MessageType.EXTENSION_RESPONSE, result);
					}
				} catch (NumberFormatException e) {
					ret = new Message(MessageType.EXTENSION_RESPONSE, "Invalid number format for extension hours.");
				}
				client.sendToClient(serialize(ret));
				break;

			case REQUEST_SUBSCRIBER_DATA: {
				String userName = (String) message.getContent();
				ParkingSubscriber userInfo = parkingController.getUserInfo(userName); // use your DB instance
				response = new Message(MessageType.SUBSCRIBER_DATA_RESPONSE, userInfo);
				client.sendToClient(response);
				break;
			}
			// Handle client request for dashboard statistics
			case DASHBOARD_DATA_REQUEST:
				// Generate the dashboard data by querying the database
				DashboardData data = buildDashboardData();
				ret = new Message(MessageType.DASHBOARD_DATA_RESPONSE, data);
				client.sendToClient(serialize(ret));
				break;

			default:
				System.out.println("Unknown message type: " + message.getType());
				break;

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Handle String messages (following your string handling pattern)
	 */
	private synchronized void handleStringMessage(String message, ConnectionToClient client) {
		String[] arr = message.split("\\s");

		try {
			switch (arr[0]) {
			case "ClientDisconnect":
				disconnect(client);
				break;

			case "login:":
				String loginResult = parkingController.checkLogin(arr[1], arr.length > 2 ? arr[2] : "");
				client.sendToClient("login: " + loginResult);
				break;

			case "LoggedOut":
				parkingController.logoutUser(arr[1]);
				break;

			case "getParkingSpots":
				int availableSpots = parkingController.getAvailableParkingSpots();
				client.sendToClient("availableSpots " + availableSpots);
				break;

			case "enterParking":
				String enterResult = parkingController.enterParking(arr[1]);
				client.sendToClient("enterResult " + enterResult);
				break;

			case "enterWithReservation":
				String reservationResult = parkingController.enterParkingWithReservation(Integer.parseInt(arr[1]));
				client.sendToClient("reservationResult " + reservationResult);
				break;

			case "exitParking":
				String exitResult = parkingController.exitParking(arr[1]);
				client.sendToClient("exitResult " + exitResult);
				break;

			case "extendParking":
				String extendResult = parkingController.extendParkingTime(arr[1], Integer.parseInt(arr[2]));
				client.sendToClient("extendResult " + extendResult);
				break;

			case "getLostCode":
				String lostCode = parkingController.sendLostParkingCode(arr[1]);
				client.sendToClient("parkingCode " + lostCode);
				break;

			case "makeReservation":
				// Format: makeReservation userName reservationDate
				String makeReservationResult = parkingController.makeReservation(arr[1], arr[2]);
				client.sendToClient("reservationResult " + makeReservationResult);
				break;

			case "cancelReservation":
				String cancelResult = parkingController.cancelReservation(Integer.parseInt(arr[1]));
				client.sendToClient("cancelResult " + cancelResult);
				break;

			case "getReports":
				// This could be enhanced to return actual report data
				client.sendToClient("reports " + "Available reports: parking_time, subscriber_status");
				break;

			default:
				System.out.println("Unknown string command: " + arr[0]);
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			try {
				client.sendToClient("error " + e.getMessage());
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}

	/**
	 * Serializes a Message object to byte array (following your pattern)
	 */
	private byte[] serialize(Message msg) {
		try {
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(byteStream);
			out.writeObject(msg);
			out.flush();
			return byteStream.toByteArray();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	/**
	 * Deserializes byte array to Message object (following your pattern)
	 */
	private Object deserialize(Object msg) {
		try {
			byte[] messageBytes = (byte[]) msg;
			ByteArrayInputStream byteStream = new ByteArrayInputStream(messageBytes);
			ObjectInputStream objectStream = new ObjectInputStream(byteStream);
			return objectStream.readObject();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	/**
	 * This method overrides the one in the superclass. Called when the server
	 * starts listening for connections.
	 */
	protected void serverStarted() {
		System.out.println("ParkB Server listening for connections on port " + getPort());
		// Initialize parking spots if needed
		parkingController.initializeParkingSpots();
	}

	/**
	 * This method overrides the one in the superclass. Called when the server stops
	 * listening for connections. MODIFIED: Now includes auto-cancellation service
	 * shutdown
	 */
	protected void serverStopped() {
		System.out.println("ParkB Server has stopped listening for connections.");

		// Stop auto-cancellation service cleanly
		if (parkingController != null) {
			parkingController.shutdown();
			System.out.println("Auto-cancellation service shut down successfully");
		}

		if (connectionPoolTimer != null) {
			connectionPoolTimer.shutdown();
		}
	}

	/**
	 * Client connected handler (following your pattern)
	 */
	@Override
	protected synchronized void clientConnected(ConnectionToClient client) {
		String clientIP = client.getInetAddress().getHostAddress();
		String clientHostName = client.getInetAddress().getHostName();
		String connectionStatus = "ClientIP: " + clientIP + " Client Host Name: " + clientHostName
				+ " status: connected";

		// Check if IP already exists
		synchronized (clientsMap) {
			boolean ipExists = false;
			ConnectionToClient existingClient = null;

			for (Map.Entry<ConnectionToClient, String> entry : clientsMap.entrySet()) {
				if (entry.getValue().contains(clientIP)) {
					ipExists = true;
					existingClient = entry.getKey();
					break;
				}
			}

			if (!ipExists) {
				clientsMap.put(client, connectionStatus);
			} else {
				clientsMap.put(existingClient, connectionStatus);
			}
		}

		if (spf != null) {
			spf.printConnection(clientsMap);
		}
	}

	/**
	 * Client disconnect handler (following your pattern)
	 */
	protected synchronized void disconnect(ConnectionToClient client) {
		String clientIP = client.getInetAddress().getHostAddress();
		String clientHostName = client.getInetAddress().getHostName();
		String disconnectionStatus = "ClientIP: " + clientIP + " Client Host Name: " + clientHostName
				+ " status: disconnected";

		synchronized (clientsMap) {
			boolean ipExists = false;
			ConnectionToClient existingClient = null;

			for (Map.Entry<ConnectionToClient, String> entry : clientsMap.entrySet()) {
				if (entry.getValue().contains(clientIP)) {
					ipExists = true;
					existingClient = entry.getKey();
					break;
				}
			}

			if (ipExists) {
				clientsMap.put(existingClient, disconnectionStatus);
			}
		}

		if (spf != null) {
			spf.printConnection(clientsMap);
		}
	}

	// Class methods ***************************************************

	/**
	 * This method is responsible for the creation of the server instance. Following
	 * your main method pattern
	 */
	public static void main(String[] args) {
		int port = 0;

		try {
			port = Integer.parseInt(args[0]);
		} catch (Throwable t) {
			port = DEFAULT_PORT;
		}

		ParkingServer sv = new ParkingServer(port);

		try {
			sv.listen();
		} catch (Exception ex) {
			System.out.println("ERROR - Could not listen for clients!");
		}
	}

	/**
	 * Shutdown the server properly MODIFIED: Now includes auto-cancellation service
	 * shutdown
	 */
	public synchronized void shutdown() {
		// Stop auto-cancellation service first
		if (parkingController != null) {
			parkingController.shutdown();
		}

		if (connectionPoolTimer != null) {
			connectionPoolTimer.shutdown();
		}
		try {
			close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Builds a {@link DashboardData} object containing key metrics for the
	 * manager's dashboard.
	 * 
	 * <p>
	 * The data includes:
	 * </p>
	 * <ul>
	 * <li>Total number of parking spots</li>
	 * <li>Number of occupied spots</li>
	 * <li>Number of available spots (calculated)</li>
	 * <li>Number of active reservations</li>
	 * <li>A breakdown of spot statuses (Available, Reserved, Immediate)</li>
	 * </ul>
	 * 
	 * <p>
	 * The method retrieves the required data using SQL queries directly from the
	 * database.
	 * </p>
	 * 
	 * @return a {@code DashboardData} object populated with current system
	 *         statistics.
	 */
	private DashboardData buildDashboardData() {
		DashboardData data = new DashboardData();
		try {
			Connection conn = DBController.getInstance().getConnection();
			Statement stmt = conn.createStatement();

			// Total number of parking spots
			ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM parkingspot");
			if (rs.next())
				data.setTotalSpots(rs.getInt(1));

			// Number of occupied parking spots
			rs = stmt.executeQuery("SELECT COUNT(*) FROM parkingspot WHERE isOccupied = 1");
			if (rs.next())
				data.setOccupied(rs.getInt(1));

			// Calculate available spots
			data.setAvailable(data.getTotalSpots() - data.getOccupied());

			// Active reservations during the current time
			rs = stmt.executeQuery(
					"SELECT COUNT(*) FROM parkinginfo WHERE NOW() BETWEEN Estimated_start_time AND Estimated_end_time");
			if (rs.next())
				data.setActiveReservations(rs.getInt(1));

			// Breakdown of spot statuses
			rs = stmt.executeQuery("SELECT " + "CASE " + " WHEN ps.isOccupied = 0 THEN 'Available' "
					+ " WHEN ps.isOccupied = 1 AND pi.IsOrderedEnum = 'yes' "
					+ "      AND NOW() BETWEEN pi.Estimated_start_time AND pi.Estimated_end_time "
					+ "      AND pi.statusEnum NOT IN ('cancelled', 'finished') THEN 'Reserved' "
					+ " WHEN ps.isOccupied = 1 AND (pi.IsOrderedEnum = 'no' OR pi.IsOrderedEnum IS NULL) THEN 'Immediate' "
					+ " ELSE 'Unknown' " + "END AS SpotStatus, " + "COUNT(*) AS CountSpots " + "FROM parkingspot ps "
					+ "LEFT JOIN parkinginfo pi ON ps.ParkingSpot_ID = pi.ParkingSpot_ID "
					+ "    AND NOW() BETWEEN pi.Estimated_start_time AND pi.Estimated_end_time "
					+ "    AND pi.statusEnum NOT IN ('cancelled', 'finished') " + "GROUP BY SpotStatus");

			while (rs.next()) {
				String spotStatus = rs.getString("SpotStatus");
				int count = rs.getInt("CountSpots");
				data.getReservationTypes().put(spotStatus, count);
			}

			rs.close();
			stmt.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return data;
	}

}