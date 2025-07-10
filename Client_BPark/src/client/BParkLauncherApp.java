//package client;
//
//import common.Message;
//import controllers.LauncherController;
//import javafx.application.Application;
//import javafx.application.Platform;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.Parent;
//import javafx.scene.Scene;
//import javafx.stage.Stage;
//import ocsf.client.ObservableClient;
//
//public class BParkLauncherApp extends Application {
//
//	private static BParkClient client;
//	private static String serverIP = "localhost";
//	private static int serverPort = 5555;
//	private static Stage primaryStage;
//
//	@Override
//	public void start(Stage stage) throws Exception {
//		primaryStage = stage;
//		showLauncherScreen(stage);
//	}
//
//	private void showLauncherScreen(Stage stage) throws Exception {
//		FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/Launcher.fxml"));
//		Parent root = loader.load();
//
//		// Set main stage in the controller
//		LauncherController controller = loader.getController();
//		controller.setMainStage(stage);
//
//		Scene scene = new Scene(root);
//		scene.getStylesheets().add(getClass().getResource("/css/LauncherStyles.css").toExternalForm());
//		stage.setTitle("BPark - Connection Launcher");
//		stage.setScene(scene);
//		stage.setResizable(true);
//		stage.show();
//	}
//
//	public static boolean testConnection(String ip, int port) {
//		try {
//			// Close existing connection if any
//			if (client != null && client.isConnected()) {
//				client.closeConnection();
//			}
//
//			client = new BParkClient(ip, port);
//			client.openConnection();
//
//			// Update stored connection settings if successful
//			serverIP = ip;
//			serverPort = port;
//
//			return client.isConnected();
//		} catch (Exception e) {
//
//			return false;
//		}
//	}
//
//	public static void connectToServer() {
//		try {
//			client = new BParkClient(serverIP, serverPort);
//			client.openConnection();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	public static boolean isConnected() {
//		return client != null && client.isConnected();
//	}
//
//	public static void showClientScene() {
//		try {
//			BParkClientScenes.showLoginScreen(primaryStage);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	public static void showKioskScene() {
//		try {
//			BParkKioskScenes.showKioskScreen(primaryStage);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	// Client communication class
//	static class BParkClient extends ObservableClient {
//		public BParkClient(String host, int port) {
//			super(host, port);
//		}
//
//		@Override
//		protected void handleMessageFromServer(Object msg) {
//			Platform.runLater(() -> {
//				try {
//					Object message = msg;
//					if (message instanceof byte[]) {
//						message = ClientMessageHandler.deserialize(msg);
//					}
//
//					if (message instanceof Message) {
//						ClientMessageHandler.handleMessage((Message) message);
//					} else if (message instanceof String) {
//						ClientMessageHandler.handleStringMessage((String) message);
//					}
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			});
//		}
//
//		@Override
//		protected void connectionClosed() {
//			System.out.println("Connection closed");
//		}
//
//		@Override
//		protected void connectionException(Exception exception) {
//			System.out.println("Connection error: " + exception.getMessage());
//		}
//	}
//
//	// Utility methods for sending messages
//	public static void sendMessage(Message msg) {
//		try {
//			if (client != null && client.isConnected()) {
//				client.sendToServer(ClientMessageHandler.serialize(msg));
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	public static void sendStringMessage(String msg) {
//		try {
//			if (client != null && client.isConnected()) {
//				client.sendToServer(msg);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	// Getters
//	public static BParkClient getClient() {
//		return client;
//	}
//
//	public static String getServerIP() {
//		return serverIP;
//	}
//
//	public static int getServerPort() {
//		return serverPort;
//	}
//
//	public static Stage getPrimaryStage() {
//		return primaryStage;
//	}
//
//	public static void disconnect() {
//		try {
//			if (client != null && client.isConnected()) {
//				client.sendToServer("ClientDisconnect");
//				client.closeConnection();
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	@Override
//	public void stop() throws Exception {
//		// Clean up when application closes
//		disconnect();
//		super.stop();
//	}
//
//	public static void main(String[] args) {
//		launch(args);
//	}
//}


package client;

import common.Message;
import controllers.LauncherController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import ocsf.client.ObservableClient;

public class BParkLauncherApp extends Application {

	private static BParkClient client;
	private static String serverIP = "localhost";
	private static int serverPort = 5555;
	private static Stage primaryStage;

	@Override
	public void start(Stage stage) throws Exception {
		primaryStage = stage;
		
		// Set the application icon using the utility method
		setStageIcon(stage);
		
		showLauncherScreen(stage);
	}

	private void showLauncherScreen(Stage stage) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/Launcher.fxml"));
		Parent root = loader.load();

		// Set main stage in the controller
		LauncherController controller = loader.getController();
		controller.setMainStage(stage);

		Scene scene = new Scene(root);
		scene.getStylesheets().add(getClass().getResource("/css/LauncherStyles.css").toExternalForm());
		stage.setTitle("BPark - Connection Launcher");
		stage.setScene(scene);
		stage.setResizable(true);
		stage.show();
	}

	/**
	 * Utility method to set the BPark icon for any stage
	 * This ensures consistent branding across all application windows
	 * @param stage The stage to set the icon for
	 */
	public static void setStageIcon(Stage stage) {
		if (stage == null) {
			System.err.println("Cannot set icon: Stage is null");
			return;
		}
		
		try {
			// Try multiple loading methods for better compatibility
			Image icon = null;
			
			// Method 1: Using getResourceAsStream
			if (BParkLauncherApp.class.getResourceAsStream("/images/bpark.png") != null) {
				icon = new Image(BParkLauncherApp.class.getResourceAsStream("/images/bpark.png"));
			}
			// Method 2: Using direct path if method 1 fails
			else {
				icon = new Image("/images/bpark.png");
			}
			
			// Check if icon loaded successfully
			if (icon != null && !icon.isError()) {
				stage.getIcons().clear(); // Clear any existing icons
				stage.getIcons().add(icon);
			} else {
				System.err.println("Icon loaded but contains errors");
			}
			
		} catch (Exception e) {
			System.err.println("Could not load application icon: " + e.getMessage());
			// Application continues without icon - not a critical error
		}
	}

	public static boolean testConnection(String ip, int port) {
		try {
			// Close existing connection if any
			if (client != null && client.isConnected()) {
				client.closeConnection();
			}

			client = new BParkClient(ip, port);
			client.openConnection();

			// Update stored connection settings if successful
			serverIP = ip;
			serverPort = port;

			return client.isConnected();
		} catch (Exception e) {
			return false;
		}
	}

	public static void connectToServer() {
		try {
			client = new BParkClient(serverIP, serverPort);
			client.openConnection();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean isConnected() {
		return client != null && client.isConnected();
	}

	public static void showClientScene() {
		try {
			// Set icon for the client scene window
			setStageIcon(primaryStage);
			BParkClientScenes.showLoginScreen(primaryStage);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void showKioskScene() {
		try {
			// Set icon for the kiosk scene window
			setStageIcon(primaryStage);
			BParkKioskScenes.showKioskScreen(primaryStage);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Client communication class
	static class BParkClient extends ObservableClient {
		public BParkClient(String host, int port) {
			super(host, port);
		}

		@Override
		protected void handleMessageFromServer(Object msg) {
			Platform.runLater(() -> {
				try {
					Object message = msg;
					if (message instanceof byte[]) {
						message = ClientMessageHandler.deserialize(msg);
					}

					if (message instanceof Message) {
						ClientMessageHandler.handleMessage((Message) message);
					} else if (message instanceof String) {
						ClientMessageHandler.handleStringMessage((String) message);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}

		@Override
		protected void connectionClosed() {
			System.out.println("Connection closed");
		}

		@Override
		protected void connectionException(Exception exception) {
			System.out.println("Connection error: " + exception.getMessage());
		}
	}

	// Utility methods for sending messages
	public static void sendMessage(Message msg) {
		try {
			if (client != null && client.isConnected()) {
				client.sendToServer(ClientMessageHandler.serialize(msg));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void sendStringMessage(String msg) {
		try {
			if (client != null && client.isConnected()) {
				client.sendToServer(msg);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Getters
	public static BParkClient getClient() {
		return client;
	}

	public static String getServerIP() {
		return serverIP;
	}

	public static int getServerPort() {
		return serverPort;
	}

	public static Stage getPrimaryStage() {
		return primaryStage;
	}

	public static void disconnect() {
		try {
			if (client != null && client.isConnected()) {
				client.sendToServer("ClientDisconnect");
				client.closeConnection();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() throws Exception {
		// Clean up when application closes
		disconnect();
		super.stop();
	}

	public static void main(String[] args) {
		launch(args);
	}
}