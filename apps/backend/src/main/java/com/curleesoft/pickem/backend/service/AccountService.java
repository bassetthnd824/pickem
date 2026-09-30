package com.curleesoft.pickem.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.curleesoft.pickem.backend.model.User;
import com.curleesoft.pickem.backend.repository.UserRepository;

/**
 * Player profile. Ports {@code AccountAction} without passwords. Email, first
 * name, and last name stay on the Google profile. {@code themeId} is an
 * existing theme path; the closed catalog of 18 keys is US-36.
 */
@Service
public class AccountService {

    public static final String PASSWORD_REJECTED = "password fields are not accepted";

    public static final String NICK_REQUIRED = "nickName is required";

    public static final String NICK_TOO_LONG = "nickName must be 40 characters or fewer";

    public static final String THEME_REQUIRED = "themeId is required";

    public static final String REQUEST_INVALID = "Request is invalid";

    public static final int NICK_NAME_MAX = 40;

    private final UserRepository userRepository;

    private final ThemeService themeService;

    public AccountService(UserRepository userRepository, ThemeService themeService) {
        this.userRepository = userRepository;
        this.themeService = themeService;
    }

    public AccountProfile profile(String uid) {
        return toProfile(user(uid));
    }

    public AccountProfile update(String uid, AccountUpdate request) {
        String nickName = SearchText.trim(request.nickName());

        if (!StringUtils.hasText(nickName)) {
            throw new InvalidRequestException(NICK_REQUIRED);
        }

        if (nickName.length() > NICK_NAME_MAX) {
            throw new InvalidRequestException(NICK_TOO_LONG);
        }

        String themeId = SearchText.trim(request.themeId());

        if (!StringUtils.hasText(themeId)) {
            throw new InvalidRequestException(THEME_REQUIRED);
        }

        if (!themeService.pathExists(themeId)) {
            throw new InvalidRequestException(ThemeService.NOT_FOUND);
        }

        User existing = user(uid);
        existing.setNickName(nickName);
        existing.setThemeId(themeId);
        existing.setVersion(request.version());
        return toProfile(userRepository.save(existing));
    }

    private User user(String uid) {
        return userRepository.findById(uid).orElseThrow(() -> new ResourceNotFoundException(UserService.NOT_FOUND));
    }

    private static AccountProfile toProfile(User user) {
        return new AccountProfile(user.getEmailAddr(), user.getFirstName(), user.getLastName(), user.getNickName(),
                user.getThemeId(), user.getVersion());
    }
}
