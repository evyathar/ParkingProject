# 🚗 BPark – Smart Parking Management System

**BPark** is a Java-based desktop application for managing parking operations. Built with **JavaFX** for the GUI and **OCSF** (Object Client Server Framework) for client-server communication, it allows customers and parking staff to interact with a centralized **MySQL** database to manage reservations, monitor parking availability, generate reports, and more.

---

## 🛠 Project Architecture

The system follows a **Client–Server** architecture:

- **Client** – A JavaFX application that provides role-based user interfaces for:
  - Subscribers
  - Attendants
  - Managers

- **Server** – A Java-based server application that listens for client requests, handles logic, and communicates with the MySQL database.

- **Database** – A local MySQL database stores all operational data: subscribers, reservations, parking activity, reports, etc.

Communication between client and server is handled via the **OCSF** framework.

---

## 🔗 What is OCSF?

**OCSF (Object Client Server Framework)** is an open-source Java library that simplifies the creation of client-server applications. It enables communication using serialized Java objects (messages) and provides built-in classes for:

- Managing connections
- Handling server requests
- Extending behavior via inheritance

In this project, OCSF is used to:
- Send/receive `Message` objects between client and server
- Maintain connection pools
- Handle different types of messages (e.g., login, reservation, reports)

---

## 💡 Technologies Used

| Component           | Technology                 | Description                                               |
|---------------------|-----------------------------|-----------------------------------------------------------|
| Language            | Java (Java SE 21)           | Main programming language                                 |
| GUI                 | JavaFX                      | Desktop GUI framework                                     |
| Database            | MySQL                       | Stores parking and subscriber data                        |
| DB Connection       | JDBC                        | Java Database Connectivity                                |
| Networking          | OCSF (OCSF.client/server)   | Handles TCP connections and object message passing        |
| IDE                 | Eclipse                     | Development environment                                   |

---

## ✨ Key Features

- 🔐 Login by role (Subscriber, Attendant, Manager)
- 🅿️ Real-time parking availability tracking
- 📅 Make, activate, or cancel reservations
- 🔁 Extend parking duration from client side
- 👤 Subscriber registration and profile update
- 🧾 Monthly reports and statistics for managers
- 📬 Lost code recovery via in-app request
- 📊 View parking history

---

## 🧱 Project Structure

src/

├── client/         # JavaFX GUI logic for the client application

├── server/         # Server-side application logic and OCSF server

├── controllers/    # Controllers for business logic (e.g., login, reservation, reports)

├── entities/       # Shared data models passed between client and server (e.g., Message, Subscriber)

├── services/       # Background services such as auto-cancellation

├── gui/            # FXML UI layout files for the client

├── serverGUI/      # JavaFX GUI for server setup (e.g., port selection screen)

├── ocsf.client/    # OCSF client networking classes

├── ocsf.server/    # OCSF server networking classes

├── common/         # Utility classes shared across components (if any)

├── resources/      # Static resources like CSS, images, and FXML (often merged with gui)
