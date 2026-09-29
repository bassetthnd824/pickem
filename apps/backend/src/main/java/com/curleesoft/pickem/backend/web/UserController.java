package com.curleesoft.pickem.backend.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.curleesoft.pickem.backend.model.User;
import com.curleesoft.pickem.backend.service.UserService;

import jakarta.validation.Valid;

/**
 * Manager CRUD for users. Replaces {@code UserAction}. Role changes are copied
 * to Firebase custom claims. Password create and reset are not exposed.
 */
@RestController
@RequestMapping("/api/manager/users")
@PreAuthorize("hasRole('MANAGER')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<User> list(@RequestParam(required = false) String emailAddr,
            @RequestParam(required = false) String firstName, @RequestParam(required = false) String lastName) {
        return userService.search(emailAddr, firstName, lastName);
    }

    @GetMapping("/{id}")
    public User get(@PathVariable String id) {
        return userService.get(id);
    }

    @PostMapping
    public ResponseEntity<User> create(@Valid @RequestBody User request) {
        User saved = userService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}").buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    public User update(@PathVariable String id, @Valid @RequestBody User request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
