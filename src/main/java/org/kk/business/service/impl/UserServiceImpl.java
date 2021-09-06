package org.kk.business.service.impl;

import org.kk.architecture.annotation.Service;
import org.kk.business.bean.Result;
import org.kk.business.bean.User;
import org.kk.business.service.UserService;

/**
 * @author Steven
 */
@Service
public class UserServiceImpl implements UserService {

    @Override
    public Result<User> getUserInformation(Integer userId) {
        User steven = new User(userId, "steven","1376969568@qq.com","山西省吕梁市");
        return Result.success(steven);
    }
}
