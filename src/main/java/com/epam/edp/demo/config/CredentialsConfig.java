package com.epam.edp.demo.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;

@Configuration
public class CredentialsConfig {

    @Value("${aws.access.key.id}")
    private String accessKey;

    @Value("${aws.secret.access.key}")
    private String secretKey;

    @Value("${aws.session.token}")
    private String sessiontToken;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.role.arn}")
    private String  roleArn;

    @Bean("userCredentialsProvider")
    @Primary
    public AwsCredentialsProvider credentialsProvider(){
        return software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                software.amazon.awssdk.auth.credentials.AwsSessionCredentials.create(
                        accessKey,
                        secretKey,
                        sessiontToken
                )
        );
    }

    @Bean("assumeRoleCredentialsProvider")
    public AwsCredentialsProvider assumeRoleCredentialsProvider(@Qualifier("userCredentialsProvider") AwsCredentialsProvider awsCredentialsProvider){
        StsClient stsClient = StsClient.builder()
                .region(software.amazon.awssdk.regions.Region.of(region))
                .credentialsProvider(awsCredentialsProvider)
                .build();

        return StsAssumeRoleCredentialsProvider.builder()
                .stsClient(stsClient)
                .refreshRequest(builder -> builder.roleArn(roleArn).roleSessionName("assume-role-session"))
                .build();
    }

}
