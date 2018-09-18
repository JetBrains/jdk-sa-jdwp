package com.jetbrains.sa.jdi;

/**
 * @author egor
 */
class CompatibilityHelper {
    static final Compatibility INSTANCE;

    static {
        Compatibility instance = null;
        String version = System.getProperty("java.specification.version");
        if (version.equals("1.6") || version.equals("1.7")) {
            instance = new CompatibilityHelper6();
        }
        else if (version.equals("1.8") || version.equals("9")) {
            try {
                instance = (Compatibility) Class.forName("com.jetbrains.sa.jdi.CompatibilityHelper8").newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                instance = (Compatibility) Class.forName("com.jetbrains.sa.jdi.CompatibilityHelper10").newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        INSTANCE = instance;
    }
}
