package at.wielander.elevator.Exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MQTTClientExceptionTest {

    @Test
    public void testMQTTClientException() {
        Throwable throwable = new IllegalArgumentException("Invalid argument");
        MQTTClientException exception = new MQTTClientException("MQTT Client Exception Thrown", throwable);

        assertEquals("MQTT Client Exception Thrown", exception.getMessage());
        assertEquals(throwable, exception.getCause());
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertEquals("Invalid argument", exception.getCause().getMessage());
    }

}