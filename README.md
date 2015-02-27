dex-method-counts
=================
Tool to output method counts in Android APK grouped by package.

Features
========
* Multidex apk support
* Flattening common package prefixes (`com`, `org`, `com.github`, etc.)

Usage
=====

```
    $ ./gradlew install
    $ ./build/install/dex-method-counts yourapp.apk
```

Credits
=======
Based on the [dex-method-counts](https://github.com/mihaip/dex-method-counts) by [Mihai Parparita](https://github.com/mihaip).

The DEX file parsing is based on the `dexdeps` tool from
[the Android source tree](https://android.googlesource.com/platform/dalvik.git/+/master/tools/dexdeps/).

License
=======

    Copyright (C) 2015 Jerzy Chalupski

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
