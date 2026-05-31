package com.iae;

/**
 * Non-JavaFX entry point for native launchers.
 *
 * Java detects classes that directly extend Application and expects JavaFX on
 * the module path before main() runs. Keeping the packaged entry point separate
 * allows jpackage to launch the classpath-based application correctly.
 */
public final class Launcher {

    private Launcher() {}

    public static void main(String[] args) {
        Main.main(args);
    }
}
