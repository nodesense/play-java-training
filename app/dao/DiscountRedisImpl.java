package dao;

import com.fasterxml.jackson.databind.JsonNode;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.protocol.RedisCommand;
import models.Discount;
import io.lettuce.core.*;
import io.lettuce.core.api.sync.*;
import io.lettuce.core.api.*;
import models.Email;
import play.inject.ApplicationLifecycle;
import play.libs.Json;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.typesafe.config.Config;
public class DiscountRedisImpl implements  DiscountDao {
    RedisClient redisClient;
    StatefulRedisConnection<String, String> connection;
    Config config;

    @Inject
    public DiscountRedisImpl(ApplicationLifecycle lifecycle, Config config) {
        this.config = config;
        // redisClient = RedisClient.create("redis://password@endpoint/");

        String redisUrl = config.getString("redis.url");
        if (redisUrl == null ||  redisUrl.isEmpty()) {
            redisUrl = "redis://localhost";
        }
        System.out.println("**Redis Url " + redisUrl);

        // redisClient = RedisClient.create("redis://v8iWDhQEfrTtU2UTC11mZHrOJOaQn2jr@redis-18350.c1.ap-southeast-1-1.ec2.cloud.redislabs.com:18350/");
        redisClient = RedisClient.create(redisUrl);

        connection = redisClient.connect();


        lifecycle.addStopHook( () -> {
            System.out.println("***redis db cleanup");
            // redisClient.shutdown();
            return CompletableFuture.completedFuture(null);
        });
    }


    @Override
    public void setDiscountSync(Discount discount) {
        RedisCommands<String, String> sync = connection.sync();
        // Blocking call
        // bad
        // send request to redis
        // send paylaod
        // once redis return the response, it come out until it is blocked
        System.out.println("Discoutn " + discount.getProductId());
        String jsonText = Json.toJson(discount).toString();
        System.out.println("Value is " + jsonText);
        sync.set(discount.getProductId(), jsonText);
    }

    @Override
    public Discount getDiscountSync(String productId) {
        RedisCommands<String, String> sync = connection.sync();
        // Blocking call
        // bad
        // send request to redis
        // send command
        // once redis return the response, it will not come out until it is blocked
        System.out.println("product id " + productId);
        String jsonText = sync.get(productId);
        System.out.println("Json discount " + jsonText);

        JsonNode node = Json.parse(jsonText);

        Discount discount = Json.fromJson(node, Discount.class); // JSON to POJO
        return discount;
    }

    @Override
    public void setDiscount(Discount discount) {

    }

    @Override
    public CompletionStage<Discount> getDiscount(String productId) {
        RedisAsyncCommands<String, String> asyncCommands = connection.async();
        RedisFuture<String> result = asyncCommands.get(productId);
        // result.get(); // bad, blocking call
        return result.thenCompose( jsonText -> {
                    return CompletableFuture.supplyAsync( () -> {
                        JsonNode node = Json.parse(jsonText);
                        Discount discount = Json.fromJson(node, Discount.class); // JSON to POJO
                        return discount;
                    });
                });
    }
}
