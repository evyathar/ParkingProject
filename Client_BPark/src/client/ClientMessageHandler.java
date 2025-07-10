package client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import common.Message;
import common.ParkingOrder;
import common.ParkingReport;
import common.ParkingSubscriber;
import controllers.AttendantController;
import controllers.CustomDialog;
import controllers.ExtendParkingController;
import controllers.KioskController;
import controllers.LoginController;
import controllers.ManagerController;
import controllers.UpdateProfileController;
import javafx.application.Platform;
import javafx.collections.FXCollections;

public class ClientMessageHandler {

	/**
	 * Handle incoming Message objects from the server
	 */
	public static void handleMessage(Message message) {
		switch (message.getType()) {
		case SUBSCRIBER_LOGIN_RESPONSE:
			handleLoginResponse(message);
			break;

		case KIOSK_LOGIN_RESPONSE:
			handleKioskLoginResponse(message);
			break;

		case ENTER_PARKING_KIOSK_RESPONSE:
			handleEnterParkingKioskResponse(message);
			break;

		case RETRIEVE_CAR_KIOSK_RESPONSE:
			handleRetrieveCarKioskResponse(message);
			break;

		case FORGOT_CODE_KIOSK_RESPONSE:
			handleForgotCodeKioskResponse(message);
			break;

		case ACTIVATE_RESERVATION_KIOSK_RESPONSE:
			handleActivateReservationKioskResponse(message);
			break;

		case PARKING_AVAILABILITY_RESPONSE:
			handleParkingAvailability(message);
			break;

		case RESERVATION_RESPONSE:
			handleReservationResponse(message);
			break;

		case REGISTRATION_RESPONSE:
			handleRegistrationResponse(message);
			break;

		case LOST_CODE_RESPONSE:
			handleLostCodeResponse(message);
			break;

		case PARKING_HISTORY_RESPONSE:
			handleParkingHistory(message);
			break;

		case MANAGER_SEND_REPORTS:
			handleReports(message);
			break;

		case ACTIVE_PARKINGS_RESPONSE:
			handleActiveParkings(message);
			break;

		case UPDATE_SUBSCRIBER_RESPONSE:
			handleUpdateResponse(message);
			break;

		case SUBSCRIBER_DATA_RESPONSE:
			handleSubscriberDataResponse(message);
			break;

		case ACTIVATION_RESPONSE:
			handleActivationResponse(message);
			break;

		case CANCELLATION_RESPONSE:
			handleCancellationResponse(message);
			break;

		case EXTENSION_RESPONSE:
			handleExtendParkingResponse(message);
			break;

		case EXIT_PARKING_RESPONSE:
			handleExitParkingResponse(message);
			break;

		case SHOW_SUBSCRIBER_DETAILS:
			ParkingSubscriber subscriber = (ParkingSubscriber) message.getContent();
			Platform.runLater(() -> {
				if (BParkClientScenes.getAttendantController() != null)
					BParkClientScenes.getAttendantController().showSubscriberDetails(subscriber);
				else if (BParkClientScenes.getManagerController() != null)
					BParkClientScenes.getManagerController().showSubscriberDetails(subscriber);
			});
			break;

		case SHOW_ALL_SUBSCRIBERS:
			List<ParkingSubscriber> subs = (List<ParkingSubscriber>) message.getContent();
			if (BParkClientScenes.getAttendantController() != null)
				BParkClientScenes.getAttendantController().updateSubscriberTable(subs);
			if (BParkClientScenes.getManagerController() != null)
				BParkClientScenes.getManagerController().updateSubscriberTable(subs);
			break;

		default:
			System.out.println("Unknown message type: " + message.getType());
		}
	}

	public static void handleStringMessage(String message) {
		String[] parts = message.split(" ", 2);
		String command = parts[0];
		String data = parts.length > 1 ? parts[1] : "";

		// Format long messages for better display
		String formattedData = data;
		if (data.contains("Access denied") && data.length() > 50) {
			formattedData = data.replaceFirst(": ", ":\n");
		} else if (data.contains("does not belong") || data.contains("not authorized")) {
			formattedData = data.replaceFirst("does not", "\ndoes not");
		}

		switch (command) {
		case "login:":
			handleStringLoginResponse(formattedData);
			break;

		case "availableSpots":
			showInfo("Available Spots", "Parking Update", "Current available spots: " + formattedData);
			break;

		case "enterResult":
			if (formattedData.contains("successful") || formattedData.contains("Entry recorded")) {
				showSuccess("Entry Result", "Welcome!", formattedData);
			} else {
				showError("Entry Failed", "Cannot Enter", formattedData);
			}
			break;

		case "exitResult":
			if (formattedData.contains("successful") || formattedData.contains("Thank you")) {
				showSuccess("Exit Result", "Goodbye!", formattedData);
			} else {
				showError("Exit Failed", "Cannot Exit", formattedData);
			}
			break;

		case "parkingCode":
			showInfo("Lost Code", "Your Parking Code", "Your parking code is: " + formattedData);
			break;

		case "reservationResult":
			if (formattedData.contains("confirmed") || formattedData.contains("successful")) {
				showSuccess("Reservation", "Success!", formattedData);
			} else {
				showError("Reservation", "Failed", formattedData);
			}
			break;

		default:
			System.out.println("Unknown string command: " + command);
		}
	}

	private static void handleLoginResponse(Message message) {
		ParkingSubscriber subscriber = (ParkingSubscriber) message.getContent();

		if (subscriber != null) {
			BParkClientScenes.setCurrentUser(subscriber.getSubscriberCode());
			BParkClientScenes.setCurrentUserID(subscriber.getSubscriberID());
			BParkClientScenes.setUserType(subscriber.getUserType());
			BParkClientScenes.switchToMainScreen(subscriber.getUserType());

			Platform.runLater(() -> LoginController.getInstance().handleLoginSuccess(subscriber.getUserType()));
		} else {
			Platform.runLater(() -> {
				LoginController.getInstance().handleLoginFailed("Invalid username/userCode or user not found.");
			});
		}
	}

	private static void handleKioskLoginResponse(Message message) {
		KioskController.handleKioskLoginResult(message.getContent());
	}

	private static void handleEnterParkingKioskResponse(Message message) {
		String response = (String) message.getContent();
		if (response.contains("successful") || response.contains("Entry recorded")) {
			// Extract parking code if present
			if (response.contains("code:")) {
				showSuccess("Enter Parking", "Entry Successful!", response);
			} else {
				showSuccess("Enter Parking", "Welcome!", response);
			}
		} else {
			showError("Enter Parking", "Entry Failed", response);
		}
	}

	private static void handleRetrieveCarKioskResponse(Message message) {
		String response = (String) message.getContent();
		if (response.contains("successful") || response.contains("Retrieved")) {
			showSuccess("Retrieve Car", "Car Retrieved!", response);
		} else {
			showError("Retrieve Car", "Retrieval Failed", response);
		}
	}

	private static void handleForgotCodeKioskResponse(Message message) {
		String response = (String) message.getContent();
		if (response.matches(".*\\d+.*")) { // Contains numbers (likely a code)
			showInfo("Parking Code", "Code Found", response);
		} else {
			showError("Parking Code", "Not Found", response);
		}
	}

	private static void handleActivateReservationKioskResponse(Message message) {
		String response = (String) message.getContent();
		
		// Format long messages
		String formattedResponse = response;
		if (response.contains("Invalid reservation code") && response.length() > 50) {
			formattedResponse = response.replaceFirst("or ", "or\n");
		}
		
		if (response.contains("activated") || response.contains("successful")) {
			showSuccess("Activate Reservation", "Activated!", formattedResponse);
		} else {
			showError("Activate Reservation", "Activation Failed", formattedResponse);
		}
	}

	private static void handleParkingAvailability(Message message) {
		Integer availableSpots = (Integer) message.getContent();
		
		// Check if this is from login screen
		if (LoginController.getInstance() != null) {
			LoginController.getInstance().showAvailabilityResult(availableSpots);
		} else {
			// General availability check
			String header = availableSpots > 0 ? "Parking Available" : "No Parking";
			String details = "Available spots: " + availableSpots;
			
			if (availableSpots == 0) {
				showError("Parking Availability", header, "Sorry, no parking spots are currently available.");
			} else if (availableSpots < 5) {
				showInfo("Parking Availability", header, details + "\n⚠️ Limited spots remaining!");
			} else {
				showSuccess("Parking Availability", header, details);
			}
		}
	}

	private static void handleReservationResponse(Message message) {
		String response = (String) message.getContent();
		
		if (response.startsWith("SUCCESS") || response.contains("confirmed")) {
			// Try to parse reservation details
			if (response.contains(",")) {
				try {
					// Expected format: "SUCCESS: Reservation confirmed for YYYY-MM-DD HH:MM. Confirmation code: XX. Spot: X"
					// Or simpler: "YYYY-MM-DD HH:MM,code,spot"
					String[] parts = response.split("[,.]");
					if (parts.length >= 3) {
						String dateTime = extractDateTime(response);
						String code = extractCode(response);
						String spot = extractSpot(response);
						
						if (dateTime != null && code != null && spot != null) {
							CustomDialog.showReservationSuccess(dateTime, code, spot);
							return;
						}
					}
				} catch (Exception e) {
					// Fall back to regular success dialog
				}
			}
			
			// Format long messages with line breaks
			String formattedResponse = response;
			if (response.length() > 50 && response.contains("confirmed for")) {
				formattedResponse = response.replaceFirst("\\. ", ".\n");
			}
			
			showSuccess("Reservation Success", "Confirmed!", formattedResponse);
		} else {
			showError("Reservation Failed", "Cannot Reserve", response);
		}
	}

	private static void handleRegistrationResponse(Message message) {
		String response = (String) message.getContent();
		if (response.startsWith("SUCCESS")) {
			showSuccess("Registration Success", "Welcome to BPark!", response);
		} else {
			showError("Registration Failed", "Cannot Register", response);
		}
	}

	private static void handleLostCodeResponse(Message message) {
		String code = (String) message.getContent();
		if (code.matches("\\d+")) {
			showInfo("Parking Code Recovery", "Code Found!", 
				"Your parking code is: " + code + "\n\n" + 
				"📧 This has also been sent to your email/SMS");
		} else {
			showError("Code Recovery Failed", "Not Found", code);
		}
	}

	@SuppressWarnings("unchecked")
	private static void handleParkingHistory(Message message) {
		ArrayList<ParkingOrder> history = (ArrayList<ParkingOrder>) message.getContent();
		
		// Open the parking history window with the received data
		Platform.runLater(() -> {
			BParkClientScenes.showParkingHistoryWindow(history);
		});
	}

	@SuppressWarnings("unchecked")
	private static void handleReports(Message message) {
		ArrayList<ParkingReport> reports = (ArrayList<ParkingReport>) message.getContent();

		ManagerController managerController = BParkClientScenes.getManagerController();
		if (managerController != null) {
			managerController.updateReports(reports);
		}
	}

	@SuppressWarnings("unchecked")
	private static void handleActiveParkings(Message message) {
		ArrayList<ParkingOrder> activeParkings = (ArrayList<ParkingOrder>) message.getContent();

		AttendantController controller = BParkClientScenes.getAttendantController();
		if (controller != null) {
			controller.updateActiveParkings(FXCollections.observableArrayList(activeParkings));
		}

		ManagerController managerController = BParkClientScenes.getManagerController();
		if (managerController != null) {
			managerController.updateActiveParkings(FXCollections.observableArrayList(activeParkings));
		}
	}

	private static void handleUpdateResponse(Message message) {
		String response = (String) message.getContent();
		if (response.contains("successful") || response.contains("updated")) {
			showSuccess("Update Profile", "Profile Updated!", response);
		} else {
			showError("Update Profile", "Update Failed", response);
		}
	}

	private static void handleSubscriberDataResponse(Message message) {
		ParkingSubscriber subscriber = (ParkingSubscriber) message.getContent();
		Platform.runLater(() -> {
			UpdateProfileController controller = BParkClientScenes.getUpdateProfileController();
			if (controller != null) {
				controller.setFieldPrompts(subscriber.getEmail(), subscriber.getPhoneNumber(), subscriber.getCarNumber());
			}
		});
	}

	private static void handleActivationResponse(Message message) {
		String response = (String) message.getContent();
		if (response.contains("successful") || response.contains("activated")) {
			showSuccess("Reservation Activated", "Ready to Park!", response);
		} else {
			showError("Activation Failed", "Cannot Activate", response);
		}
	}

	// String message handlers (legacy)

	private static void handleStringLoginResponse(String data) {
		if (!data.equals("None")) {
			BParkClientScenes.setUserType(data);
			BParkClientScenes.switchToMainScreen(data);
		} else {
			showError("Login Failed", "Authentication Error", "Invalid credentials");
		}
	}

	private static void handleCancellationResponse(Message message) {
		String response = (String) message.getContent();
		if (response.contains("successful") || response.contains("cancelled")) {
			showSuccess("Reservation Cancellation", "Cancelled", response);
		} else {
			showError("Cancellation Failed", "Cannot Cancel", response);
		}
	}

	private static void handleExtendParkingResponse(Message message) {
		String response = (String) message.getContent();
		ExtendParkingController controller = ExtendParkingController.instance;

		// Format the response for better display
		String formattedResponse = response;
		
		// Add line breaks for long messages
		if (response.contains("Cannot extend again") && response.contains("already extended")) {
			formattedResponse = response.replace(": ", ":\n");
		} else if (response.contains("extended by") && response.contains("until")) {
			// Format: "Parking time extended by X hours until DATE TIME"
			formattedResponse = response.replaceFirst("until", "\nuntil");
		}

		if (response.contains("extended") || response.contains("successful")) {
			if (controller != null) {
				controller.showSuccess(formattedResponse);
			} else {
				showSuccess("Extension Successful", "Time Extended!", formattedResponse);
			}
		} else {
			if (controller != null) {
				controller.showError(formattedResponse);
			} else {
				showError("Extension Failed", "Cannot Extend", formattedResponse);
			}
		}
	}

	private static void handleExitParkingResponse(Message message) {
		String response = (String) message.getContent();
		if (response.contains("Exit successful") || response.contains("Thank you")) {
			// Extract fee information if present
			if (response.contains("Fee:") || response.contains("$")) {
				showSuccess("Exit Successful", "Thank You!", response + "\n\n🚗 Drive safely!");
			} else {
				showSuccess("Exit Successful", "Goodbye!", response);
			}
		} else {
			showError("Exit Failed", "Cannot Exit", response);
		}
	}

	// Helper methods for parsing reservation response
	private static String extractDateTime(String response) {
		// Look for date pattern YYYY-MM-DD HH:MM
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}");
		java.util.regex.Matcher matcher = pattern.matcher(response);
		return matcher.find() ? matcher.group() : null;
	}

	private static String extractCode(String response) {
		// Look for "code: XX" or "Confirmation code: XX"
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:code:|Code:)\\s*(\\d+)");
		java.util.regex.Matcher matcher = pattern.matcher(response);
		return matcher.find() ? matcher.group(1) : null;
	}

	private static String extractSpot(String response) {
		// Look for "Spot: X" or "spot X"
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:spot:|Spot:)\\s*(\\d+)");
		java.util.regex.Matcher matcher = pattern.matcher(response);
		return matcher.find() ? matcher.group(1) : null;
	}

	public static byte[] serialize(Message msg) {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream out = new ObjectOutputStream(bos)) {
			out.writeObject(msg);
			out.flush();
			return bos.toByteArray();
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static Object deserialize(Object msg) {
		try (ByteArrayInputStream bis = new ByteArrayInputStream((byte[]) msg);
				ObjectInputStream in = new ObjectInputStream(bis)) {
			return in.readObject();
		} catch (IOException | ClassNotFoundException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	// Custom dialog helper methods
	private static void showInfo(String title, String header, String content) {
		Platform.runLater(() -> {
			CustomDialog.showInformation(title, header, content);
		});
	}

	private static void showError(String title, String header, String content) {
		Platform.runLater(() -> {
			CustomDialog.showError(title, header, content);
		});
	}

	private static void showSuccess(String title, String header, String content) {
		Platform.runLater(() -> {
			// Success messages use the info dialog with success styling
			CustomDialog.showInformation(title, header, content);
		});
	}
}