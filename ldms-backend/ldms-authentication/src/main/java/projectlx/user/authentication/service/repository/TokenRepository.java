package projectlx.user.authentication.service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.user.authentication.service.model.Token;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByToken(String token);
}
