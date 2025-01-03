package at.wielander.elevator.Exception;

public class MQTTClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MQTTClientException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
