package co.edu.escuelaing.securetutorialauth0;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MessageService {
    private final Map<String, Message> messages = new HashMap<>();

    public Message addMessage(String messageText, String clientIp) {
        String id = UUID.randomUUID().toString();
        Message message = new Message(messageText, clientIp, LocalDateTime.now());
        messages.put(id, message);
        return message;
    }

    public List<Message> getLastMessages(int count) {
        List<Message> messageList = new ArrayList<>(messages.values());
        messageList.sort((m1, m2) -> m2.getTimestamp().compareTo(m1.getTimestamp()));
        
        int size = Math.min(count, messageList.size());
        return messageList.subList(0, size);
    }
}

