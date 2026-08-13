package com.back.daycare.service;

import com.back.daycare.dto.response.UserResponse;
import com.back.daycare.exception.ResourceNotFoundException;
import com.back.daycare.mapper.UserMapper;
import com.back.daycare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserResponse getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .map(userMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + username));
    }
}


