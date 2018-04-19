package users;

import com.rabbitmq.client.*;
import system.ConnectionEstablisher;
import utils.Color;
import utils.Printer;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static system.Constants.*;


public class Doctor {

    private static final Color COLOR = Color.BLUE;
    private static final String ERROR_MESSAGE = "ERROR received Malformed message";
    private final String name;
    private final Channel channel;
    private final Printer printer;

    public Doctor(String name) throws IOException, TimeoutException {
        this.name = name;

        this.channel = new ConnectionEstablisher().establish(HOST, EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

        this.startListeningForExaminationResults();

        this.printer = new Printer();
        this.printer.printColored(COLOR, "Doctor " + name + " woke up!");
    }

    /**
     * Examination type has to match technician specialization realized through TOPIC mechanism
     * Request format : "request;doctorName;examinationType;patientName"
     */
    public void postExaminationRequest(final String examinationType, final String patientName) throws IOException {

        String examinationRequest = String.format("%s;%s;%s;%s", "request", name, examinationType, patientName);
        String routingKey = String.format("%s.technician.%s", GLOBAL_ROUTING_KEY, examinationType);

        channel.basicPublish(EXCHANGE_NAME, routingKey, null, examinationRequest.getBytes("UTF-8"));
        printer.printColored(COLOR, name + " sent request: " + examinationRequest);
    }

    /**
     * Response Listener
     */
    private void startListeningForExaminationResults() throws IOException {

        // queue & bind every doctor has its own que
        String personalQueueName = channel.queueDeclare().getQueue();

        String resultsRoutingKey = String.format("%s.doctor.%s", GLOBAL_ROUTING_KEY, name);

        channel.queueBind(personalQueueName, EXCHANGE_NAME, resultsRoutingKey);
        channel.queueBind(personalQueueName, EXCHANGE_NAME, BROADCAST_ROUTING_KEY);

        // consumer (message handling)
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");

                // parse message
                String[] split = message.split(";");
                verifyMessage(split);

                switch (split[0]) {
                    case "result":
                        printer.printColored(COLOR, name + " received results: " + message);
                        break;

                    case "broadcast":
                        String adminRole = split[1];
                        String adminName = split[2];
                        String broadcastMsg = split[3];
                        printer.printColored(COLOR, name + " received " + adminRole + " " + adminName + " broadcast: \n" + broadcastMsg);
                        break;

                    default:
                        printer.printColored(COLOR, name + " " + ERROR_MESSAGE);
                }

            }
        };

        // start listening
        channel.basicConsume(personalQueueName, true, consumer);
    }

    private void verifyMessage(String[] split) {
        if (split.length != 4) {
            printer.printColored(COLOR, name + " " + ERROR_MESSAGE);
        }
    }

}
