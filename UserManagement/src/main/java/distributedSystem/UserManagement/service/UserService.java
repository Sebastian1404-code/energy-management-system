package distributedSystem.UserManagement.service;

import distributedSystem.UserManagement.events.UserEventsProducer;
import distributedSystem.UserManagement.model.UserEntity;
import distributedSystem.UserManagement.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserEventsProducer producer;

    public UserService(UserRepository userRepository, UserEventsProducer producer) {
        this.userRepository = userRepository;
        this.producer = producer;
    }

    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<UserEntity> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public UserEntity createUser(UserEntity user) {
        user.setId(null);
        UserEntity saved = userRepository.save(user);

        producer.publishUserCreatedEvent(saved.getId());

        return saved;
    }

    public UserEntity updateUser(Long id, UserEntity updatedUser) {
        return userRepository.findById(id)
                .map(existing -> {
                    existing.setUsername(updatedUser.getUsername());
                    existing.setRole(updatedUser.getRole());
                    return userRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
        producer.publishUserDeletedEvent(id);
    }

    public Optional<Long> getUserIdByUsername(String username) {
        System.out.println(username);
        return userRepository.findByUsername(username)
                .map(UserEntity::getId);
    }


}
