package distributedSystem.UserManagement.service;

import distributedSystem.UserManagement.dto.CreateUserRequest;
import distributedSystem.UserManagement.dto.CreateUserRequestAdmin;
import distributedSystem.UserManagement.dto.CredentialRequest;
import distributedSystem.UserManagement.events.UserEventsProducer;
import distributedSystem.UserManagement.model.UserEntity;
import distributedSystem.UserManagement.repository.UserRepository;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserEventsProducer producer;
    private final WebClient authWebClient;


    public UserService(UserRepository userRepository, UserEventsProducer producer, WebClient authWebClient) {
        this.userRepository = userRepository;
        this.producer = producer;
        this.authWebClient = authWebClient;
    }

    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<UserEntity> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public UserEntity createUser(CreateUserRequest user) {
        UserEntity userEntity = UserEntity.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
        userEntity.setId(null);

        if(userRepository.findByUsername(userEntity.getUsername()).isPresent())
        {
            return null;
        }

        UserEntity saved = userRepository.save(userEntity);

        producer.publishUserCreatedEvent(saved.getId());

        return saved;
    }

    public UserEntity createUserAdmin(CreateUserRequestAdmin user) {
        UserEntity userEntity = UserEntity.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
        userEntity.setId(null);

        if(userRepository.findByUsername(userEntity.getUsername()).isPresent())
        {
            return null;
        }


        UserEntity saved = userRepository.save(userEntity);

        CredentialRequest credReq = new CredentialRequest(
                saved.getId(), user.getUsername(), user.getPassword(), user.getRole());

        try {
            authWebClient.post()
                    .uri("/auth/credential")
                    .bodyValue(credReq)
                    .retrieve()
                    .onStatus(s -> s.value() == 409, resp ->
                            Mono.error(new IllegalStateException("Credentials already exist")))
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            userRepository.deleteById(saved.getId());
            throw new RuntimeException("Failed to create credentials in Authorization service", e);
        }



        producer.publishUserCreatedEvent(saved.getId());

        return saved;
    }

    public UserEntity updateUser(Long id, UserEntity updatedUser) {
        return userRepository.findById(id)
                .map(existing -> {
                    existing.setUsername(updatedUser.getUsername());
                    existing.setRole(updatedUser.getRole());
                    existing.setEmail(updatedUser.getEmail());
                    return userRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void deleteUser(Long id) {

        System.out.println("before");
        authWebClient
                .delete()
                .uri(uriBuilder -> uriBuilder.path("/auth/delete/{userId}").build(id))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, resp ->
                        // swallow 404 (no creds to delete); bubble up other 4xx
                        resp.statusCode().value() == 404 ? Mono.empty()
                                : resp.createException().flatMap(Mono::error))
                .toBodilessEntity()
                .block();
        System.out.println("after");
        userRepository.deleteById(id);

        producer.publishUserDeletedEvent(id);
    }

    public Optional<Long> getUserIdByUsername(String username) {
        System.out.println(username);
        return userRepository.findByUsername(username)
                .map(UserEntity::getId);
    }


}
