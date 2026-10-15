package com.diskree.achievetodo.client.gui;

import com.diskree.achievetodo.client.ExternalPack;
import com.sun.nio.file.ExtendedOpenOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ExternalPackDownloaderTest {
    private static final Path NATIVE = Path.of("src/test/resources/phase_b_certification/bacap_1_21_pinned.zip");
    private static final String NATIVE_SHA1 = "14da3f07b5467e8b59ffc0253fd8212c938cd739";

    @TempDir
    Path temporary;

    private Path target() {
        return temporary.resolve("global/bacap.zip");
    }

    private void install(Path source) throws IOException {
        ExternalPackDownloader.PackInstaller.install(source, target(), NATIVE_SHA1, null);
    }

    private void seedValidDestination() throws IOException {
        Files.createDirectories(target().getParent());
        Files.copy(NATIVE, target());
    }

    private void assertNativeDestinationAndNoStage() throws IOException {
        assertEquals(-1, Files.mismatch(NATIVE, target()));
        try (var files = Files.list(target().getParent())) {
            assertEquals(List.of("bacap.zip"), files.map(p -> p.getFileName().toString()).sorted().toList());
        }
    }

    private static String sha1(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(path)));
    }

    private Path wrapper(boolean validInner) throws IOException {
        Path result = temporary.resolve("wrapper.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(result))) {
            zip.putNextEntry(new ZipEntry("README.txt"));
            zip.write("Select the pinned inner pack, not the first file".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("nested/pack.zip"));
            zip.write(validInner ? Files.readAllBytes(NATIVE) : new byte[]{1, 2, 3});
            zip.closeEntry();
        }
        return result;
    }

    @Test
    void installsActualPinnedNativeSourceIntoAbsentDestination() throws Exception {
        assertEquals(ExternalPack.BACAP.getSha1(), sha1(NATIVE));
        install(NATIVE);
        assertNativeDestinationAndNoStage();
    }

    @Test
    void replacesExistingDifferentArchiveAtomically() throws Exception {
        Files.createDirectories(target().getParent());
        Files.writeString(target(), "previous different archive");
        install(NATIVE);
        assertNativeDestinationAndNoStage();
    }

    @Test
    void repeatedValidInstallationIsSafeIncludingSourceEqualToDestination() throws Exception {
        install(NATIVE);
        install(NATIVE);
        install(target());
        assertNativeDestinationAndNoStage();
    }

    @Test
    void invalidSourcePreservesExistingValidBytes() throws Exception {
        seedValidDestination();
        Path invalid = temporary.resolve("invalid.zip");
        Files.writeString(invalid, "not the pinned pack");
        assertThrows(ExternalPackDownloader.InvalidPackException.class, () -> install(invalid));
        assertNativeDestinationAndNoStage();
    }

    @Test
    void missingSourcePreservesExistingValidBytes() throws Exception {
        seedValidDestination();
        assertThrows(IOException.class, () -> install(temporary.resolve("missing.zip")));
        assertNativeDestinationAndNoStage();
    }

    @Test
    void filesystemReplacementFailurePreservesValidDestinationAndReportsFailure() throws Exception {
        assumeTrue(System.getProperty("os.name").startsWith("Windows"), "Native Windows deny-delete sharing control");
        seedValidDestination();
        var attempt = new ExternalPackDownloader.DownloadAttempt();
        AtomicInteger successes = new AtomicInteger();
        AtomicReference<Exception> failure = new AtomicReference<>();
        // Real OS failure at the actual atomic replacement, not a mocked installer or publisher.
        try (FileChannel ignored = FileChannel.open(target(), StandardOpenOption.READ, ExtendedOpenOption.NOSHARE_DELETE)) {
            attempt.complete(() -> install(NATIVE), successes::incrementAndGet, failure::set);
            assertInstanceOf(IOException.class, failure.get());
            assertEquals(0, successes.get());
            assertFalse(attempt.isActive());
            assertNativeDestinationAndNoStage();
        }
    }

    @Test
    void supportedWrapperSelectsPinnedInnerArchiveAndReplacesExistingDestination() throws Exception {
        seedValidDestination();
        Path wrapper = wrapper(true);
        ExternalPackDownloader.PackInstaller.install(wrapper, target(), NATIVE_SHA1, sha1(wrapper));
        assertNativeDestinationAndNoStage();
        assertFalse(Files.exists(target().getParent().resolve("README.txt")));
        assertFalse(Files.exists(target().getParent().resolve("nested")));
    }

    @Test
    void wrapperCanInstallIntoAbsentDestination() throws Exception {
        Path wrapper = wrapper(true);
        ExternalPackDownloader.PackInstaller.install(wrapper, target(), NATIVE_SHA1, sha1(wrapper));
        assertNativeDestinationAndNoStage();
    }

    @Test
    void validWrapperIdentityCannotInstallUnverifiedInnerArchive() throws Exception {
        seedValidDestination();
        Path wrapper = wrapper(false);
        assertThrows(ExternalPackDownloader.InvalidPackException.class,
            () -> ExternalPackDownloader.PackInstaller.install(wrapper, target(), NATIVE_SHA1, sha1(wrapper)));
        assertNativeDestinationAndNoStage();
    }

    @Test
    void wrongWrapperIdentityPreservesPreviousDestination() throws Exception {
        seedValidDestination();
        Path wrapper = wrapper(true);
        assertThrows(ExternalPackDownloader.InvalidPackException.class,
            () -> ExternalPackDownloader.PackInstaller.install(wrapper, target(), NATIVE_SHA1, "0000000000000000000000000000000000000000"));
        assertNativeDestinationAndNoStage();
    }

    @Test
    void installationFailureCannotInvokeSuccessEvenOnRepeatedCompletion() throws Exception {
        seedValidDestination();
        Path bad = temporary.resolve("bad.zip");
        Files.writeString(bad, "bad source");
        var attempt = new ExternalPackDownloader.DownloadAttempt();
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        attempt.complete(() -> install(bad), successes::incrementAndGet, error -> failures.incrementAndGet());
        attempt.complete(() -> install(NATIVE), successes::incrementAndGet, error -> failures.incrementAndGet());
        assertEquals(0, successes.get());
        assertEquals(1, failures.get());
        assertNativeDestinationAndNoStage();
    }

    @Test
    void successfulInstallationCompletesExactlyOnce() throws Exception {
        var attempt = new ExternalPackDownloader.DownloadAttempt();
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        attempt.complete(() -> install(NATIVE), successes::incrementAndGet, error -> failures.incrementAndGet());
        attempt.complete(() -> install(NATIVE), successes::incrementAndGet, error -> failures.incrementAndGet());
        attempt.fail(new IOException("late error"), error -> failures.incrementAndGet());
        assertEquals(1, successes.get());
        assertEquals(0, failures.get());
        assertFalse(attempt.isActive());
        assertNativeDestinationAndNoStage();
    }

    @Test
    void cancellationBeforeQueuedCompletionCannotInstallPartialFile() throws Exception {
        seedValidDestination();
        Path partial = temporary.resolve("partial.zip");
        Files.writeString(partial, "partial download");
        var attempt = new ExternalPackDownloader.DownloadAttempt();
        AtomicInteger callbacks = new AtomicInteger();
        var queue = new ArrayDeque<Runnable>();
        queue.add(() -> attempt.complete(() -> install(partial), callbacks::incrementAndGet, error -> callbacks.incrementAndGet()));
        attempt.cancel();
        queue.remove().run();
        assertEquals(0, callbacks.get());
        assertNativeDestinationAndNoStage();
    }

    @Test
    void staleCompletionAfterRetryCannotOverwriteOrCompleteNewAttempt() throws Exception {
        var stale = new ExternalPackDownloader.DownloadAttempt();
        var retry = new ExternalPackDownloader.DownloadAttempt();
        AtomicInteger staleCallbacks = new AtomicInteger();
        AtomicInteger retryCallbacks = new AtomicInteger();
        stale.cancel();
        retry.complete(() -> install(NATIVE), retryCallbacks::incrementAndGet, error -> fail(error));
        // This is valid for its older attempt's pin, so validation alone cannot protect the newer destination.
        Path staleSource = wrapper(true);
        String stalePin = sha1(staleSource);
        stale.complete(() -> ExternalPackDownloader.PackInstaller.install(staleSource, target(), stalePin, null),
            staleCallbacks::incrementAndGet, error -> staleCallbacks.incrementAndGet());
        assertEquals(0, staleCallbacks.get());
        assertEquals(1, retryCallbacks.get());
        assertNativeDestinationAndNoStage();
    }

    @Test
    void cancelledAttemptIgnoresLateNetworkFailureAndRetryRemainsPossible() throws Exception {
        var cancelled = new ExternalPackDownloader.DownloadAttempt();
        AtomicInteger failures = new AtomicInteger();
        cancelled.cancel();
        cancelled.fail(new IOException("late network failure"), error -> failures.incrementAndGet());
        var retry = new ExternalPackDownloader.DownloadAttempt();
        AtomicInteger successes = new AtomicInteger();
        retry.complete(() -> install(NATIVE), successes::incrementAndGet, error -> failures.incrementAndGet());
        assertEquals(0, failures.get());
        assertEquals(1, successes.get());
        assertNativeDestinationAndNoStage();
    }
}
