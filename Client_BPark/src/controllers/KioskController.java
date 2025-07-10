package controllers;

import static common.Message.MessageType.KIOSK_ID_LOGIN;
import static common.Message.MessageType.KIOSK_RF_LOGIN;

import client.BParkKioskScenes;
import common.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.util.Pair;

public class KioskController {

	@FXML
	private Button btnLoginByID;

	@FXML
	private Button btnLoginByRF;

	@FXML
	private Button btnCheckAvailability;

	@FXML
	private Button btnExitKiosk;
	private static Stage mainStage;

	public static void setMainStage(Stage stage) {
		mainStage = stage;
	}

	@FXML
	private void handleCheckAvailability() {
		// Send a request to the server
		Message checkMsg = new Message(Message.MessageType.CHECK_PARKING_AVAILABILITY, null);
		BParkKioskScenes.sendMessage(checkMsg);
	}

	@FXML
	private void handleExitKiosk() {
		// Show custom confirmation dialog with proper button text
		int result = CustomDialog.showConfirmation(
			"Exit Kiosk",
			"Exit Application",
			"Are you sure you want to exit the kiosk application?",
			"Yes, Exit",
			"No, Stay"
		);
		
		if (result == CustomDialog.RESULT_YES) {
			try {
				// Disconnect from server if connected
				if (BParkKioskScenes.isConnected()) {
					BParkKioskScenes.sendStringMessage("KioskTerminating");
					BParkKioskScenes.disconnect();
				}

				// Close the application
				Platform.exit();
				System.exit(0);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@FXML
	private void handleLoginByID() {
		Platform.runLater(() -> {
			// Create custom login dialog
			try {
				FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/LoginByIDDialog.fxml"));
				Parent root = loader.load();
				
				LoginByIDDialogController controller = loader.getController();
				Stage dialogStage = LoginByIDDialogController.createDialog(root);
				controller.setDialogStage(dialogStage);
				
				dialogStage.showAndWait();
				
				Pair<String, String> result = controller.getResult();
				if (result != null) {
					String username = result.getKey();
					String userIDStr = result.getValue();
					try {
						int userID = Integer.parseInt(userIDStr);
						Message msg = new Message(KIOSK_ID_LOGIN, username + "," + userID);
						BParkKioskScenes.sendMessage(msg);
					} catch (NumberFormatException e) {
						CustomDialog.showError("Invalid Input", "Input Error", "User ID must be numeric.");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				// Fallback to simple input dialog
				showLoginByIDFallback();
			}
		});
	}
	
	// Fallback method if custom dialog fails to load
	private void showLoginByIDFallback() {
		// First get username
		String username = CustomDialog.showTextInput(
			"Login by ID",
			"Enter Username",
			"Please enter your username:",
			""
		);
		
		if (username != null && !username.isEmpty()) {
			// Then get user ID
			String userIDStr = CustomDialog.showTextInput(
				"Login by ID",
				"Enter User ID",
				"Please enter your User ID:",
				""
			);
			
			if (userIDStr != null && !userIDStr.isEmpty()) {
				try {
					int userID = Integer.parseInt(userIDStr);
					Message msg = new Message(KIOSK_ID_LOGIN, username + "," + userID);
					BParkKioskScenes.sendMessage(msg);
				} catch (NumberFormatException e) {
					CustomDialog.showError("Invalid Input", "Input Error", "User ID must be numeric.");
				}
			}
		}
	}

	@FXML
	private void handleLoginByRF() {
		String result = CustomDialog.showTextInput(
			"Login by RF",
			"Scan RF Card",
			"Enter your User ID:",
			""
		);
		
		if (result != null && !result.isEmpty()) {
			try {
				int userID = Integer.parseInt(result);
				Message msg = new Message(KIOSK_RF_LOGIN, userID);
				BParkKioskScenes.sendMessage(msg);
			} catch (NumberFormatException e) {
				CustomDialog.showError("Invalid Input", "Input Error", "Please enter a valid numeric User ID.");
			}
		}
	}

	public static void handleKioskLoginResult(Object content) {
		if (content instanceof String response) {
			if (!response.isEmpty()) {
				// Split "John Doe,4"
				String[] parts = response.split(",");
				if (parts.length == 2) {
					String name = parts[0].trim();
					int userID = Integer.parseInt(parts[1].trim());

					// Store for future operations
					KioskDashboardController.setLoggedInUser(name, userID);

					// Welcome message and load dashboard
					showWelcomeAndLoadDashboard(name);
				} else {
					showAlertStatic("Login Failed", "Invalid login data received from server.");
				}
			} else {
				showAlertStatic("Login Failed", "Invalid credentials or user not found.");
			}
		}
	}

	private void showAlert(String title, String message) {
		CustomDialog.showInformation(title, title, message);
	}

	private static void showAlertStatic(String title, String message) {
		Platform.runLater(() -> {
			CustomDialog.showError("Login Failed", title, message);
		});
	}

	private static void showWelcomeAndLoadDashboard(String name) {
		Platform.runLater(() -> {
			CustomDialog.showInformation("Login Success", "Welcome!", "Welcome " + name + "!");

			try {
				FXMLLoader loader = new FXMLLoader(KioskController.class.getResource("/client/KioskDashboard.fxml"));
				Parent dashboardRoot = loader.load();
				Scene dashboardScene = new Scene(dashboardRoot);
				dashboardScene.getStylesheets()
						.add(KioskController.class.getResource("/css/BParkStyle.css").toExternalForm());
				mainStage.setScene(dashboardScene);
			} catch (Exception e) {
				e.printStackTrace();
				showAlertStatic("Error", "Failed to load dashboard screen.");
			}
		});
	}
}