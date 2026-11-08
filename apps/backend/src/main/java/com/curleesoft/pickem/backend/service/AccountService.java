package com.curleesoft.pickem.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.curleesoft.pickem.backend.model.Theme;
import com.curleesoft.pickem.backend.model.User;
import com.curleesoft.pickem.backend.repository.UserRepository;

/**
 * Player profile. Ports {@code AccountAction} without passwords. Email, first
 * name, and last name stay on the Google profile. {@code themeId} must be one
 * of the 18 compiled keys. A player may keep an inactive key they already have;
 * choosing a different inactive key is rejected.
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

    public List<ThemeChoice> themeChoices() {
        return themeService.activeChoices();
    }

    public AccountProfile update(String uid, AccountUpdate request) {
        User existing = user(uid);
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

        if (!ThemeCatalog.contains(themeId)) {
            throw new InvalidRequestException(ThemeService.KEY_NOT_IN_CATALOG);

        }

        Theme theme = themeService.findByPath(themeId)
                .orElseThrow(() -> new InvalidRequestException(ThemeService.NOT_FOUND));
        boolean keepingCurrent = themeId.equals(existing.getThemeId());

        if (!Boolean.TRUE.equals(theme.getActive()) && !keepingCurrent) {
            throw new InvalidRequestException(ThemeService.THEME_INACTIVE);

        }

        existing.setNickName(nickName);
        existing.setThemeId(themeId);
        existing.setVersion(request.version());
        return toProfile(userRepository.save(existing));
    }

    private User user(String uid) {
        return userRepository.findById(uid).orElseThrow(() -> new ResourceNotFoundException(UserService.NOT_FOUND));
    }

    private static AccountProfile toProfile(User user) {
        return new AccountProfile(
                user.getEmailAddr(),
                user.getFirstName(),
                user.getLastName(),
                user.getNickName(),
                user.getThemeId(),
                user.getVersion()
        );
    }
}
