package com.epam.edp.demo.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String awsRegion;

    @Value("${aws.accessKeyId}")
    private String accessKey;

    @Value("${aws.secretKey}")
    private String secretKey;

    @Value("${aws.sessionToken}")
    private String sessionToken;

    @Value("${aws.role.arn}")
    private String roleArn;

    @Bean
    public DynamoDBMapper dynamoDBMapper() {
        return new DynamoDBMapper(amazonDynamoDB());
    }

    @Bean
    public AmazonDynamoDB amazonDynamoDB() {
        return buildAmazonDynamoDB();
    }

    private AmazonDynamoDB buildAmazonDynamoDB() {
        BasicSessionCredentials awsCredentials = new BasicSessionCredentials(
                accessKey,
                secretKey,
                sessionToken
        );

        // Create STS client with these credentials
        AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard()
                .withRegion("eu-north-1")
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();

        // Create the credentials provider to assume the role
        STSAssumeRoleSessionCredentialsProvider credentialsProvider =
                new STSAssumeRoleSessionCredentialsProvider.Builder(roleArn, "assume-role-session-name")
                        .withStsClient(stsClient)
                        .build();

        // Create the DynamoDB client with the assumed role credentials
        return AmazonDynamoDBClientBuilder.standard()
                .withRegion("eu-north-1")
                .withCredentials(credentialsProvider)
                .build();
    }

    @Bean
    public AmazonSimpleEmailService amazonSimpleEmailService() {
        BasicSessionCredentials awsCredentials = new BasicSessionCredentials(
                accessKey,
                secretKey,
                sessionToken
        );

        // Create STS client with these credentials
        AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard()
                .withRegion("eu-north-1")
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();

        STSAssumeRoleSessionCredentialsProvider credentialsProvider =
                new STSAssumeRoleSessionCredentialsProvider.Builder(roleArn, "assume-role-session-name")
                        .withStsClient(stsClient)
                        .build();

        return AmazonSimpleEmailServiceClientBuilder.standard()
                .withRegion("eu-north-1")
                .withCredentials(credentialsProvider)
                .build();
    }

}