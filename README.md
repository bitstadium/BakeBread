# BakeBread

Copyright (c) Microsoft Corporation

All rights reserved. Released under MIT License.

## What's it?

BakeBread is a collection of tools that facilitate postmortem debugging of [Minidump](https://msdn.microsoft.com/en-us/library/windows/desktop/ms680369.aspx) files, such as those captured by [Breakpad](https://chromium.googlesource.com/breakpad/breakpad). It's originally been developed to complement [HockeyApp](https://www.hockeyapp.net) for Android.

Naive stack scanning - based on the assumption that every valid code address found in the stack denotes a call frame - is simple and straightforward, but in many cases produces false positives and unrealistic results. Dump processing on the cloud side adds to the complications because platform binaries (especially on fragmented platforms such as Android) stay out of scope. It would be just natural to utilize the intimate knowledge of frame and object layout shared between the compiler and the debugger from the same NDK toolchain. Unfortunately, the minidump format is not native to Android NDK toolchains; the core dump format is. The primary functionality of BakeBread is **conversion of minidumps into core dumps** for further high-level postmortem debugging; no longer bits and hexadecimals, but code lines and variables.

**If you use [HockeyApp for Android NDK](https://support.hockeyapp.net/kb/client-integration-android/hockeyapp-for-android-ndk-early-access) or a similar native crash reporting solution, you can start using BakeBread straight away.** To improve analysis quality, we suggest reporting memory areas referenced from the stack of the crashed thread in addition to the crashed thread stack itself (if you are concerned about the upload size, HockeyApp understands and processes dumps uploaded with GZip compression). If, for instance, your libraries use smart pointers, such an enhancement is really a must. See [MOREDUMP.md](MOREDUMP.md) for details.

## Getting started

BakeBread has been tested on Ubuntu Linux and Microsoft Windows host operating systems; it should seamlessly work on OS X.

Make sure you have Java 1.7+ and Ant installed. Check out (clone) the code. Run `ant` in the root of the local copy folder; `bakebread.jar` would be built. (Alternatively, open the project in IntelliJ IDEA and build it into a single artifact.)

Assuming `mini.dmp` is a minidump captured on ARM/Linux, application libraries (with symbols, ideally) are placed in `app/lib/unstripped` and device libraries are pulled from the device (or expanded from a firmware image) into `device/sysroot`, the following command

 java -jar bakebread.jar -Dc -CS ./out -CC core.dump -P app/lib/unstripped:device/sysroot -Vdh -Mea -Or mini.dmp

displays crash information (-Dc), splits the dump into individual streams and sections into `out` (-CS) and converts the minidump into `core.dump` (-CC). A "courtesy" GDB session loading script will be written to `core.dmp.dbg`. Differences between host and target binaries, if encountered, would be summarized in `core.dmp.diff`.

## Caveats

BakeBread's goal is not to enforce any specification authoritatively, but to adjust to existing diverse and imperfect implementations of multiple specifications based on incomplete and sometimes mutually contradictory information. Rather than a consumer product, it's an "ad-hoc" tool intended for conscious use by a developer roughly capable of doing a similar job, however slower, manually. In a nutshell, the tool is released in source so that you could debug it in place.

## Contributing

This project welcomes contributions and suggestions.  Most contributions require you to agree to a Contributor License Agreement (CLA) declaring that you have the right to, and actually do, grant us the rights to use your contribution. For details, visit https://cla.microsoft.com.

When you submit a pull request, a CLA-bot will automatically determine whether you need to provide a CLA and decorate the PR appropriately (e.g., label, comment). Simply follow the instructions provided by the bot. You will only need to do this once across all repos using our CLA.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.
