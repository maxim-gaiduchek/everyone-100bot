package datasourse.repositories;

import entities.BotChat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BotChatRepository extends JpaRepository<BotChat, Integer> {
}
