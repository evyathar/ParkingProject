package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

public class InputDialogController {
    
    @FXML private Label lblHeader;
    @FXML private Label lblMessage;
    @FXML private TextField txtInput;
    @FXML private Button btnOK;
    @FXML private Button btnCancel;
    
    @FXML
    public void initialize() {
        // Allow Enter key to submit
        txtInput.setOnKeyPressed((KeyEvent event) -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleOK(null);
            }
        });
        
        // Focus on text field when dialog opens
        txtInput.requestFocus();
    }
    
    public void setContent(String title, String header, String message, String defaultText) {
        if (header != null && !header.isEmpty()) {
            lblHeader.setText(header);
        }
        if (message != null && !message.isEmpty()) {
            lblMessage.setText(message);
        }
        if (defaultText != null) {
            txtInput.setText(defaultText);
            txtInput.selectAll();
        }
    }
    
    @FXML
    private void handleOK(ActionEvent event) {
        String input = txtInput.getText().trim();
        CustomDialog.setTextInput(input);
        closeDialog();
    }
    
    @FXML
    private void handleCancel(ActionEvent event) {
        CustomDialog.setTextInput("");
        closeDialog();
    }
    
    private void closeDialog() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
}