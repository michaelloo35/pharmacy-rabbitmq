package users;

import com.rabbitmq.client.*;
import system.ConnectionEstablisher;
import utils.Color;
import utils.Printer;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static system.Constants.*;


public class Admin {

    private static final Color COLOR = Color.YELLOW;
    private final Channel channel;
    private final String name;
    private final Printer printer;

    public Admin(String name) throws IOException, TimeoutException {
        this.name = name;


        this.channel = new ConnectionEstablisher().establish(HOST, EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
        this.startListening();

        this.printer = new Printer();
        this.printer.printColored(COLOR, "Admin " + name + " woke up!");
    }

    //
    public void broadcast(String message) throws IOException {

        // * (star) can substitute for exactly one word.
        // # (hash) can substitute for zero or more words.
        String routingKey = BROADCAST_ROUTING_KEY;

        String broadcast = String.format("%s;%s;%s;%s", "broadcast", "admin", name, message);

        channel.basicPublish(EXCHANGE_NAME, routingKey, null, broadcast.getBytes("UTF-8"));
        printer.printColored(COLOR, name + " broadcasted: " + message);
    }

    /**
     * Response Listener
     */
    private void startListening() throws IOException {

        // queue & bind every admin has its own que
        String queueName = channel.queueDeclare().getQueue();

        // * (star) can substitute for exactly one word.
        // # (hash) can substitute for zero or more words.
        String routingKey = GLOBAL_ROUTING_KEY + ".#";

        channel.queueBind(queueName, EXCHANGE_NAME, routingKey);

        // consumer (message handling)
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                printer.printColored(COLOR, name + " logged activity: " + message);
            }
        };

        // start listening
        channel.basicConsume(queueName, true, consumer);
    }


}
