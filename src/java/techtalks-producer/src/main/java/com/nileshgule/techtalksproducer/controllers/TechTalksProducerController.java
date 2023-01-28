package com.nileshgule.techtalksproducer.controllers;

import com.github.javafaker.Faker;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.Metadata;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

import java.util.stream.IntStream;

import static java.util.Collections.singletonMap;


@RestController
@RequestMapping("/api/TechTalks/")
public class TechTalksProducerController {
    private static final Logger log = LoggerFactory.getLogger(TechTalksProducerController.class);

    @Value("${RABBITMQ_TOPIC}")
    private String topicName;

    @Value("${PUBSUB_NAME}")
    private String pubSubName;


    //Time-to-live for messages published.
    private static final String MESSAGE_TTL_IN_SECONDS = "1000";

    @GetMapping (name = "/generate")
//    @RequestMapping(value = "/generate", method = RequestMethod.GET)
    public ResponseEntity<String> produceMessages(@RequestParam(value = "numberOfMessages", defaultValue = "1") int numberOfMessages) {
        log.info("Publishing messages to topic: " + topicName);

        Faker faker = new Faker();

        try (DaprClient client = new DaprClientBuilder().build()){
            IntStream.range(0, numberOfMessages)
                    .forEach(i -> {
                        int techTalkId = faker.number().numberBetween(1, 1000);
                        String techTalkName = faker.lorem().sentence();
                        int categoryId = faker.random().nextInt(1, 5);
                        int levelId = faker.random().nextInt(1, 4);

                        TechTalk techTalk = new TechTalk(techTalkId, techTalkName, categoryId, levelId);

                        log.info("Publishing message: " + techTalk + "on queue :" + pubSubName + " with topic: " + topicName);
                        client.publishEvent("rabbitmq-pubsub", "techtalks", techTalk, singletonMap(Metadata.TTL_IN_SECONDS, MESSAGE_TTL_IN_SECONDS)).block();
            });
        } catch (Exception e) {
            log.error("Error while publishing messages: " + e.getMessage());
            return ResponseEntity.badRequest().body("Error while publishing messages: " + e.getMessage());
        }

        return ResponseEntity.ok().body("Successfully produced " + numberOfMessages + " messages to RabbitMQ");
    }
}



@AllArgsConstructor
@Getter
class TechTalk {
    private int Id;
    private String techTalkName;
    private int categoryId;
    private int levelId;
}
