package com.example.smsservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
/*
class PaymentServiceMockTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Инициализация моков
    }

    @Test
    void testAddBalance() {
        // Arrange: создаём пользователя с балансом
        User user = new User();
        user.setUsername("testUser");
        user.setBalance(50.0);

        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));

        // Act: вызываем метод пополнения баланса
        paymentService.addBalance("testUser", 25.0);

        // Assert: проверяем, что баланс обновлён
        assertEquals(75.0, user.getBalance());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void testDeductBalance() {
        // Arrange: создаём пользователя с балансом
        User user = new User();
        user.setUsername("testUser");
        user.setBalance(50.0);

        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));

        // Act: вызываем метод списания баланса
        paymentService.deductBalance("testUser", 20.0);

        // Assert: проверяем, что баланс обновлён
        assertEquals(30.0, user.getBalance());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void testDeductBalance_InsufficientFunds() {
        // Arrange: создаём пользователя с недостаточным балансом
        User user = new User();
        user.setUsername("testUser");
        user.setBalance(10.0);

        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));

        // Act & Assert: проверяем, что возникает исключение при недостатке средств
        try {
            paymentService.deductBalance("testUser", 20.0);
        } catch (RuntimeException e) {
            assertEquals("Insufficient balance", e.getMessage());
        }

        verify(userRepository, never()).save(user); // Убедимся, что метод save() не вызывается
    }
}
*/