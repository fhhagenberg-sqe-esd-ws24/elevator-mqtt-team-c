package at.wielander.elevator.Exception;

import at.wielander.elevator.exception.MQTTClientException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MQTTClientExceptionTest {

    @Test
    void testMQTTClientException() {
        Throwable throwable = new IllegalArgumentException("Invalid argument");
        MQTTClientException exception = new MQTTClientException("adapter Client Exception Thrown", throwable);

        assertEquals("adapter Client Exception Thrown", exception.getMessage());
        assertEquals(throwable, exception.getCause());
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertEquals("Invalid argument", exception.getCause().getMessage());
    }

}