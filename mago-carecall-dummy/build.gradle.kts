// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

buildscript {
    val grpcVersion by extra("1.49.0")
    val protobufVersion by extra("0.9.1")
    val grpcKotlinVersion by extra("1.2.1") // gRPC Kotlin 플러그인 버전 추가

    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.google.protobuf:protobuf-gradle-plugin:$protobufVersion")
        classpath("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion") // gRPC Kotlin stub 추가
    }
}
