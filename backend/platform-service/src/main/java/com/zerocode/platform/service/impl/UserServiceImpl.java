package com.zerocode.platform.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zerocode.platform.mapper.UserMapper;
import com.zerocode.platform.model.UserEntity;
import com.zerocode.platform.service.UserService;
import com.zerocode.platform.vo.LoginVO;
import com.zerocode.platform.vo.UserVO;
import java.time.LocalDateTime;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public LoginVO register(String username, String password) {
        UserEntity existing = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, username)
                .last("LIMIT 1"));
        if (existing != null) {
            throw new IllegalArgumentException("Username already exists");
        }

        UserEntity user = new UserEntity();
        user.setId(System.currentTimeMillis());
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("user");
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);

        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();
        return new LoginVO(token, toVO(user));
    }

    @Override
    public LoginVO login(String username, String password) {
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, username)
                .last("LIMIT 1"));
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();
        return new LoginVO(token, toVO(user));
    }

    @Override
    public UserVO getCurrentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        return toVO(user);
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    private UserVO toVO(UserEntity user) {
        return new UserVO(user.getId(), user.getUsername(), user.getRole(), user.getCreateTime());
    }
}
