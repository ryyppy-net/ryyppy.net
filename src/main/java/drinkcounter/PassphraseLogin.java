package drinkcounter;

import drinkcounter.model.User;
import drinkcounter.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PassphraseLogin {

    private final UserRepository userRepository;
    private final UserAccounts userAccounts;

    public PassphraseLogin(UserRepository userRepository, UserAccounts userAccounts) {
        this.userRepository = userRepository;
        this.userAccounts = userAccounts;
    }

    public void issue(User user) {
        String passphrase = UUID.randomUUID().toString();
        user.setPassphrase(passphrase);
        userAccounts.update(user);
    }

    public User findUser(String passphrase) {
        return userRepository.findByPassphrase(passphrase);
    }
}
