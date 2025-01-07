package at.wielander.elevator.Exception;

/**
 * Exception for Elevator Algorithmm
 */
public class MQTTClientException extends RuntimeException {

    /**
     * Throws exception for elevator algorithm
     * @param message Exception message
     * @param throwable throwable
     */
    public MQTTClientException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
