# How to use it

Paste the following code in your build.gradle file

```groovy
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}

dependencies {
	compile 'com.github.tentello:chino-notifications:1.2'
}
```