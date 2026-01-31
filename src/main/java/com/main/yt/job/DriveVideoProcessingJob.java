package com.main.yt.job;

import com.google.api.services.drive.model.File;
import com.main.yt.drive.DriveClient;
import com.main.yt.drive.DriveStateService;
import com.main.yt.video.VideoOverlayService;
import com.main.yt.youtube.YouTubeUploader;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

@Component
public class DriveVideoProcessingJob {

    private final DriveClient drive;
    private final VideoOverlayService overlay;
    private final YouTubeUploader uploader;
    private final Random random = new Random();
    private final DriveStateService stateService;

    String stateFolderId = System.getenv("STATE_FOLDER_ID");



    public DriveVideoProcessingJob(
            DriveClient drive,
            VideoOverlayService overlay,
            YouTubeUploader uploader
    ) {
        this.drive = drive;
        this.overlay = overlay;
        this.uploader = uploader;

        // 🔹 Create DriveStateService once
        this.stateService = new DriveStateService(
                drive.getDrive(),      // Google Drive SDK object
                stateFolderId
        );
    }


    public void run() throws Exception {

        String videoFolderId = requireEnv("CONTENT_FOLDER_ID");
        String audioFolderId = requireEnv("AUDIO_FOLDER_ID");
        String quoteFolderId = requireEnv("QUOTE_FOLDER_ID");
        String postedFolderId = requireEnv("POSTED_FOLDER_ID");

        // ============================
        // 1️⃣ FETCH DRIVE FILES (SAFE)
        // ============================
        List<File> videos = drive.listVideos(videoFolderId);
        if (videos.isEmpty()) {
            System.out.println("⚠️ No videos found. Job finished.");
            return;
        }

        List<File> audios = drive.listFiles(audioFolderId);
        if (audios.isEmpty()) {
            throw new IllegalStateException("❌ No audio files found in Drive folder: " + audioFolderId);
        }

        List<File> quoteFiles = drive.listFiles(quoteFolderId);
        if (quoteFiles.isEmpty()) {
            throw new IllegalStateException("❌ No quote file found in Drive folder: " + quoteFolderId);
        }

        // ============================
        // 2️⃣ LOAD QUOTES
        // ============================
        File quoteFile = quoteFiles.get(0);
        java.io.File localQuotes = drive.download(quoteFile, Path.of("tmp"));
        List<String> quotes = Files.readAllLines(localQuotes.toPath());

        if (quotes.isEmpty()) {
            throw new IllegalStateException("❌ Quote file is empty: " + quoteFile.getName());
        }

        System.out.println("✅ Videos: " + videos.size());
        System.out.println("✅ Quotes: " + quotes.size());
        System.out.println("✅ Audios: " + audios.size());

        //READ QUOTE INDEX (FROM DRIVE)
        int quoteIndex = stateService.readQuoteIndex();

        if (quoteIndex >= quotes.size()) {
            System.out.println("⚠️ No more quotes left. Stopping.");
            return;
        }

        // ============================
        // 4️⃣ PICK QUOTE
        // ============================
        String quote = quotes.get(quoteIndex);
        System.out.println("🎯 Using quote index: " + quoteIndex);

        // ============================
        // 3️⃣ PROCESS VIDEOS
        // ============================
        File v = drive.pickRandomVideo(System.getenv("CONTENT_FOLDER_ID"));

            File audioDrive = audios.get(random.nextInt(audios.size()));

            System.out.println("🎬 Processing video: " + v.getName());
            System.out.println("📝 Quote: " + quote);
            System.out.println("🎵 Audio: " + audioDrive.getName());

            java.io.File localVideo = drive.download(v, Path.of("downloads"));
            java.io.File localAudio = drive.download(audioDrive, Path.of("audios"));

            System.out.println("🎥 Video exists : " + localVideo.exists());
            System.out.println("🎵 Audio exists : " + localAudio.exists());
            System.out.println("📄 Video path   : " + localVideo.getAbsolutePath());
            System.out.println("🎵 Audio path   : " + localAudio.getAbsolutePath());

            java.io.File processed =
                    overlay.process(localVideo, quote, localAudio);

            // ============================
            // 4️⃣ UPLOAD TO YOUTUBE
            // ============================

             uploader.upload(processed);

            // ============================
            // 5️⃣ AFTER SUCCESSFUL VIDEO POST
            // ============================
            stateService.saveQuoteIndex(quoteIndex + 1);
            System.out.println("✅ Quote index updated to: " + (quoteIndex + 1));

            // ============================
            // 5️⃣ MOVE VIDEO IN DRIVE
            // ============================
            /* drive.move(
                    v.getId(),
                    videoFolderId,
                    postedFolderId
            );*/

            // ============================
            // 6️⃣ CLEANUP
            // ============================
            localVideo.delete();
            localAudio.delete();

            // 1️⃣ Upload processed video to Drive
            File uploaded = drive.uploadVideo(
                    processed,
                    System.getenv("POSTED_FOLDER_ID")
            );

            processed.delete();


        System.out.println("✅ Job completed successfully.");
    }

    // ============================
    // ENV VALIDATION
    // ============================
    private String requireEnv(String key) {
        String val = System.getenv(key);
        if (val == null || val.isBlank()) {
            throw new IllegalStateException(
                    "Missing required environment variable: " + key
            );
        }
        return val;
    }

}
