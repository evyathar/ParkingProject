package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class InfoDialogController {
    
    @FXML private Label lblHeader;
    @FXML private Label lblMessage;
    @FXML private Button btnOK;
    
    public void setContent(String title, String header, String message) {
        if (header != null && !header.isEmpty()) {
            lblHeader.setText(header);
        }
        if (message != null && !message.isEmpty()) {
            lblMessage.setText(message);
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