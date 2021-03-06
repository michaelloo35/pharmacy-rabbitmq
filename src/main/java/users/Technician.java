package users;

import com.rabbitmq.client.*;
import system.ConnectionEstablisher;
import utils.Color;
import utils.Printer;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import static system.Constants.*;

public class Technician {

    private static final Color COLOR = Color.RED;
    private static final String ERROR_MESSAGE = "ERROR received Malformed message";
    private final String name;
    private final String specialization1;
    private final String specialization2;
    private final Channel channel;
    private final Printer printer;

    public Technician(String name, String specialization1, String specialization2) throws IOException, TimeoutException {
        this.name = name;

        this.specialization1 = specialization1;
        this.specialization2 = specialization2;

        this.channel = new ConnectionEstablisher().establish(HOST, EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

        this.startListeningForExaminationRequests();

        printer = new Printer();
        printer.printColored(COLOR, "Technician " + name + " with specialization " + specialization1 + "," + specialization2 + " woke up!");
    }


    /**
     * Response Listener
     */
    private void startListeningForExaminationRequests() throws IOException {

        // queue & bind technicians share queues
        channel.queueDeclare(specialization1, false, false, false, null);
        channel.queueDeclare(specialization2, false, false, false, null);
        String personalQueueName = channel.queueDeclare().getQueue();

        String specialization1RoutingKey = GLOBAL_ROUTING_KEY + ".technician" + "." + specialization1;
        String specialization2RoutingKey = GLOBAL_ROUTING_KEY + ".technician" + "." + specialization2;

        channel.queueBind(specialization1, EXCHANGE_NAME, specialization1RoutingKey);
        channel.queueBind(specialization2, EXCHANGE_NAME, specialization2RoutingKey);
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
                    case "request":
                        String doctorName = split[1];
                        String examinationType = split[2];
                        String patientName = split[3];

                        String result = doExamination(examinationType, patientName);
                        postExaminationResults(result, doctorName);
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
        channel.basicQos(1);
        channel.basicConsume(personalQueueName, true, consumer);
        channel.basicConsume(specialization1, true, consumer);
        channel.basicConsume(specialization2, true, consumer);
    }

    private void verifyMessage(String[] split) {
        if (split.length != 4) {
            printer.printColored(COLOR, name + " " + ERROR_MESSAGE);
        }
    }

    private String doExamination(String examinationType, String patientName) {
        try {
            long examinationTime = (long) (new Random().nextDouble() * 10_000);
            Thread.sleep(examinationTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return String.format("%s;%s;%s;%d%%", "result", examinationType, patientName, new Random().nextInt(101));
    }


    /**
     * Result Format "result";examinationType;patientName;examinationResult
     */
    private void postExaminationResults(String examinationResult, String doctorName) throws IOException {


        String routingKey = String.format("%s.doctor.%s", GLOBAL_ROUTING_KEY, doctorName);

        channel.basicPublish(EXCHANGE_NAME, routingKey, null, examinationResult.getBytes("UTF-8"));
        printer.printColored(COLOR, name + " sent result: " + examinationResult + " to Doctor: " + doctorName);
    }


}
