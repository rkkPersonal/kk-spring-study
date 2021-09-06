package org.kk.business.web;

import org.kk.architecture.annotation.*;
import org.kk.business.bean.Result;
import org.kk.business.bean.User;
import org.kk.business.service.UserService;

/**
 * @author Steven
 */
@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @RequestMapping("/search")
    @ResponseBody
    public Result<User> geUserInformation(@RequestParam("userId") Integer userId) {
        return userService.getUserInformation(userId);
    }


}
