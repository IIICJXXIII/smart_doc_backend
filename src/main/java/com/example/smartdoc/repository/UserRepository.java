package com.example.smartdoc.repository;

import com.example.smartdoc.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // JPA 会自动根据方法名生成 SQL：select * from sys_user where username = ?
    User findByUsername(String username);
}