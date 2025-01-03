pid_file /run/mosquitto/mosquitto.pid

persistence true
persistence_location /var/lib/mosquitto/

log_dest file /var/log/mosquitto/mosquitto.log

listener 1883 0.0.0.0
allow_anonymous false
password_file /etc/mosquitto/passwd

include_dir /etc/mosquitto/conf.d