package datasourse.converters;

import entities.ChatUser;

import javax.persistence.AttributeConverter;
import java.util.ArrayList;
import java.util.List;

public class UserListToStringConverter implements AttributeConverter<List<ChatUser>, String> {

    private static final String SPLIT_REGEX = "\uD83D\uDD4B\uD83D\uDC69\u200D❤️\u200D\uD83D\uDC8B\u200D\uD83D\uDC69\uD83D\uDC37";
    private static final String USER_SPLIT_REGEX = "\uD83E\uDD9A\uD83C\uDF28\uD83E\uDD52";

    @Override
    public String convertToDatabaseColumn(List<ChatUser> longs) {
        if (longs.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();

        sb.append(longs.get(0).toDBString(USER_SPLIT_REGEX));
        for (int i = 1; i < longs.size(); i++) {
            sb.append(SPLIT_REGEX).append(longs.get(i).toDBString(USER_SPLIT_REGEX));
        }

        return sb.toString();
    }

    @Override
    public List<ChatUser> convertToEntityAttribute(String s) {
        List<ChatUser> list = new ArrayList<>();

        if (s.equals("")) return list;

        for (String info : s.split(SPLIT_REGEX)) list.add(new ChatUser(info.split(USER_SPLIT_REGEX)));

        return list;
    }
}
