#![cfg(test)]

use jni::{InitArgsBuilder, JNIVersion, JavaVM};

pub fn create_jvm() -> JavaVM {
    let args = InitArgsBuilder::new()
        .version(JNIVersion::V1_8)
        .option("-Djava.class.path=target/test-classes")
        .build()
        .expect("failed to build JVM args");

    JavaVM::new(args).expect("failed to create JVM")
}
