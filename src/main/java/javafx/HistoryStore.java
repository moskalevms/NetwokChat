package javafx;

import org.apache.commons.lang3.ArrayUtils;
import javafx.Message;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HistoryStore {

    private static final Pattern RECORD_PATTERN = Pattern.compile("(\\S+\\s\\S+)\\|(\\w+)\\|(.*)");
    private static final String RECORD_TEMPLATE = "%s|%s|%s";

    private static final String HISTORY_DIR = "history";
    private File file;
    private String username;

    public HistoryStore(String username) {
        this.username = username;
        this.file = createFile(username);
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File createFile(String username) {
        return new File(HISTORY_DIR, username + "-history.txt");
    }


    public void rename(String newUsername) {
        this.username = newUsername;
        File newFile = createFile(newUsername);
        this.file.renameTo(newFile);
        this.file = newFile;
    }

    public void saveRecord(Message record) {
        try (BufferedWriter wr = new BufferedWriter(new FileWriter(file, true))) {
            wr.write(String.format(RECORD_TEMPLATE, record.getTimestamp(), record.getUserFrom(), record.getText()));
            wr.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Message> getLastMessages() {
        List<String> history = new ArrayList<>();
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            int historyLength = 100;

            long pointer = file.length() - 1;

            List<Byte> bytes = new LinkedList<>();
            for (int count = 0; pointer >= 0 && count < historyLength; pointer--) {
                raf.seek(pointer);
                int chr = raf.read();
                if (chr == '\n') {
                    if (bytes.size() > 1) {
                        Collections.reverse(bytes);
                        byte[] record = ArrayUtils.toPrimitive(bytes.toArray(new Byte[0]));
                        history.add(new java.lang.String(record).replace("\r", ""));
                        bytes.clear();
                    }
                    count++;
                }
                else {
                    bytes.add((byte) chr);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<Message> result = history.stream()
                .map(record -> {
                    Matcher matcher = RECORD_PATTERN.matcher(record);
                    if (matcher.matches()) {
                        LocalDateTime timestamp = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern(Message.DATE_FORMAT));
                        String fromUser = matcher.group(2);
                        String text = matcher.group(3);
                        return new Message(fromUser, null, text, timestamp);
                    }
                    return new Message(null, null, null);
                })
                .collect(Collectors.toList());

        Collections.reverse(result);
        return result;

    }

    public static void main(String[] args) {
        LocalDateTime.parse("11/03/2019 01:01:17", DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }
}
