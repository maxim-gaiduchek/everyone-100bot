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
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import utils.Formatter;
import utils.SimpleSender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main extends TelegramLongPollingBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static final String BOT_USERNAME = System.getenv("EVERYONE_100BOT_TELEGRAM_USERNAME");
    private static final String BOT_TOKEN = System.getenv("EVERYONE_100BOT_TELEGRAM_TOKEN");
    /*private static final String BOT_USERNAME = System.getenv("TEST_BOT_TELEGRAM_USERNAME");
    private static final String BOT_TOKEN = System.getenv("TEST_BOT_TELEGRAM_TOKEN");*/
    private static final String DONATIONALERTS_LINK = System.getenv("DONATIONALERTS_LINK");
    private static final long DEV_CHAT_ID = 505457346L;
    private static final long WAIT_TO_DELETE_MILLIS = 5000;

    private final SimpleSender sender = new SimpleSender(BOT_TOKEN);
    private static final ApplicationContext CONTEXT = new AnnotationConfigApplicationContext(DatasourceConfig.class);
    public static final Service SERVICE = (Service) CONTEXT.getBean("service");

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
        new Thread(() -> {
            LOGGER.debug(update.toString());

            if (update.hasMessage()) {
                parseMessage(update.getMessage());
            }
        }).start();
    }

    // message parsing

    private void parseMessage(Message message) {
        //LOGGER.debug(message.getNewChatMembers().toString());

        if (message.isCommand()) {
            parseCommand(message);
        } else if (message.isUserMessage()) {
            sendUserMessage(message.getChatId());
        }
        if (message.isGroupMessage() || message.isSuperGroupMessage()) {
            parseGroupMessage(message);
        } else if (!message.isUserMessage()) {
            sender.leaveChat(message.getChatId());
        }
    }

    private void parseCommand(Message message) {
        Long chatId = message.getChatId();
        boolean isUserMessage = message.isUserMessage();

        switch (message.getText()) {
            case "/everyone", "/everyone@Everyone100Bot" -> {
                if (isUserMessage) sendCommandCannotBeUsed(chatId);
            }
            case "/switchmute", "/switchmute@Everyone100Bot" -> {
                if (isUserMessage) {
                    sendCommandCannotBeUsed(chatId);
                } else {
                    switchMuteCommand(chatId, message.getFrom().getId(), message.getMessageId());
                }
            }
            case "/help", "/help@Everyone100Bot" -> helpCommand(chatId, isUserMessage);
            case "/donate", "/donate@Everyone100Bot" -> donateCommand(chatId);
            case "/sendstats" -> sendStatistics(chatId, isUserMessage);
            default -> {
                if (isUserMessage) sendUserMessage(chatId);
            }
        } // TODO change "/help@Everyone100Bot" to "/help@" + BOT_USERNAME
    }

    private void sendUserMessage(Long chatId) {
        String msg = """
                *Привет! Я - бот для упоминания всех пользователей в чате* (практически всех). Сначала добавь меня в твой чат. Что я буду в нем делать: напиши @everyone, /everyone, /everyone@Everyone100Bot или @Everyone100Bot, и я упомяну всех в чате, чтоб они обратили внимание на твое сообщение

                *Примечание:* из-за того, что Телеграм не дает ботам информацию про пользователей чата, я обхожу это ограничение по-другому. Я сохраняю тех юзеров, которые написали хоть раз пока я был в чате, потом их упоминаю. *Так что я не всех смогу упомянуть!*
                                
                *Команды*
                /everyone - Упомянуть всех
                /switchmute - Выкл./вкл. упоминание себя
                /help - Как пользоваться ботом
                /donate - Помочь творителю""";

        sender.sendStringAndInlineKeyboard(chatId, msg, getDonationKeyboard());
    }

    private void sendCommandCannotBeUsed(Long chatId) {
        String msg = """
                Добавь меня в чат, чтоб использовать эту команду :)""";

        sender.sendString(chatId, msg);
    }

    private void parseGroupMessage(Message message) {
        Long chatId = message.getChat().getId();
        Integer messageId = message.getMessageId();

        if (!chatsByChatIds.containsKey(chatId)) {
            sendFirstGroupMessage(chatId);
        }

        BotChat chat = getChat(chatId);

        addUser(chat, message.getFrom());
        if (message.isReply()) {
            addUser(chat, message.getReplyToMessage().getFrom());
        }
        if (!message.getNewChatMembers().isEmpty()) {
            addUsers(chat, message.getNewChatMembers());
        }
        try {
            if (message.getLeftChatMember() != null && !getMe().equals(message.getLeftChatMember())) {
                chat.deleteUser(message.getLeftChatMember().getId());
            } // TODO case if this bot is kicked
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        if (isBotCalled(message.getEntities())) {
            sendReply(chat, chatId, messageId);
        }

        SERVICE.saveBotChat(chat);
    }

    private void sendFirstGroupMessage(Long chatId) {
        String msg = """
                *Привет! Я - бот для упоминания всех пользователей в чате* (практически всех). Что я буду делать в чате: напиши @everyone, /everyone, /everyone@Everyone100Bot или @Everyone100Bot, и я упомяну всех в чате, чтоб они обратили внимание на твое сообщение

                *Примечание:* из-за того, что Телеграм не дает ботам информацию про пользователей чата, я обхожу это ограничение по-другому. Я сохраняю тех юзеров, которые написали хоть раз пока я был в чате, потом их упоминаю. *Так что я не всех смогу упомянуть!*
                                
                *Команды*
                /everyone - Упомянуть всех
                /switchmute - Выкл./вкл. упоминание себя
                /help - Как пользоваться ботом
                /donate - Помочь творителю""";

        sender.sendStringAndInlineKeyboard(chatId, msg, getDonationKeyboard());
    }

    // bot actions

    private BotChat getChat(Long chatId) {
        BotChat chat;

        if (chatsByChatIds.containsKey(chatId)) {
            chat = SERVICE.getBotChat(chatsByChatIds.get(chatId));
        } else {
            chat = new BotChat(chatId);
            SERVICE.saveBotChat(chat);
            chatsByChatIds.put(chatId, chat.getId());
        }

        return chat;
    }

    private void addUser(BotChat chat, User user) {
        if (user != null && !user.getIsBot()) chat.addUser(new ChatUser(user));
    }

    private void addUsers(BotChat chat, List<User> users) {
        users.forEach(user -> addUser(chat, user));
    }

    private boolean isBotCalled(List<MessageEntity> entities) {
        if (entities != null) {
            for (MessageEntity entity : entities) {
                if (entity.getText().equals("/everyone@" + BOT_USERNAME) ||
                    entity.getText().equals("@" + BOT_USERNAME) ||
                    entity.getText().equals("/everyone") ||
                    entity.getText().equals("@everyone")) return true;
            }
        }
        return false;
    }

    private void sendReply(BotChat chat, Long chatId, Integer messageId) {
        StringBuilder sb = new StringBuilder();
        List<ChatUser> users = chat.getUsers();
        int noReplyCounter = 0;

        users.sort((u1, u2) -> {
            if (chat.isMuted(u1.getUserId()) && chat.isMuted(u2.getUserId())) {
                return u1.getName().compareTo(u2.getName());
            } else if (chat.isMuted(u1.getUserId())) {
                return 1;
            } else if (chat.isMuted(u2.getUserId())) {
                return -1;
            } else {
                return u1.getName().compareTo(u2.getName());
            }
        });

        for (ChatUser user : users) {
            if (!chat.isMuted(user.getUserId())) {
                sb.append("[").append(user.getName()).append("](tg://user?id=").append(user.getUserId()).append(") ");
            } else {
                sb.append(Formatter.formatTelegramText(user.getName())).append(" ");
                noReplyCounter++;
            }
        }

        int replies = users.size() - noReplyCounter;

        sb.append("_(").append(replies).append(" упомянуто");
        if (noReplyCounter > 0) sb.append(", ").append(noReplyCounter).append(" не упомянуто");
        sb.append(")_");

        sender.sendString(chatId, sb.toString(), messageId);
        chat.incrementCallCounter();
    }

    // commands

    private void switchMuteCommand(Long chatId, Integer userId, Integer messageId) {
        BotChat chat = getChat(chatId);
        boolean isMuted = chat.switchMute(userId);
        String msg = "Теперь я " + (isMuted ? "не " : "") + "буду вас упоминать";

        SERVICE.saveBotChat(chat);
        sender.sendString(chatId, msg);

        /*try {
            Message message = sender.sendString(chatId, msg);
            sender.deleteMessage(chatId, messageId);
            Thread.sleep(WAIT_TO_DELETE_MILLIS);
            sender.deleteMessage(chatId, message.getMessageId());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

    } // TODO get admin rights

    private void helpCommand(Long chatId, boolean isUserMessage) {
        String msg;

        if (isUserMessage) {
            msg = """
                    *Я - бот для упоминания всех пользователей в чате* (практически всех). Сначала добавь меня в твой чат. Что я буду в нем делать: напиши @everyone, /everyone, /everyone@Everyone100Bot или @Everyone100Bot, и я упомяну всех в чате, чтоб они обратили внимание на твое сообщение

                    *Примечание:* из-за того, что Телеграм не дает ботам информацию про пользователей чата, я обхожу это ограничение по-другому. Я сохраняю тех юзеров, которые написали хоть раз пока я был в чате, потом их упоминаю. *Так что я не всех смогу упомянуть!*
                                    
                    *Команды*
                    /everyone - Упомянуть всех
                    /switchmute - Выкл./вкл. упоминание себя
                    /help - Как пользоваться ботом
                    /donate - Помочь творителю""";
        } else {
            msg = """
                    *Я - бот для упоминания всех пользователей в чате* (практически всех). Что я буду делать в чате: напиши @everyone, /everyone, /everyone@Everyone100Bot или @Everyone100Bot, и я упомяну всех в чате, чтоб они обратили внимание на твое сообщение

                    *Примечание:* из-за того, что Телеграм не дает ботам информацию про пользователей чата, я обхожу это ограничение по-другому. Я сохраняю тех юзеров, которые написали хоть раз пока я был в чате, потом их упоминаю. *Так что я не всех смогу упомянуть!*
                                    
                    *Команды*
                    /everyone - Упомянуть всех
                    /switchmute - Выкл./вкл. упоминание себя
                    /help - Как пользоваться ботом
                    /donate - Помочь творителю""";
        }

        sender.sendString(chatId, msg);
    }

    private void donateCommand(Long chatId) {
        String msg = "Творитель будет рад любой мелочи <3";

        sender.sendStringAndInlineKeyboard(chatId, msg, getDonationKeyboard());
    }

    // admin commands

    private void sendStatistics(Long chatId, boolean isUserMessage) {
        if (chatId.equals(DEV_CHAT_ID)) {
            String msg = "Бота добавлено в *" + chatsByChatIds.size() + " чата(-ов)*!\n" +
                         "Ботом воспользовались *" + SERVICE.getSumOfCallCounters() + " раза(-)*!";

            sender.sendString(DEV_CHAT_ID, msg);
        } else {
            if (isUserMessage) sendUserMessage(chatId);
        }
    }

    // keyboards

    private List<List<InlineKeyboardButton>> getDonationKeyboard() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        row.add(InlineKeyboardButton.builder().text("Помочь моему творителю").url(DONATIONALERTS_LINK).build());
        keyboard.add(row);

        return keyboard;
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
