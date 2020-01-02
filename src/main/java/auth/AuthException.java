package auth;

/**
 * Свой класс исключений при авторизации
 */

public class AuthException extends RuntimeException {
        public AuthException(String message, Throwable cause) {
            super(message, cause);
        }
    }

