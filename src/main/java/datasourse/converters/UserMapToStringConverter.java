package datasourse.converters;

import entities.ChatUser;

import javax.persistence.AttributeConverter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserMapToStringConverter implements AttributeConverter<Map<Integer, ChatUser>, String> {

    private static final String SPLIT_REGEX = "\uD83D\uDD4B\uD83D\uDC69\u200D❤️\u200D\uD83D\uDC8B\u200D\uD83D\uDC69\uD83D\uDC37";
    private static final String USER_SPLIT_REGEX = "\uD83E\uDD9A\uD83C\uDF28\uD83E\uDD52";

    @Override
    public String convertToDatabaseColumn(Map<Integer, ChatUser> users) {
        return convertMapToString(users);
    }

    @Override
    public Map<Integer, ChatUser> convertToEntityAttribute(String s) {
        Map<Integer, ChatUser> list = new HashMap<>();

        if (s.equals("")) return list;

        for (String info : s.split(SPLIT_REGEX)) {
            ChatUser chatUser = new ChatUser(info.split(USER_SPLIT_REGEX));

            list.put(chatUser.getUserId(), chatUser);
        }

        return list;
    }

    public static String convertMapToString(Map<Integer, ChatUser> users) {
        if (users.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        List<ChatUser> usersList = new ArrayList<>(users.values());

        sb.append(usersList.get(0).toDBString(USER_SPLIT_REGEX));
        for (int i = 1; i < users.size(); i++) {
            sb.append(SPLIT_REGEX).append(usersList.get(i).toDBString(USER_SPLIT_REGEX));
        }

        return sb.toString();
    }
}
