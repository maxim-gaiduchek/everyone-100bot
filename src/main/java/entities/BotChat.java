package entities;

import datasourse.converters.IntegerListToStringConverter;
import datasourse.converters.UserMapToStringConverter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "chats")
public class BotChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "chat_id")
    private long chatId;

    @Column(name = "users")
    @Convert(converter = UserMapToStringConverter.class)
    private final Map<Integer, ChatUser> users = new HashMap<>();

    @Column(name = "muted")
    @Convert(converter = IntegerListToStringConverter.class)
    private final List<Integer> muted = new ArrayList<>();

    private BotChat() {
    }

    public BotChat(long chatId) {
        this.chatId = chatId;
    }

    // getters

    public int getId() {
        return id;
    }

    public long getChatId() {
        return chatId;
    }

    public List<ChatUser> getUsers() {
        return new ArrayList<>(users.values());
    }

    public boolean isMuted(Integer userId) {
        return muted.contains(userId);
    }

    // setters

    public void addUser(ChatUser user) {
        Integer userId = user.getUserId();

        if (users.containsKey(userId)) {
            users.get(userId).setName(user.getName());
        } else {
            users.put(userId, user);
        }
    }

    public void deleteUser(Integer userId) {
        users.remove(userId);
    }

    public boolean switchMute(Integer userId) {
        boolean isMuted = isMuted(userId);

        if (isMuted) {
            muted.remove(userId);
        } else {
            muted.add(userId);
        }

        return !isMuted;
    }

    // core

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BotChat botChat)) return false;

        return id == botChat.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return "BotChat{" +
                "id=" + id +
                ", chatId=" + chatId +
                ", users=" + users +
                ", muted=" + muted +
                '}';
    }
}
