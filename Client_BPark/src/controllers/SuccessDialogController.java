package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SuccessDialogController {
    
    @FXML private Label lblHeader;
    @FXML private Label lblMessage;
    @FXML private Label lblDate;
    @FXML private Label lblCode;
    @FXML private Label lblSpot;
    @FXML private VBox detailsBox;
    @FXML private Button btnOK;
    
    public void setContent(String title, String header, String message) {
        if (header != null && !header.isEmpty()) {
            lblHeader.setText(header);
        }
        if (message != null && !message.isEmpty()) {
            lblMessage.setText(message);
        }
        
        // Hide details box by default
        detailsBox.setVisible(false);
        detailsBox.setManaged(false);
    }
    
    public void setReservationDetails(String date, String code, String spot) {
        // Show details box
        detailsBox.setVisible(true);
        detailsBox.setManaged(true);
        
        // Set the details
        if (date != null) {
            lblDate.setText("📅 Date: " + date);
        }
        if (code != null) {
            lblCode.setText("🎫 Confirmation Code: " + code);
        }
        if (spot != null) {
            lblSpot.setText("🅿️ Parking Spot: " + spot);
        }
    }
    
    @FXML
    private void handleOK(ActionEvent event) {
        closeDialog();
    }
    
    private void closeDialog() {
        Stage stage = (Stage) btnOK.getScene().getWindow();
        stage.close();
    }
}