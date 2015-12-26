obsqr - minimalistic QR scanner for Android
===========================================

[![Build Status](https://travis-ci.org/trikita/obsqr.svg?branch=master)](https://travis-ci.org/trikita/obsqr)

obsqr is a fast and lightweight QR scanner application for Android.

![Obsqr screenshot](http://i.imgur.com/zSb1Jib.png)

Requirements
------------

To make it run on your Android device you need to have Andoird 4.0 or higher,
and of course your device should have a camera.

Build
-----

obsqr uses zbar library to decode QR images. Zbar sources has been added into
libzbar subdirectory, modified to be compiled with NDK, also added minimal
iconv implementation.

Since Android NDK is still a second-class citizen - the best way to deal with
native code is to build it manually, then copy `*.so` into `src/main/jniLibs`
of the main project tree.

To rebuild ZBar from the sources (optional step):

	cd libzbar
	export NDK_PATH=/path/to/your/ndk
	NDK_PROJECT_PATH=$(pwd) $NDK_PATH/ndk-build
	cp -rv libs/* ../src/main/jniLibs/

To build obsqr APK:

	./gradlew build

To run tests:

	./gradlew connectedAndroidTest

License
-------

obsqr is free software distributed under the terms of the MIT license.
See LICENSE file for more details.

