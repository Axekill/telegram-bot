package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.TaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private static final String START = "/start";
    private static final DateTimeFormatter DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final Pattern PATTERN = Pattern.compile("([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)");


    @Autowired
    private TelegramBot telegramBot;
    private final TaskRepository repository;

    public TelegramBotUpdatesListener(TaskRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        for (Update update : updates) {
            if (update.message() != null && update.message().text() != null) {
                logger.info("Processing update: {}", update);
                var text = update.message().text();
                Long chatId = update.message().chat().id();
                if (text.equals(START)) {
                    String userName = update.message().chat().username();
                    String firstName = update.message().chat().firstName();
                    String lastName = update.message().chat().lastName();
                    startCommand(chatId, userName, firstName, lastName);
                } else {
                    // unknownCommand(chatId);
                    var matcher = PATTERN.matcher(text);
                    if (matcher.matches()) {
                        LocalDateTime dateTime = parse(matcher.group(1));
                        if (dateTime == null) {
                            new SendMessage(chatId,"Дата указана не верно");
                           // telegramBot.execute(new SendMessage(chatId, "Дата указана не верно"));
                            continue;
                        }
                        var taskText = matcher.group(3);
                        var saved = repository.save(new NotificationTask(chatId, taskText, dateTime));
                        telegramBot.execute(new SendMessage(chatId, "уведомление запланировано"));
                        logger.info("Notification saved: {}", saved);
                    }
                }
            }
        }
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private LocalDateTime parse(String text) {
        try {
            return LocalDateTime.parse(text, DATE_TIME_PATTERN);
        } catch (DateTimeParseException e) {
            logger.error("Cannot parse date and time: {}", text, e);
        }
        return null;
    }

    private void startCommand(Long chatId, String userName, String firstName, String lastName) {
        String textChat;
        String fullName = userName + firstName + lastName;
        if (userName == null & lastName == null) {
            textChat = "Добро пожаловать в бот, %s !";

            var formattedText = String.format(textChat, firstName);
            sendMessage(chatId, formattedText);
        } else if (fullName == null) {
            textChat = "Добро пожаловать в бот";
            sendMessage(chatId, textChat);
        } else {
            textChat = "Добро пожаловать в бот, %s !";
            var formattedText = String.format(textChat, userName);
            sendMessage(chatId, formattedText);
        }
    }

   /* private void unknownCommand(Long chatId) {
        var text = "Не удалось распознать команду!";
        sendMessage(chatId, text);
    }*/

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage(chatId, text);
        SendResponse response = telegramBot.execute(message);
    }


}
