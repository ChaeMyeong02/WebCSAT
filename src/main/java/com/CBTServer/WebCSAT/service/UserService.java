package com.CBTServer.WebCSAT.service;

import com.CBTServer.WebCSAT.domain.User;
import com.CBTServer.WebCSAT.dto.UserDTO;
import com.CBTServer.WebCSAT.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/* Service about UserManagementSystem */
@RequiredArgsConstructor
@Service
public class UserService {
    // DI
    private final UserRepository userRepository;

    // Save NewUser
    public Long save(UserDTO dto) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        User entity = new User();
        entity.setEmail(dto.getEmail());
        entity.setNickname(dto.getNickname());

        //TODO Have To Set Admin Account
        entity.setRole("USER");
        entity.setPassword(encoder.encode(dto.getPassword()));

        User savedUser = userRepository.save(entity);
        return savedUser.getId();
    }


    // Search User data By ID
    public User findById(Long userId) {
        return userRepository.findById(userId).orElseThrow(()-> new IllegalArgumentException(userId + " : 존재하지 않는 ID 입니다."));
    }

    // Search User data By Email because Email is Unique
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(()-> new IllegalArgumentException(email + " : 해당 이메일은 존재하지 않습니다."));
    }

    public UserDTO apiUserInfo(String email) {
        User user = findByEmail(email);
        UserDTO userDTO = new UserDTO();
        userDTO.setEmail(user.getEmail());
        userDTO.setNickname(user.getEmail());
        userDTO.setOauth2(user.getOauth2());
        userDTO.setRole(user.getRole());
        userDTO.setCreated_at(user.getCreatedAt());
        return userDTO;
    }

    public ArrayList<UserDTO> apiAllUserInfo() {
        List<User> userList = userRepository.findAll();
        ArrayList<UserDTO> userDTOList = new ArrayList<>();
        for (User u : userList) {
            UserDTO userDTO = new UserDTO();
            userDTO.setEmail(u.getEmail());
            userDTO.setNickname(u.getEmail());
            userDTO.setOauth2(u.getOauth2());
            userDTO.setRole(u.getRole());
            userDTO.setCreated_at(u.getCreatedAt());
            userDTOList.add(userDTO);
        }
        return userDTOList;
    }
}
