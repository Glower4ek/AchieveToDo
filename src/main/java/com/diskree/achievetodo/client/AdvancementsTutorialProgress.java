package com.diskree.achievetodo.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Keeps an early acknowledgement without skipping vanilla movement/look prerequisites. */
public final class AdvancementsTutorialProgress {
    private static final String RECEIPT = "open-advancements-v1\n";
    private final Path earlyCompletionReceipt;
    private boolean acknowledged;
    private boolean advanced;
    private Runnable hideToast;

    public AdvancementsTutorialProgress(Path earlyCompletionReceipt) throws IOException {
        this.earlyCompletionReceipt = earlyCompletionReceipt;
        acknowledged = Files.exists(earlyCompletionReceipt)
            && RECEIPT.equals(Files.readString(earlyCompletionReceipt));
    }

    public boolean mayShowToast() {
        return !acknowledged && hideToast == null;
    }

    public void showToast(Runnable show, Runnable hide) {
        if (mayShowToast()) {
            hideToast = hide;
            show.run();
        }
    }

    public void opened(boolean movementAndLookComplete, Runnable advanceVanilla) throws IOException {
        clear();
        if (!acknowledged && !movementAndLookComplete) {
            Files.createDirectories(earlyCompletionReceipt.getParent());
            Files.writeString(earlyCompletionReceipt, RECEIPT, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
        }
        acknowledged = true;
        advanceIfReady(movementAndLookComplete, advanceVanilla);
    }

    public void advanceIfReady(boolean movementAndLookComplete, Runnable advanceVanilla) {
        if (acknowledged && movementAndLookComplete && !advanced) {
            advanced = true;
            clear();
            advanceVanilla.run();
        }
    }

    public void clear() {
        if (hideToast != null) {
            Runnable hide = hideToast;
            hideToast = null;
            hide.run();
        }
    }
}
