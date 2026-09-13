package com.ecom.infra.client.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Map;

/**
 * The S3 buckets this profile knows, by name, under {@code ecom.client.s3.<name>}.
 *
 * @param s3 the buckets, keyed by name (for example {@code assets})
 */
@ConfigurationProperties(prefix = "ecom.client")
public record S3ClientProperties(@DefaultValue Map<String, Bucket> s3) {

   /**
    * One bucket. Credentials come from the AWS default chain (environment variables {@code AWS_ACCESS_KEY_ID} and
    * {@code AWS_SECRET_ACCESS_KEY}, a shared profile, or an IAM role). The static keys exist only for local
    * containers that insist on some key; do not use them for a real AWS account.
    *
    * @param bucket          the bucket name
    * @param region          AWS region, for example {@code ap-south-1}
    * @param endpoint        optional custom endpoint, for example {@code http://localhost:9090} for a local S3Mock
    * @param pathStyle       whether to address the bucket as part of the path (needed by local stand-ins), false for AWS
    * @param publicBaseUrl   optional address files are served from, for example a CDN; when not set it is derived
    *                        from the bucket and region (or the endpoint)
    * @param accessKeyId     optional static key, local use only
    * @param secretAccessKey optional static secret, local use only
    */
   public record Bucket(String bucket, String region, String endpoint, Boolean pathStyle, String publicBaseUrl,
                        String accessKeyId, String secretAccessKey) {
   }
}
