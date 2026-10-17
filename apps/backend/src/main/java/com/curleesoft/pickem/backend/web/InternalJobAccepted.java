package com.curleesoft.pickem.backend.web;

import java.util.Objects;

public record InternalJobAccepted(String job) {

    public InternalJobAccepted {
        Objects.requireNonNull(job, "job");
    }
}
