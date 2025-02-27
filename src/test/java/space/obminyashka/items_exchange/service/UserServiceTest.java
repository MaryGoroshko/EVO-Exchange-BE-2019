package space.obminyashka.items_exchange.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import space.obminyashka.items_exchange.dao.UserRepository;
import space.obminyashka.items_exchange.dto.UserChangeEmailDto;
import space.obminyashka.items_exchange.dto.UserChangePasswordDto;
import space.obminyashka.items_exchange.model.User;
import space.obminyashka.items_exchange.model.enums.Status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static space.obminyashka.items_exchange.model.enums.Status.ACTIVE;
import static space.obminyashka.items_exchange.model.enums.Status.DELETED;
import static space.obminyashka.items_exchange.util.MessageSourceUtil.getMessageSource;

@SpringBootTest
class UserServiceTest {

    public static final String CORRECT_OLD_PASSWORD = "123456xX";
    public static final String NEW_PASSWORD = "123456wW";
    public static final String NEW_USER_EMAIL = "user@mail.ru";

    @MockBean
    private UserRepository userRepository;
    @Captor
    private ArgumentCaptor<User> userArgumentCaptor;
    @Autowired
    private UserService userService;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Value("${number.of.days.to.keep.deleted.users}")
    private int numberOfDaysToKeepDeletedUsers;
    private User userWithOldPassword;

    @BeforeEach
    void setUp() {
        userWithOldPassword = createUserWithOldPassword();
    }

    @Test
    void testUpdateUserPassword_WhenDataCorrect_Successfully() {
        UserChangePasswordDto userChangePasswordDto = new UserChangePasswordDto(CORRECT_OLD_PASSWORD, NEW_PASSWORD, NEW_PASSWORD);
        String message = userService.updateUserPassword(userChangePasswordDto, userWithOldPassword);

        assertEquals(getMessageSource("changed.user.password"), message);
        assertTrue(bCryptPasswordEncoder.matches(NEW_PASSWORD, userWithOldPassword.getPassword()));
        verify(userRepository).saveAndFlush(userWithOldPassword);
    }

    @Test
    void testUpdateUserEmail_WhenDataCorrect_Successfully() {
        UserChangeEmailDto userChangeEmailDto = new UserChangeEmailDto(NEW_USER_EMAIL, NEW_USER_EMAIL);
        String message = userService.updateUserEmail(userChangeEmailDto, userWithOldPassword);

        assertEquals(getMessageSource("changed.user.email"), message);
        assertEquals(NEW_USER_EMAIL, userWithOldPassword.getEmail());
        verify(userRepository).saveAndFlush(userWithOldPassword);
    }

    @Test
    void testSelfDeleteRequest_WhenDataCorrect_Successfully() {
        userService.selfDeleteRequest(userWithOldPassword);

        assertEquals(DELETED, userWithOldPassword.getStatus());
        verify(userRepository).saveAndFlush(userWithOldPassword);
    }

    @Test
    void testPermanentlyDeleteUsers_ShouldDeleteRequiredUsers() {
        List<User> users = createTestUsers();
        assertEquals(4, users.size());

        when(userRepository.findAll()).thenReturn(users);
        userService.permanentlyDeleteUsers();

        verify(userRepository).delete(users.get(0));

        for (int i = 1; i < users.size(); i++) {
            verify(userRepository, never()).delete(users.get(i));
        }
    }

    @Test
    void makeAccountActiveAgain_WhenDataCorrect_Successfully() {
        userService.makeAccountActiveAgain(userWithOldPassword);

        assertEquals(ACTIVE, userWithOldPassword.getStatus());
        verify(userRepository).saveAndFlush(userWithOldPassword);
    }

    private User createUserWithOldPassword() {
        userWithOldPassword = new User();
        userWithOldPassword.setPassword(bCryptPasswordEncoder.encode(CORRECT_OLD_PASSWORD));
        userWithOldPassword.setUpdated(LocalDateTime.now());

        return userWithOldPassword;
    }

    private List<User> createTestUsers() {
        User shouldBeDeleted = createUserForDeleting(DELETED, numberOfDaysToKeepDeletedUsers + 1);
        User shouldNotBeDeleted0 = createUserForDeleting(ACTIVE, 0);
        User shouldNotBeDeleted1 = createUserForDeleting(DELETED, numberOfDaysToKeepDeletedUsers - 1);
        User shouldNotBeDeleted2 = createUserForDeleting(ACTIVE, numberOfDaysToKeepDeletedUsers + 1);

        return List.of(shouldBeDeleted, shouldNotBeDeleted0, shouldNotBeDeleted1, shouldNotBeDeleted2);
    }

    private User createUserForDeleting(Status status, int delay) {
        User user = new User();
        user.setStatus(status);
        user.setUpdated(LocalDateTime.now().minusDays(delay));

        return user;
    }

    @ParameterizedTest
    @MethodSource("getTestLocales")
    void updatePreferableLanguage_shouldSetLanguageAccordingContext(Locale expectedLocale) {
        LocaleContextHolder.setLocale(expectedLocale);
        final var user = new User();
        user.setLanguage(Locale.FRANCE);
        when(userRepository.findByRefreshToken_Token(anyString())).thenReturn(Optional.of(user));

        userService.updatePreferableLanguage("mocked token");
        verify(userRepository).saveAndFlush(userArgumentCaptor.capture());
        assertEquals(expectedLocale, userArgumentCaptor.getValue().getLanguage());
    }

    private static List<Locale> getTestLocales() {
        return List.of(Locale.ENGLISH, new Locale("ua"), new Locale("ru"));
    }
}
