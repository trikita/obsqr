obsqr - minimalistic QR scanner for Android
===========================================

obsqr is a fast and lightweight QR scanner application for Android.

Requirements
------------

To make it run on your Android device you need to have Andoird 1.6 or higher.
Both, ARM and x86 architectures are supported.

Build
-----

obsqr uses zbar library to decode QR images. Zbar mirror is added as a subrepo
so it's fetched automatically. Zbar sources are added to the jni directory as
symlinks.

Update project accodring to your SDK version:

	$ android update project -p . -t <your-target>

Edit local.properties by specifying path to the NDK:

	...
	sdk.path=/path/to/sdk
	ndk.path=/path/to/ndk
	...

Finally, run ant to build obsqr:

	$ ant debug

After this step, you should get an *.apk inside the 'bin' directory.

License
-------

obsqr is free software distributed under the terms of the MIT license.
See LICENSE file for more details.

