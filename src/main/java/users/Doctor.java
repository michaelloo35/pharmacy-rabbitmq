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
     * Request format : "doctorName;examinationType;patientName"
     */
    public void postExaminationRequest(final String examinationType, final String patientName) throws IOException {

        String examinationRequest = String.format("%s;%s;%s", name, examinationType, patientName);
        String routingKey = String.format("%s.technician.%s", GLOBAL_ROUTING_KEY, examinationType);

        channel.basicPublish(EXCHANGE_NAME, routingKey, null, examinationRequest.getBytes("UTF-8"));
        printer.printColored(COLOR, name + " sent request: " + examinationRequest);
    }

    /**
     * Response Listener
     */
    private void startListeningForExaminationResults() throws IOException {

        // queue & bind every doctor has its own que
        String queueName = channel.queueDeclare().getQueue();

        String routingKey = String.format("%s.doctor.%s", GLOBAL_ROUTING_KEY, name);
        channel.queueBind(queueName, EXCHANGE_NAME, routingKey);

        // consumer (message handling)
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                printer.printColored(COLOR, name + " received results: " + message);
            }
        };

        // start listening
        channel.basicConsume(queueName, true, consumer);
    }

}
