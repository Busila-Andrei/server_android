package com.example.server_android.token;

import java.util.List;
import java.util.Optional;

import com.example.server_android.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TokenRepository extends JpaRepository<Token, Integer> {

    @Query(value = """
      select t from Token t inner join User u\s
      on t.user.id = u.id\s
      where u.id = :id and (t.expired = false or t.disable = false)\s
      """)
    List<Token> findAllValidTokenByUser(Integer id);
    Optional<Token> findByToken(String token);
    List<Token> findByUser(User user);
}
