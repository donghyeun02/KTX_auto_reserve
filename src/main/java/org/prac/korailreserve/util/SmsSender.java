package org.prac.korailreserve.util;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SmsSender {
    private final AmazonSNSClient snsClient;

    public SmsSender(@Value("${aws.access.key.id}") String accessKeyId,
                     @Value("${aws.secret.access.key}") String secretAccessKey) {
        this.snsClient = (AmazonSNSClient) AmazonSNSClientBuilder.standard()
                .withRegion(Regions.AP_NORTHEAST_1.getName())
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKeyId, secretAccessKey)))
                .build();
    }


    public void sendSms(String phoneNumber, String message) {
        // SNS를 통해 SMS 메시지 전송
        PublishRequest request = new PublishRequest()
                .withMessage(message)
                .withPhoneNumber(phoneNumber);

        PublishResult result = snsClient.publish(request);

        // 메시지가 성공적으로 전송되었는지 확인
        System.out.println("Message ID: " + result.getMessageId() + " sent to " + phoneNumber);
    }

    @PreDestroy
    public void destroy() {
        if (snsClient != null) {
            snsClient.shutdown();
        }
    }
}
