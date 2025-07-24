package damnosol.triggeriq.sentiment.storage;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class MinioStorageService {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    @Value("${minio.bucket}")
    private String bucketName;

    private S3Client s3;

    @PostConstruct
    public void init() {
        s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1) // required, even for MinIO
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .forcePathStyle(true) // required for MinIO
                .build();

        // Ensure bucket exists
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder().bucket(bucketName).build();
            s3.headBucket(headBucketRequest);
        } catch (NoSuchBucketException e) {
            log.info("Bucket '{}' does not exist. Creating it...", bucketName);
            s3.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        }
    }

    public void uploadJson(String key, String json) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("application/json")
                .build();

        s3.putObject(request, RequestBody.fromBytes(json.getBytes(StandardCharsets.UTF_8)));

        log.info("✅ Uploaded to MinIO: {}/{}", bucketName, key);
    }

    public String getObjectUrl(String key) {
        return String.format("%s/%s/%s", endpoint, bucketName, key);
    }
}