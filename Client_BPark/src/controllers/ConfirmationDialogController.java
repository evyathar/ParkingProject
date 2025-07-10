package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ConfirmationDialogController {
    
    @FXML private Label lblHeader;
    @FXML private Label lblMessage;
    @FXML private Button btnYes;
    @FXML private Button btnNo;
    @FXML private Button btnCancel;
    
    private boolean showNoButton = true;
    
    public void setContent(String title, String header, String message) {
        if (header != null && !header.isEmpty()) {
            lblHeader.setText(header);
        }
        if (message != null && !message.isEmpty()) {
            // If message is very long, add natural line breaks
            String formattedMessage = message;
            if (message.length() > 60 && !message.contains("\n")) {
                // Try to break at commas or periods
                if (message.contains(", ")) {
                    formattedMessage = message.replace(", ", ",\n");
                } else if (message.contains(". ")) {
                    formattedMessage = message.replace(". ", ".\n");
                }
            }
            lblMessage.setText(formattedMessage);
        }
    }
    
    public void setButtonTexts(String yesText, String noText) {
        if (yesText != null) {
            btnYes.setText(yesText);
        }
        if (noText != null) {
            btnNo.setText(noText);
        }
    }
    
    public void hideNoButton() {
        showNoButton = false;
        btnNo.setVisible(false);
        btnNo.setManaged(false);
    }
    
    @FXML
    private void handleYes(ActionEvent event) {
        CustomDialog.setDialogResult(CustomDialog.RESULT_YES);
        closeDialog();
    }
    
    @FXML
    private void handleNo(ActionEvent event) {
        CustomDialog.setDialogResult(CustomDialog.RESULT_NO);
        closeDialog();
    }
    
    @FXML
    private void handleCancel(ActionEvent event) {
        CustomDialog.setDialogResult(CustomDialog.RESULT_CANCEL);
        closeDialog();
    }
    
    private void closeDialog() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
}