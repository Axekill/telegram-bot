package pro.sky.telegrambot.scheduler;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.repository.TaskRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class Scheduler {
    @Autowired
    private final TelegramBot telegramBot;
    private final TaskRepository repository;

    public Scheduler(TelegramBot telegramBot, TaskRepository repository) {
        this.telegramBot = telegramBot;
        this.repository = repository;
    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void notifyTask() {
        repository.findAllByDateTime(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                .forEach(task->{
                    telegramBot.execute(new SendMessage(task.getChatId(), task.getText()));
                    repository.delete(task);
                });

    }
}
