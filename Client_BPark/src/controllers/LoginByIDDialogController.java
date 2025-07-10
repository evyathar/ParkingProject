package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Pair;

public class LoginByIDDialogController {
    
    @FXML private TextField txtUsername;
    @FXML private TextField txtUserID;
    @FXML private Button btnLogin;
    @FXML private Button btnCancel;
    
    private Stage dialogStage;
    private Pair<String, String> result = null;
    
    @FXML
    public void initialize() {
        // Allow Enter key to submit from either field
        txtUsername.setOnKeyPressed((KeyEvent event) -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleLogin(null);
            }
        });
        
        txtUserID.setOnKeyPressed((KeyEvent event) -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleLogin(null);
            }
        });
        
        // Add listener to enable/disable login button
        txtUsername.textProperty().addListener((obs, oldVal, newVal) -> 
            validateFields()
        );
        
        txtUserID.textProperty().addListener((obs, oldVal, newVal) -> 
            validateFields()
        );
        
        // Initially disable login button
        btnLogin.setDisable(true);
    }
    
    private void validateFields() {
        boolean isValid = !txtUsername.getText().trim().isEmpty() && 
                         !txtUserID.getText().trim().isEmpty();
        btnLogin.setDisable(!isValid);
    }
    
    @FXML
    private void handleLogin(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String userID = txtUserID.getText().trim();
        
        if (!username.isEmpty() && !userID.isEmpty()) {
            result = new Pair<>(username, userID);
            closeDialog();
        }
    }
    
    @FXML
    private void handleCancel(ActionEvent event) {
        result = null;
        closeDialog();
    }
    
    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
    
    public Pair<String, String> getResult() {
        return result;
    }
    
    public Stage showAndWait() {
        // Dialog stage is created and shown by the parent
        // This method is called after the dialog is loaded
        return dialogStage;
    }
    
    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }
    
    // Static method to create and show the dialog
    public static Stage createDialog(Parent root) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        
        try {
            scene.getStylesheets().add(
                LoginByIDDialogController.class.getResource("/css/DialogStyle.css").toExternalForm()
            );
        } catch (Exception e) {
            // Continue without custom styles
        }
        
        stage.setScene(scene);
        
        // Add shadow effect
        DropShadow shadow = new DropShadow();
        shadow.setRadius(20);
        shadow.setOffsetX(0);
        shadow.setOffsetY(10);
        shadow.setColor(Color.color(0, 0, 0, 0.3));
        root.setEffect(shadow);
        
        // Make dialog draggable
        makeDraggable(root, stage);
        
        return stage;
    }
    
    private static void makeDraggable(Parent root, Stage stage) {
        final double[] xOffset = new double[1];
        final double[] yOffset = new double[1];
        
        root.setOnMousePressed(event -> {
            xOffset[0] = event.getSceneX();
            yOffset[0] = event.getSceneY();
        });
        
        root.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset[0]);
            stage.setY(event.getScreenY() - yOffset[0]);
        });
    }
}