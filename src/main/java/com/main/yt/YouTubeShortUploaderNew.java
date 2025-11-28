/*
package com.main.yt;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import com.google.api.services.drive.model.File;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

public class YouTubeShortUploaderNew {
    private static final String APPLICATION_NAME = "AviTech";
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    public static void main(String[] args) throws Exception {
        // Load from env (GitHub Secrets mapped to env vars in GH Actions)
          // Drive "posted" folder

        if (clientId == null || clientSecret == null || refreshToken == null || contentFolderId == null || postedFolderId == null) {
            System.err.println("Missing required env vars.");
            System.exit(1);
        }

        // 1) Build credential from refresh token
        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setRefreshToken(refreshToken);
        credential.refreshToken();

        // 2) Build Drive client
        Drive drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        // 3) List all video files in content folder
        String query = "'" + contentFolderId + "' in parents and mimeType contains 'video/' and trashed = false";
        FileList result = drive.files().list()
                .setQ(query)
                .setFields("files(id, name)")
                .execute();

        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.err.println("No videos found in content folder.");
            System.exit(1);
        }

        // 4) Pick a random video
        Random rand = new Random();
        File selectedFile = files.get(rand.nextInt(files.size()));
        System.out.println("Selected file: " + selectedFile.getName() + " (ID: " + selectedFile.getId() + ")");

        // 5) Download the video to local downloads folder
        String downloadsDir = Paths.get(System.getProperty("user.dir"), "downloads").toString();
        new java.io.File(downloadsDir).mkdirs();

        String localFilePath = Paths.get(downloadsDir, selectedFile.getName()).toString();
        try (OutputStream out = new FileOutputStream(localFilePath)) {
            drive.files().get(selectedFile.getId()).executeMediaAndDownloadTo(out);
        }
        System.out.println("Downloaded file to: " + localFilePath);

        // 6) Move file to posted folder in Drive
        drive.files().update(selectedFile.getId(), null)
                .setAddParents(postedFolderId)      // folder to add
                .setRemoveParents(contentFolderId)  // folder to remove
                .execute();
        System.out.println("Moved file to posted folder.");

        // 7) Build YouTube client
        YouTube youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        // 8) Create Video metadata
        VideoSnippet snippet = new VideoSnippet();
        snippet.setTitle("Auto Uploaded Video"); // Replace with dynamic title if needed
        snippet.setDescription("Uploaded via Java API"); // Replace description if needed
        snippet.setCategoryId("26"); // People & Blogs

        VideoStatus status = new VideoStatus();
        status.setPrivacyStatus("public"); // or "unlisted" / "private"

        Video videoObjectDefiningMetadata = new Video();
        videoObjectDefiningMetadata.setSnippet(snippet);
        videoObjectDefiningMetadata.setStatus(status);

        // 9) Upload the file to YouTube using InputStreamContent (no content length)
        java.io.File mediaFile = new java.io.File(localFilePath);
        InputStreamContent mediaContent = new InputStreamContent(
                "video/*",
                new java.io.FileInputStream(mediaFile)
        );

        YouTube.Videos.Insert request = youtube.videos()
                .insert("snippet,status", videoObjectDefiningMetadata, mediaContent);

        MediaHttpUploader uploader = request.getMediaHttpUploader();
        uploader.setDirectUploadEnabled(false); // resumable
        uploader.setProgressListener(new MediaHttpUploaderProgressListener() {
            public void progressChanged(MediaHttpUploader uploader) throws java.io.IOException {
                switch (uploader.getUploadState()) {
                    case INITIATION_STARTED:
                        System.out.println("Upload Initiation Started");
                        break;
                    case INITIATION_COMPLETE:
                        System.out.println("Upload Initiation Completed");
                        break;
                    case MEDIA_IN_PROGRESS:
                        System.out.println("Uploading... Bytes uploaded: " + uploader.getNumBytesUploaded());
                        break;
                    case MEDIA_COMPLETE:
                        System.out.println("Upload Completed!");
                        break;
                    case NOT_STARTED:
                        System.out.println("Upload Not Started");
                        break;
                }
            }
        });

        Video returnedVideo = request.execute();
        System.out.println("Uploaded video ID: " + returnedVideo.getId());
    }
}


*/
