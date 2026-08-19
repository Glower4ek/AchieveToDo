package com.diskree.achievetodo.client;

public final class CompatibilityJoinGate {

    private boolean preparedJoinArmed;

    public boolean shouldRunPreparation() {
        if (!preparedJoinArmed) {
            return true;
        }
        preparedJoinArmed = false;
        return false;
    }

    public void runPreparedJoin(Runnable joinAction) {
        preparedJoinArmed = true;
        try {
            joinAction.run();
        } catch (RuntimeException | Error e) {
            preparedJoinArmed = false;
            throw e;
        }
    }

    public void reset() {
        preparedJoinArmed = false;
    }
}
