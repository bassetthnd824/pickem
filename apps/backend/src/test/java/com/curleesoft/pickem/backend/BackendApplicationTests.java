package com.curleesoft.pickem.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.curleesoft.pickem.backend.support.FirestoreEmulatorSupport;
import com.google.cloud.firestore.Firestore;

@SpringBootTest
class BackendApplicationTests extends FirestoreEmulatorSupport {

    @Autowired
    private Firestore firestore;

    @Test
    void contextLoads() {
    }

    @Test
    void bootsAgainstTheFirestoreEmulator() {
        assertThat(firestore.listCollections()).isNotNull();
        firestore.collection("seasons").limit(1).get();
    }
}
