// src/main/java/com/ijin/hanaro/user/UserRepository.java
package com.ijin.hanaro.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);

    @Query("""
        select u
        from User u
        where u.role = :role
          and (
               :q is null or :q = ''
               or lower(u.username) like lower(concat('%', :q, '%'))
               or lower(u.nickname) like lower(concat('%', :q, '%'))
               or lower(u.phone)    like lower(concat('%', :q, '%'))
          )
        """)
    Page<User> searchUsers(@Param("q") String q,
                           @Param("role") User.Role role,
                           Pageable pageable);
}