module com.chitchat.client {
    requires java.desktop;
    requires javafx.controls;
    requires javafx.fxml;
    requires com.chitchat.shared;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires okhttp3;
    requires org.java_websocket;

    opens com.chitchat.client to javafx.fxml;
    opens com.chitchat.client.controller to javafx.fxml;
    opens com.chitchat.client.model to javafx.fxml;
    opens com.chitchat.client.service to javafx.fxml;

    exports com.chitchat.client;
}
