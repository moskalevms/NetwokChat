package javafx;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message {

    public static final String DATE_FORMAT = "dd/MM/yyyy HH:mm:ss";
    private String userFrom;

    private String userTo;

    private String text;

    private LocalDateTime timestamp;

    public Message(String userFrom, String userTo, String text) {
        this(userFrom, userTo, text, LocalDateTime.now());
    }

    public Message(String userFrom, String userTo, String text, LocalDateTime timestamp) {
        this.userFrom = userFrom;
        this.userTo = userTo;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getUserFrom() {
        return userFrom;
    }

    public String getUserTo() {
        return userTo;
    }

    public String getText() {
        return text;
    }

    public String getTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
    }
}
