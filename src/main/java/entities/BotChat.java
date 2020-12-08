package entities;

import datasourse.converters.UserSetToStringConverter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

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
    @Convert(converter = UserSetToStringConverter.class)
    private List<ChatUser> users = new ArrayList<>();

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
        return users;
    }

    // setters

    public void addUser(ChatUser user) {
        if (!users.contains(user)) users.add(user);
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
                '}';
    }
}
