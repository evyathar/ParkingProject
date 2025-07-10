package controllers;

import client.BParkClientScenes;
import common.Message;
import common.Message.MessageType;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Controller class for handling the parking extension feature in the JavaFX GUI.
 * <p>
 * This controller is responsible for:
 * <ul>
 * <li>Initializing the hour selection combo box.</li>
 * <li>Validating user input (parking code and duration).</li>
 * <li>Sending extension requests to the server via {@link BParkClientApp}.</li>
 * <li>Displaying success or error messages to the user.</li>
 * </ul>
 *
 * <p>This controller is intended to be used with an FXML file defining the UI.</p>
 */

public class ExtendParkingController {

	/**
	 * Singleton-reference to the current instance of this controller.
	 */
	public static ExtendParkingController instance;

	/**
	 * Text field for the user to enter their unique parking code.
	 */
	@FXML private TextField codeField;

	/**
	 * ComboBox for selecting the number of hours to extend the parking.
	 */
	@FXML private ComboBox<String> hoursCombo;

	/**
	 * Label used to display validation messages from server responses.
	 */
	@FXML private Label statusLabel;

	/**
	 * Initializes the controller after its root element has been completely processed.
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Assigns the static instance reference.</li>
	 * <li>Populates the {@code hoursCombo} with values from 1 to 4 hours.</li>
	 * <li>Sets the default selected value to "1".</li>
	 * </ul>
	 */
	@FXML
	public void initialize() {
		instance = this;
		hoursCombo.getItems().addAll("1", "2", "3", "4");
		hoursCombo.setValue("1");
	}

	/**
	 * Handles the submit button click event.
	 * <p>
	 * This method performs the following:
	 * <ul>
	 * <li>Retrieves the parking code and extension hours selected by the user.</li>
	 * <li>Validates that the code field is not empty.</li>
	 * <li>Creates a {@link Message} object with type {@code REQUEST_EXTENSION} and sends it to the server.</li>
	 * <li>Now includes user ID for ownership validation.</li>
	 * </ul>
	 */
	@FXML
	private void handleSubmit() {
		String code = codeField.getText();
		String hours = hoursCombo.getValue();

		if (code == null || code.trim().isEmpty()) {
			// Show error dialog instead of just updating label
			CustomDialog.showError(
				"Invalid Input", 
				"Missing Code", 
				"Please enter a valid parking code."
			);
			codeField.requestFocus();
			return;
		}

		// Get current user ID for validation
		int userID = BParkClientScenes.getCurrentUserID();
		if (userID == 0) {
			CustomDialog.showError(
				"Authentication Error", 
				"User Not Found", 
				"Error: User not logged in properly.\nPlease log out and log in again."
			);
			return;
		}

		// Show confirmation dialog
		int result = CustomDialog.showConfirmation(
			"Confirm Extension",
			"Extend Parking Time?",
			"Do you want to extend your parking by " + hours + " hour(s)?\nParking code: " + code
		);

		if (result == CustomDialog.RESULT_YES) {
			// New format: parkingCode,hours,userID (with validation)
			String extensionData = code + "," + hours + "," + userID;
			
			// Update status label to show processing
			setStatusMessage("Processing extension request...", "#3498DB");
			
			// Create and send the extension request message
			Message msg = new Message(MessageType.REQUEST_EXTENSION, extensionData);
			BParkClientScenes.sendMessage(msg);
		}
	}

	/**
	 * Updates the status label with a custom message and text color.
	 * <p>
	 * This method is typically used to inform the user of the result of an action,
	 * such as a successful extension or an input error.
	 * It can be called from other parts of the application.
	 *
	 * @param msg The text message to be displayed in the status label.
	 * @param color The color of the text, defined using a CSS color name
	 * or a hexadecimal color code.
	 */
	public void setStatusMessage(String msg, String color) {
		statusLabel.setText(msg);
		statusLabel.setStyle("-fx-text-fill: " + color + ";");
	}

	/**
	 * Shows a success dialog and updates the status label
	 * 
	 * @param message The success message to display
	 */
	public void showSuccess(String message) {
		CustomDialog.showInformation(
			"Extension Successful",
			"Success!",
			message
		);
		setStatusMessage("Extension successful!", "#27AE60");
		// Clear the code field for next use
		codeField.clear();
	}

	/**
	 * Shows an error dialog and updates the status label
	 * 
	 * @param message The error message to display
	 */
	public void showError(String message) {
		CustomDialog.showError(
			"Extension Failed",
			"Error",
			message
		);
		setStatusMessage("Extension failed. Please try again.", "#E74C3C");
	}
}