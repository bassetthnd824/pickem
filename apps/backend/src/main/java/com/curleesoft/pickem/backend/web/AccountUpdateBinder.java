package com.curleesoft.pickem.backend.web;

import java.util.Locale;
import java.util.Set;

import com.curleesoft.pickem.backend.service.AccountService;
import com.curleesoft.pickem.backend.service.AccountUpdate;
import com.curleesoft.pickem.backend.service.InvalidRequestException;

import tools.jackson.databind.JsonNode;

/**
 * Binds {@code PUT /api/game/account}. Password names are rejected before the
 * body becomes an {@link AccountUpdate}. Any other unknown field is ignored.
 */
final class AccountUpdateBinder {

    private static final Set<String> PASSWORD_FIELDS = Set.of("password", "oldpass", "userpass", "confirmpass");

    private AccountUpdateBinder() {
    }

    static AccountUpdate bind(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw new InvalidRequestException(AccountService.REQUEST_INVALID);
        }

        for (String name : body.propertyNames()) {
            if (name != null && PASSWORD_FIELDS.contains(name.toLowerCase(Locale.ROOT))) {
                throw new InvalidRequestException(AccountService.PASSWORD_REJECTED);
            }
        }

        return new AccountUpdate(text(body.get("nickName")), text(body.get("themeId")), version(body.get("version")));
    }

    private static String text(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        if (!node.isString()) {
            throw new InvalidRequestException(AccountService.REQUEST_INVALID);
        }

        return node.asString();
    }

    private static Long version(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        if (!node.isIntegralNumber()) {
            throw new InvalidRequestException(AccountService.REQUEST_INVALID);
        }

        return Long.valueOf(node.longValue());
    }
}
