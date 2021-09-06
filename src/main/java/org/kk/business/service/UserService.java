package org.kk.business.service;

import org.kk.business.bean.Result;
import org.kk.business.bean.User;

/**
 * @author Steven
 */
public interface UserService {

    public Result<User> getUserInformation(Integer userId);
}
