package com.sparta.springnewsfeed.service;

import com.sparta.springnewsfeed.config.EmailAlreadyExistsException;
import com.sparta.springnewsfeed.config.InvalidCredentialsException;
import com.sparta.springnewsfeed.dto.UserLoginRequestDto;
import com.sparta.springnewsfeed.dto.UserLoginResponseDto;
import com.sparta.springnewsfeed.dto.UserSignupRequestDto;
import com.sparta.springnewsfeed.dto.UserSignupResponseDto;
import com.sparta.springnewsfeed.entity.User;
import com.sparta.springnewsfeed.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserSignupResponseDto signup(UserSignupRequestDto requestDto) {
        // email 중복체크
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new EmailAlreadyExistsException("이미 사용중인 이메일입니다");
        }

        User user = new User(requestDto.getEmail(),requestDto.getPassword(), requestDto.getNickname(), null);
        userRepository.save(user);

        return new UserSignupResponseDto(user.getId(), user.getEmail(), user.getNickname());
    }

    @Transactional
    public UserLoginResponseDto login(UserLoginRequestDto requestDto) {
        Optional<User> userOptional = userRepository.findByEmail(requestDto.getEmail());
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (requestDto.getPassword().equals(user.getPassword())) {
                String token = generateToken(user);
                return new UserLoginResponseDto(token, user.getEmail(), user.getNickname());
            }
        }
        throw new InvalidCredentialsException("잘못된 이메일 또는 비밀번호입니다");
    }

    private String generateToken(User user) {
        return "generated-jwt-token";
    }


}