package com.dtvi.Pet.Ecommerce.service.interf;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.dtvi.Pet.Ecommerce.exception.BusinessException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.io.ByteArrayInputStream;


@Service
@Slf4j
public class AwsS3Service {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    @Value("${aws.s3.access}")
    private String awsS3AccessKey;

    @Value("${aws.s3.secret}")
    private String awsS3SecretKey;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    private record DetectedImage(
            String contentType,
            String extension
    ) {
    }

    public String saveImageToS3(MultipartFile photo) {
        if (photo == null
                || photo.isEmpty()
                || !ALLOWED_CONTENT_TYPES.contains(photo.getContentType())) {
                throw BusinessException.badRequest(
                        "INVALID_IMAGE",
                        "Only non-empty JPEG, PNG or WebP images are accepted"
                );
        }

        try {
                byte[] bytes = photo.getBytes();
                DetectedImage image = detectImage(bytes);
                String fileName =
                        "images/" + UUID.randomUUID() + image.extension();

                BasicAWSCredentials credentials =
                        new BasicAWSCredentials(
                                awsS3AccessKey,
                                awsS3SecretKey
                        );

                AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                        .withCredentials(
                                new AWSStaticCredentialsProvider(credentials)
                        )
                        .withRegion(region)
                        .build();

                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType(image.contentType());
                metadata.setContentLength(bytes.length);

                try (InputStream input =
                        new ByteArrayInputStream(bytes)) {
                s3Client.putObject(
                        new PutObjectRequest(
                                bucketName,
                                fileName,
                                input,
                                metadata
                        )
                );
                }

                return "https://" + bucketName
                        + ".s3." + region
                        + ".amazonaws.com/" + fileName;

        } catch (IOException exception) {
                log.error("Unable to read product image", exception);
                throw new IllegalStateException(
                        "Unable to upload product image",
                        exception
                );
        }
    }

    private DetectedImage detectImage(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff) {
            return new DetectedImage(
                    "image/jpeg",
                    ".jpg"
            );
        }

        if (bytes.length >= 8
                && (bytes[0] & 0xff) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4e
                && bytes[3] == 0x47
                && bytes[4] == 0x0d
                && bytes[5] == 0x0a
                && bytes[6] == 0x1a
                && bytes[7] == 0x0a) {
            return new DetectedImage(
                    "image/png",
                    ".png"
            );
        }

        if (bytes.length >= 12
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P') {
            return new DetectedImage(
                    "image/webp",
                    ".webp"
            );
        }

        throw BusinessException.badRequest(
                "INVALID_IMAGE_CONTENT",
                "Uploaded file is not a valid JPEG, PNG or WebP image"
        );
    }
}
