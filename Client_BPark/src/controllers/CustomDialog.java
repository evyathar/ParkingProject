package controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class CustomDialog {
    
    public static final int RESULT_YES = 1;
    public static final int RESULT_NO = 2;
    public static final int RESULT_CANCEL = 0;
    public static final int RESULT_OK = 1;
    
    private static int dialogResult = RESULT_CANCEL;
    private static String textInput = "";
    
    public static int getDialogResult() {
        return dialogResult;
    }
    
    public static void setDialogResult(int result) {
        dialogResult = result;
    }
    
    public static String getTextInput() {
        return textInput;
    }
    
    public static void setTextInput(String input) {
        textInput = input;
    }
    
    /**
     * Shows a confirmation dialog with Yes/No/Cancel options
     */
    public static int showConfirmation(String title, String header, String message) {
        return showConfirmation(title, header, message, "Yes", "No");
    }
    
    /**
     * Shows a confirmation dialog with custom button texts
     */
    public static int showConfirmation(String title, String header, String message, String yesText, String noText) {
        try {
            dialogResult = RESULT_CANCEL;
            
            FXMLLoader loader = new FXMLLoader(CustomDialog.class.getResource("/client/ConfirmationDialog.fxml"));
            Parent root = loader.load();
            
            ConfirmationDialogController controller = loader.getController();
            controller.setContent(title, header, message);
            controller.setButtonTexts(yesText, noText);
            
            Stage dialogStage = createDialogStage(root, title);
            dialogStage.showAndWait();
            
            return dialogResult;
        } catch (Exception e) {
            e.printStackTrace();
            return RESULT_CANCEL;
        }
    }
    
    /**
     * Shows a parking entry confirmation dialog (special case)
     */
    public static int showParkingEntryConfirmation() {
        try {
            dialogResult = RESULT_CANCEL;
            
            FXMLLoader loader = new FXMLLoader(CustomDialog.class.getResource("/client/ConfirmationDialog.fxml"));
            Parent root = loader.load();
            
            ConfirmationDialogController controller = loader.getController();
            controller.setContent("Parking Entry", "Do you have a parking reservation?", 
                "If you have a reservation code,\nplease select 'Yes' to activate it first.");
            controller.setButtonTexts("Yes, I have a reservation", "No, continue without reservation");
            
            Stage dialogStage = createDialogStage(root, "Parking Entry");
            dialogStage.showAndWait();
            
            return dialogResult;
        } catch (Exception e) {
            e.printStackTrace();
            return RESULT_CANCEL;
        }
    }
    
    /**
     * Shows an input dialog and returns the entered text
     */
    public static String showTextInput(String title, String header, String message, String defaultText) {
        try {
            textInput = "";
            
            FXMLLoader loader = new FXMLLoader(CustomDialog.class.getResource("/client/InputDialog.fxml"));
            Parent root = loader.load();
            
            InputDialogController controller = loader.getController();
            controller.setContent(title, header, message, defaultText);
            
            Stage dialogStage = createDialogStage(root, title);
            dialogStage.showAndWait();
            
            return textInput;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    
    /**
     * Shows an information dialog
     */
    public static void showInformation(String title, String header, String message) {
        try {
            FXMLLoader loader = new FXMLLoader(CustomDialog.class.getResource("/client/InfoDialog.fxml"));
            Parent root = loader.load();
            
            InfoDialogController controller = loader.getController();
            controller.setContent(title, header, message);
            
            Stage dialogStage = createDialogStage(root, title);
            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Shows an error dialog
     */
    public static void showError(String title, String header, String message) {
        try {
            FXMLLoader loader = new FXMLLoader(CustomDialog.class.getResource("/client/ErrorDialog.fxml"));
            Parent root = loader.load();
            
            ErrorDialogController controller = loader.getController();
            controller.setContent(title, header, message);
            
            Stage dialogStage = createDialogStage(root, title);
            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static Stage createDialogStage(Parent root, String title) {
        Stage dialogStage = new Stage();
        dialogStage.setTitle(title);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        
        // Try to load CSS - check if it exists first
        try {
            String cssPath = CustomDialog.class.getResource("/css/DialogStyle.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
        } catch (Exception e) {
            System.err.println("Warning: Could not load DialogStyle.css - " + e.getMessage());
        }
        
        dialogStage.setScene(scene);
        
        // Add shadow effect to the root
        DropShadow shadow = new DropShadow();
        shadow.setRadius(20);
        shadow.setOffsetX(0);
        shadow.setOffsetY(10);
        shadow.setColor(Color.color(0, 0, 0, 0.3));
        root.setEffect(shadow);
        
        // Make dialog draggable
        makeDraggable(root, dialogStage);
        
        // Center on screen
        dialogStage.centerOnScreen();
        
        return dialogStage;
    }
    
    /**
     * Shows a reservation success dialog with details
     */
    public static void showReservationSuccess(String dateTime, String code, String spot) {
        try {
            FXMLLoader loader = new FXMLLoader(CustomDialog.class.getResource("/client/SuccessDialog.fxml"));
            Parent root = loader.load();
            
            SuccessDialogController controller = loader.getController();
            controller.setContent("Reservation Success", "Reservation Confirmed!", "Your parking reservation has been successfully created.");
            controller.setReservationDetails(dateTime, code, spot);
            
            Stage dialogStage = createDialogStage(root, "Reservation Success");
            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to simple information dialog
            showInformation("Reservation Success", "Reservation Confirmed", 
                "Reservation confirmed for " + dateTime + ". Confirmation code: " + code + ". Spot: " + spot);
        }
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