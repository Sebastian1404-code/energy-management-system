package distributedSystem.UserManagement.controller;

import distributedSystem.UserManagement.dto.*;
import distributedSystem.UserManagement.model.UserEntity;
import distributedSystem.UserManagement.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserEntity> getAllUsers() {
        return userService.getAllUsers();
    }

    @PostMapping("/id-by-username")
    public ResponseEntity<Long> getUserIdByUsername(@RequestBody UsernameDTO username) {
        Optional<Long> userOpt = userService.getUserIdByUsername(username.getUsername());
        return userOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }


    @GetMapping("/{id}")
    public ResponseEntity<UserEntity> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest user) {
        UserEntity saved = userService.createUser(user);
        if(saved==null)
        {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Username already exists"));
        }
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new CreateUserResponse(saved.getId()));
    }

    @PostMapping("/create")
    public ResponseEntity<?> createUserByAdmin(@RequestBody CreateUserRequestAdmin user) {

        UserEntity saved = userService.createUserAdmin(user);
        if(saved==null)
        {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Username already exists"));
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new CreateUserResponse(saved.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserEntity> updateUser(@PathVariable Long id, @RequestBody UserEntity user) {
        return ResponseEntity.ok(userService.updateUser(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
