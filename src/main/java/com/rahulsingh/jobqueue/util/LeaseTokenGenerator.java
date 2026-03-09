package com.rahulsingh.jobqueue.util;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class LeaseTokenGenerator {

    public String newToken() {
        return UUID.randomUUID().toString();
    }
}
