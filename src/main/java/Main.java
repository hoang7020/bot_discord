import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import model.Question;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.util.List;

public class Main extends ListenerAdapter {

    public static final String QUEUE_NAME = "CONFETTI";

    public static void main(String[] args) throws LoginException {
        JDABuilder builder = new JDABuilder(AccountType.BOT);
        String token = "NTY3MTczODA3NDMzODQyNjk4.XLPyFQ.2K0aEUgLRURN9wTEN_r-5pcs80M";
        builder.setToken(token);
        builder.addEventListeners(new Main());
        JDA jda = builder.build();
        try {
            jda.awaitReady();
            rabbitmq(jda);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static void sendMessage(TextChannel ch, Question question) {
        String[] quesArr = (question.getQuestionText() + " "
                + question.getOptionA() + " "
                + question.getOptionB() + " "
                + question.getOptionC())
                .split("[()? ]");
        StringBuilder stringBuilder = new StringBuilder();
        int i = 1;
        for (String s: quesArr) {
            stringBuilder.append(s);
            if (i < quesArr.length) {
                stringBuilder.append("%20");
                i++;
            }
        }
        EmbedBuilder builder = new EmbedBuilder();
        builder.setColor(Color.red);
        builder.setTitle("Question: " + question.getQuestionText());
        builder.setDescription(
                    "A. " + question.getOptionA()
                + "\nB. " + question.getOptionB()
                + "\nC. " + question.getOptionC()
                + "\n[Google Search](https://www.google.com./search?q=" + stringBuilder.toString() + ")"
                + "\n[Bing Search](https://www.bing.com/search?q=" + stringBuilder.toString() + ")"
                );
        ch.sendMessage(builder.build()).queue();
    }

    public static String message = "";

    public static void rabbitmq(JDA jda) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setRequestedHeartbeat(30);
        factory.setConnectionTimeout(30000);
        try {
            factory.setUri("amqp://qjoahoff:Qxm4reMreDbVPRyDlie5PfuQEOe_SWR2@dinosaur.rmq.cloudamqp.com/qjoahoff");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                if (!message.equals(new String(delivery.getBody(), "UTF-8"))) {
                    message = new String(delivery.getBody(), "UTF-8");
                    System.out.println(" [x] Received '" + message + "'");
                    List<TextChannel> channels = jda.getTextChannelsByName("general", true);
                    Gson gson = new Gson();
                    Question question = gson.fromJson(message, Question.class);
                    for (TextChannel ch : channels) {
                        sendMessage(ch, question);
                    }
                }
            };
            channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
