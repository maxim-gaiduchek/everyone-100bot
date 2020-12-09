package main;

import datasourse.DatasourceConfig;
import datasourse.Service;
import entities.BotChat;
import entities.ChatUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import utils.SimpleSender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main extends TelegramLongPollingBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static final String BOT_USERNAME = "Everyone100Bot";
    private static final String BOT_TOKEN = "1294580446:AAHVEH1Qugkowk0UZgo0iBEgCu-z7dEi9s0";
    /*private static final String BOT_USERNAME = System.getenv("TEST_BOT_TELEGRAM_USERNAME");
    private static final String BOT_TOKEN = System.getenv("TEST_BOT_TELEGRAM_TOKEN");*/

    private final SimpleSender sender = new SimpleSender(BOT_TOKEN);
    private static final ApplicationContext CONTEXT = new AnnotationConfigApplicationContext(DatasourceConfig.class);
    private static final Service SERVICE = (Service) CONTEXT.getBean("service");

    private final Map<Long, Integer> chatsByChatIds = new HashMap<>();

    // setup

    private Main() {
        setChatsByChatIds();
    }

    private void setChatsByChatIds() {
        LOGGER.debug("Chats:");

        for (BotChat chat : SERVICE.getBotChats()) {
            chatsByChatIds.put(chat.getChatId(), chat.getId());

            LOGGER.debug("\t" + chat);
        }
    }

    // parsing

    @Override
    public void onUpdateReceived(Update update) {
        LOGGER.debug(update.toString());

        if (update.hasMessage()) {
            parseMessage(update.getMessage());
        }
    }

    // message parsing

    private void parseMessage(Message message) {
        //LOGGER.debug(message.getNewChatMembers().toString());

        if (message.isUserMessage()) {
            sendUserMessage(message.getChatId());
        } else if (message.isGroupMessage()) {
            parseGroupMessage(message);
        }
    }

    private void sendUserMessage(Long chatId) {
        String msg = """
                *Привет! Я - бот для упоминания всех юзеров в чате* (практически всех). Сначала добавь меня в твой чат. Что я буду в нем делать: добавь @everyone или /everyone к своему сообщению и я упомяну всех в чате, чтоб они обратили на твое сообщение

                *Примечание:* из-за того, что Телеграм не дает ботам информацию про пользователей чата, я обхожу это ограничение по-другому. Я сохраняю тех юзеров, которые написали хоть раз пока я был в чате, потом их упоминаю. *Так что я не всех смогу упомянуть!*
                
                Помочь моему творителю: https://www.donationalerts.com/r/saxxxarius""";

        sender.sendString(chatId, msg);
    }

    private void parseGroupMessage(Message message) {
        Chat chat = message.getChat();
        Long chatId = chat.getId();
        Integer messageId = message.getMessageId();

        if (!chatsByChatIds.containsKey(chatId)) {
            sendFirstGroupMessage(chatId);
        }

        BotChat botChat = addUserAndGetChat(chatId, message.getFrom());
        if (message.isReply()) {
            addUserAndGetChat(chatId, message.getReplyToMessage().getFrom());
        }

        if (isBotCalled(message.getEntities())) {
            sendReply(botChat, chatId, messageId);
        }
    }

    private void sendFirstGroupMessage(Long chatId) {
        String msg = """
                *Привет! Я - бот для упоминания всех юзеров в чате* (практически всех). Что я буду делать делать в чате: добавь @everyone или /everyone к своему сообщению и я упомяну всех в чате, чтоб они обратили на твое сообщение

                *Примечание:* из-за того, что Телеграм не дает ботам информацию про пользователей чата, я обхожу это ограничение по-другому. Я сохраняю тех юзеров, которые написали хоть раз пока я был в чате, потом их упоминаю. *Так что я не всех смогу упомянуть!*
                
                Помочь моему творителю: https://www.donationalerts.com/r/saxxxarius""";

        sender.sendString(chatId, msg);
    }

    // bot actions

    private BotChat addUserAndGetChat(Long chatId, User user) {
        BotChat chat = null;

        if (!user.getIsBot()) {
            boolean isFirstTime = false;

            if (chatsByChatIds.containsKey(chatId)) {
                chat = SERVICE.getBotChat(chatsByChatIds.get(chatId));
            } else {
                chat = new BotChat(chatId);
                isFirstTime = true;
            }

            chat.addUser(new ChatUser(user));
            SERVICE.saveBotChat(chat);

            if (isFirstTime) chatsByChatIds.put(chatId, chat.getId());
        }

        return chat;
    }

    private boolean isBotCalled(List<MessageEntity> entities) {
        if (entities != null) {
            for (MessageEntity entity : entities) {
                if (entity.getText().equals("@everyone") ||
                        entity.getText().equals("/everyone") ||
                        entity.getText().equals("/everyone@" + BOT_USERNAME)) return true;
            }
        }
        return false;
    }

    private void sendReply(BotChat chat, Long chatId, Integer messageId) {
        StringBuilder sb = new StringBuilder();

        for (ChatUser user : chat.getUsers()) {
            sb.append("[").append(user.getName()).append("](tg://user?id=").append(user.getUserId()).append(") ");
        }

        sender.sendString(chatId, sb.toString(), messageId);
    }

    // main

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    public static void main(String[] args) {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);

            telegramBotsApi.registerBot(new Main());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
