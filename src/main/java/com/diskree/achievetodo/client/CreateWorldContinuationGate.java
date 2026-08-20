package com.diskree.achievetodo.client;

public final class CreateWorldContinuationGate {

    private boolean pendingUserCreate;
    private boolean readyToResume;

    public void beginUserCreate() {
        pendingUserCreate = true;
        readyToResume = false;
    }

    public void markDatapacksReady() {
        if (pendingUserCreate) {
            readyToResume = true;
        }
    }

    public boolean hasPendingUserCreate() {
        return pendingUserCreate;
    }

    public boolean shouldResumeWhenCreateScreenReopens() {
        return pendingUserCreate && readyToResume;
    }

    public void consumeResumedCreate() {
        pendingUserCreate = false;
        readyToResume = false;
    }

    public void clear() {
        pendingUserCreate = false;
        readyToResume = false;
    }
}
