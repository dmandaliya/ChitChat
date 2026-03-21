package com.chitchat.server.repository;

import com.chitchat.server.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM UserEntity u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.fname) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.lname) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<UserEntity> searchByUsernameOrName(@Param("q") String q);
}
