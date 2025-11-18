package co.edu.escuelaing.securetutorialauth0;

import java.time.LocalDateTime;

public class Message {
    private String message;
    private String clientIp;
    private LocalDateTime timestamp;

    public Message() {
    }

    public Message(String message, String clientIp, LocalDateTime timestamp) {
        this.message = message;
        this.clientIp = clientIp;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

