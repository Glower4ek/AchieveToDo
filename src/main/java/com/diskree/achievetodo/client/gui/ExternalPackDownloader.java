package com.diskree.achievetodo.client.gui;

import com.diskree.achievetodo.client.AchieveToDoClient;
import com.diskree.achievetodo.client.ExternalPack;
import com.diskree.achievetodo.client.Utils;
import com.diskree.achievetodo.AchieveToDoMod;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Environment(EnvType.CLIENT)
public class ExternalPackDownloader extends ConfirmScreen {

    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_MARGIN = 5;

    private final Screen parent;
    private final ExternalPack externalPack;
    private final BooleanConsumer exitCallback;
    private boolean isFileDownloaded;
    private boolean closed;

    private final boolean inGameDownloadSupported;
    private Button downloadButton;
    private Button backButton;
    private DownloadAttempt downloadAttempt;

    public ExternalPackDownloader(
        Screen parent,
        @NotNull ExternalPack pack,
        BooleanConsumer exitCallback,
        boolean isOutdatedVersion
    ) {
        super(
            null,
            AchieveToDoClient.translate("downloader.title_prefix")
                .append(
                    Component.literal(pack.getTitle())
                        .withStyle(pack.getColor(), ChatFormatting.ITALIC)
                ),
            AchieveToDoClient.translate(
                    isOutdatedVersion ? "downloader.reason.outdated" : pack.getReasonKey()
                )
                .append(CommonComponents.NEW_LINE)
                .append(CommonComponents.NEW_LINE)
                .append(CommonComponents.NEW_LINE)
                .append(CommonComponents.NEW_LINE)
                .append(CommonComponents.NEW_LINE)
                .append(
                    AchieveToDoClient.translate(
                        pack.isInGameDownloadSupported() ? "downloader.automatically_info" : "downloader.manually_info"
                    ).withStyle(DesignCodePalette.TEXT_COLOR)
                )
        );
        this.parent = parent;
        this.externalPack = pack;
        this.exitCallback = exitCallback;
        inGameDownloadSupported = pack.isInGameDownloadSupported();
    }

    @Override
    public void onClose() {
        cancelDownload();
        if (!closed && minecraft != null) {
            closed = true;
            minecraft.setScreenAndShow(parent);
            exitCallback.accept(isFileDownloaded);
        }
    }

    @Override
    public void removed() {
        cancelDownload();
        super.removed();
    }

    @Override
    protected void addButtons(LinearLayout buttons) {
        downloadButton = buttons.addChild(
            Button.builder(
                    AchieveToDoClient.translate("downloader.download"),
                    button -> {
                        if (inGameDownloadSupported) {
                            startDownload();
                        } else {
                            Util.getPlatform().openUri(externalPack.getDownloadUrl());
                        }
                    }
                )
                .tooltip(inGameDownloadSupported ? null :
                    Tooltip.create(AchieveToDoClient.translate("downloader.download.tooltip"))
                )
                .size(inGameDownloadSupported ? BUTTON_WIDTH * 2 + BUTTON_MARGIN : BUTTON_WIDTH, BUTTON_HEIGHT)
                .build()
        );

        if (!inGameDownloadSupported) {
            buttons.addChild(
                Button.builder(
                        AchieveToDoClient.translate("downloader.select_file"),
                        button -> {
                            try (MemoryStack stack = MemoryStack.stackPush()) {
                                PointerBuffer filters = stack.mallocPointer(1);
                                filters.put(0, stack.UTF8("*.zip"));

                                @SuppressWarnings("DataFlowIssue")
                                String selectedFilePath = TinyFileDialogs.tinyfd_openFileDialog(
                                    AchieveToDoClient.translate("downloader.select_file").getString(),
                                    System.getProperty("user.home"),
                                    filters,
                                    null,
                                    false
                                );
                                if (selectedFilePath != null) {
                                    handleDatapackFile(Paths.get(selectedFilePath));
                                }
                            }
                        }
                    )
                    .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build()
            );
        }

        backButton = buttons.addChild(
            Button.builder(
                    CommonComponents.GUI_BACK,
                    button -> {
                        if (downloadAttempt != null && downloadAttempt.isActive()) {
                            cancelDownload();
                            hideDownloadingUI();
                        } else {
                            onClose();
                        }
                    }
                )
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .build()
        );

        buttons.addChild(
            Button.builder(
                    AchieveToDoClient.translate("downloader.learn_more"),
                    button -> Util.getPlatform().openUri(externalPack.getPageUrl())
                )
                .tooltip(Tooltip.create(AchieveToDoClient.translate("downloader.learn_more.tooltip")))
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .build()
        );
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        super.onFilesDrop(paths);
        if (paths == null || paths.size() != 1) {
            return;
        }
        handleDatapackFile(paths.getFirst());
    }

    private void handleDatapackFile(Path path) {
        if (minecraft == null || closed) {
            return;
        }
        cancelDownload();
        downloadAttempt = new DownloadAttempt();
        completeInstallation(downloadAttempt, path);
    }

    private void completeInstallation(DownloadAttempt attempt, Path path) {
        if (!isCurrent(attempt)) {
            return;
        }
        Path target = new File(minecraft.gameDirectory, "datapacks").toPath().resolve(externalPack.getFileName());
        attempt.complete(
            () -> PackInstaller.install(path, target, externalPack.getSha1(), externalPack.getWrapperSha1()),
            () -> {
                isFileDownloaded = true;
                onClose();
            },
            error -> showFailure(error, "installation")
        );
    }

    private void startDownload() {
        if (minecraft == null || closed) {
            return;
        }
        cancelDownload();
        DownloadAttempt attempt = new DownloadAttempt();
        downloadAttempt = attempt;
        backButton.setMessage(CommonComponents.GUI_CANCEL);
        downloadButton.active = false;
        String url = externalPack.getDownloadUrl();
        Path downloadTempDirectory = new File(minecraft.gameDirectory, ".datapacks_temp").toPath();
        CompletableFuture.runAsync(() -> {
            HttpURLConnection connection = null;
            Path tempFile = null;
            try {
                if (!attempt.isActive()) {
                    return;
                }
                Files.createDirectories(downloadTempDirectory);
                tempFile = Files.createTempFile(downloadTempDirectory, "download-", ".zip");
                URI uri = URI.create(url);
                connection = (HttpURLConnection) uri.toURL().openConnection();
                connection.setDoInput(true);
                connection.setDoOutput(false);
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode / 100 != 2) {
                    throw new IOException("HTTP error code: " + responseCode);
                }

                long totalBytes = connection.getContentLengthLong();
                if (totalBytes <= 0) {
                    throw new IOException("Missing download size");
                }

                try (InputStream in = connection.getInputStream();
                     OutputStream out = Files.newOutputStream(tempFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long downloadedBytes = 0;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        if (!attempt.isActive()) {
                            return;
                        }
                        out.write(buffer, 0, bytesRead);
                        downloadedBytes += bytesRead;

                        // Installation/identity validation, not received bytes, completes the operation.
                        long progress = Math.min(99, (downloadedBytes * 100) / totalBytes);
                        minecraft.execute(() -> {
                            if (isCurrent(attempt)) {
                                downloadButton.setMessage(Component.translatable("mco.download.downloading").append(": " + progress + "%"));
                            }
                        });
                    }
                    if (downloadedBytes != totalBytes) {
                        throw new IOException("Incomplete download");
                    }
                }
                // Close the worker's streams before the client thread reads or removes the file.
                Path completedFile = tempFile;
                minecraft.execute(() -> {
                    try {
                        completeInstallation(attempt, completedFile);
                    } finally {
                        deleteDownload(completedFile);
                    }
                });
                tempFile = null; // Client-thread completion now owns cleanup, including stale/cancelled completion.
            } catch (Exception e) {
                minecraft.execute(() -> {
                    if (isCurrent(attempt)) {
                        attempt.fail(e, error -> showFailure(error, "download"));
                    }
                });
            } finally {
                if (tempFile != null) {
                    deleteDownload(tempFile);
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).exceptionally(error -> {
            logFailure(error, "download completion");
            return null;
        });
    }

    private boolean isCurrent(DownloadAttempt attempt) {
        return !closed && minecraft != null && downloadAttempt == attempt && attempt.isActive();
    }

    private void cancelDownload() {
        if (downloadAttempt != null) {
            downloadAttempt.cancel();
        }
    }

    private void showFailure(Exception error, String stage) {
        logFailure(error, stage);
        hideDownloadingUI();
        minecraft.setScreenAndShow(new ErrorScreen(this,
            error instanceof InvalidPackException ? "error.wrong_datapack_file" : "error.unknown"));
    }

    private void logFailure(Throwable error, String stage) {
        // Exception messages can contain local paths. Keep the pack/stage/type, not those paths.
        AchieveToDoMod.logger.warn("External pack {} failed during {} ({}, cleanup failures: {})",
            externalPack.name(), stage, error.getClass().getSimpleName(), error.getSuppressed().length);
    }

    private void deleteDownload(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException | SecurityException error) {
            logFailure(error, "temporary file cleanup");
        }
    }

    private void hideDownloadingUI() {
        backButton.setMessage(CommonComponents.GUI_BACK);
        downloadButton.setMessage(AchieveToDoClient.translate("downloader.download"));
        downloadButton.active = true;
    }

    static final class InvalidPackException extends IOException {
        InvalidPackException() {
            super("External pack identity does not match the pinned source");
        }
    }

    /** Shared by downloaded, selected and dropped files; only validated source bytes enter the global cache. */
    static final class PackInstaller {
        static void install(Path source, Path destination, String expectedSha1, String wrapperSha1) throws IOException {
            String sourceSha1 = digest(source);
            boolean wrapper = wrapperSha1 != null && wrapperSha1.equalsIgnoreCase(sourceSha1);
            if (!wrapper && !expectedSha1.equalsIgnoreCase(sourceSha1)) {
                throw new InvalidPackException();
            }
            Path target = destination.toAbsolutePath();
            Files.createDirectories(target.getParent());
            Path staged = Files.createTempFile(target.getParent(), ".achievetodo-pack-", ".zip");
            Throwable failure = null;
            boolean committed = false;
            try {
                if (wrapper) {
                    extractPinnedArchive(source, staged, expectedSha1);
                } else {
                    Files.copy(source, staged, StandardCopyOption.REPLACE_EXISTING);
                }
                if (!expectedSha1.equalsIgnoreCase(digest(staged))) {
                    throw new InvalidPackException();
                }
                // Fail closed if atomic replacement is unavailable: never truncate the old destination.
                Files.move(staged, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                committed = true;
            } catch (IOException | RuntimeException error) {
                failure = error;
                throw error;
            } finally {
                if (!committed) {
                    try {
                        Files.deleteIfExists(staged);
                    } catch (IOException cleanup) {
                        if (failure != null) {
                            failure.addSuppressed(cleanup);
                        } else {
                            throw cleanup;
                        }
                    }
                }
            }
        }

        private static void extractPinnedArchive(Path wrapper, Path staged, String expectedSha1) throws IOException {
            try (ZipFile zip = new ZipFile(wrapper.toFile())) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory()) {
                        continue;
                    }
                    // Never extract wrapper paths into the cache or choose an unvalidated README/first entry.
                    try (InputStream input = zip.getInputStream(entry)) {
                        Files.copy(input, staged, StandardCopyOption.REPLACE_EXISTING);
                    }
                    if (expectedSha1.equalsIgnoreCase(digest(staged))) {
                        return;
                    }
                }
            }
            throw new InvalidPackException();
        }

        private static String digest(Path path) throws IOException {
            try {
                String result = Utils.calculateSHA1(path);
                if (result == null) {
                    throw new IOException("Missing external pack source");
                }
                return result;
            } catch (RuntimeException error) {
                throw new IOException("Cannot read external pack source", error);
            }
        }
    }

    @FunctionalInterface
    interface Installation {
        void run() throws IOException;
    }

    /** One attempt owns its queued completions; cancellation and commit have a single ordering point. */
    static final class DownloadAttempt {
        private boolean cancelled;
        private boolean completed;

        synchronized boolean isActive() {
            return !cancelled && !completed;
        }

        synchronized void cancel() {
            cancelled = true;
        }

        synchronized void fail(Exception error, Consumer<Exception> failure) {
            if (isActive()) {
                cancelled = true;
                failure.accept(error);
            }
        }

        synchronized void complete(Installation installation, Runnable success, Consumer<Exception> failure) {
            if (!isActive()) {
                return;
            }
            try {
                installation.run();
            } catch (IOException | RuntimeException error) {
                fail(error, failure);
                return;
            }
            completed = true;
            success.run();
        }
    }
}
