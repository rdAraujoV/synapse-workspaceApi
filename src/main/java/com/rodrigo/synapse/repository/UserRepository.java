package com.rodrigo.synapse.repository;

import com.rodrigo.synapse.entity.UserEntity;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

@Repository 
public interface UserRepository extends JpaRepository<UserEntity, Long>{
    Optional<UserEntity> findByEmail (String email);
}