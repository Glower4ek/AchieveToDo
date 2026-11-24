package com.diskree.achievetodo.core;

import java.lang.instrument.Instrumentation;

/**
 * Premain-Class of the jar. It only fills in the release defaults ({@link ReleaseLaunchDefaults}) and then hands
 * over to the unchanged certified native bootstrap. Keeping this outside
 * {@code NativeTransactionPremain} leaves that certified source file byte-for-byte as it was in rc.1.
 */
public final class ReleaseLaunchPremain {
    public static void premain(String arguments, Instrumentation instrumentation) throws Exception {
        ReleaseLaunchDefaults.apply(System::getProperty, System::setProperty);
        com.diskree.achievetodo.gameplay.nativeguard.NativeTransactionPremain.premain(arguments, instrumentation);
    }

    private ReleaseLaunchPremain() {}
}
