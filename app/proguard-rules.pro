# SQLCipher's documented ProGuard rules preserve classes and descriptors
# referenced from its JNI layer. Keep these rules aligned with upstream guidance.
-keep,includedescriptorclasses class net.sqlcipher.** { *; }
-keep,includedescriptorclasses interface net.sqlcipher.** { *; }
