package com.zerocode.platform.service;

import com.zerocode.platform.vo.LoginVO;
import com.zerocode.platform.vo.UserVO;

public interface UserService {

    LoginVO register(String username, String password);

    LoginVO login(String username, String password);

    UserVO getCurrentUser();

    void logout();
}
