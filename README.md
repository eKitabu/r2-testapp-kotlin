# Readium-2 Test App (Kotlin/Android) <a href='https://play.google.com/store/apps/details?id=org.readium.r2reader'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/badge_new.png'/></a>

A test app for the Kotlin implementation of Readium-2. Stable builds are [available on Google Play](https://play.google.com/store/apps/details?id=org.readium.r2reader). To follow the development of this app, [join the beta channel](https://play.google.com/apps/testing/org.readium.r2reader).

[![BSD-3](https://img.shields.io/badge/License-BSD--3-brightgreen.svg)](https://opensource.org/licenses/BSD-3-Clause)

## Features

- [x] EPUB 2.x and 3.x support
- [x] Readium LCP support
- [x] CBZ support
- [x] Custom styles
- [x] Night & sepia modes
- [x] Pagination and scrolling
- [x] Table of contents
- [x] OPDS 1.x and 2.0 support
- [ ] FXL support
- [ ] RTL support

## Demo


## Dependencies

- [Shared Models](https://github.com/readium/r2-shared-kotlin) (Model, shared for both streamer and navigator) [![Release](https://jitpack.io/v/readium/r2-shared-kotlin.svg)](https://jitpack.io/#readium/r2-shared-kotlin)
- [Streamer](https://github.com/readium/r2-streamer-kotlin) (The parser/server) [![Release](https://jitpack.io/v/readium/r2-streamer-kotlin.svg)](https://jitpack.io/#readium/r2-streamer-kotlin) 
- [Navigator](https://github.com/readium/r2-navigator-kotlin) (The bare ViewControllers for displaying parsed resources) [![Release](https://jitpack.io/v/readium/r2-navigator-kotlin.svg)](https://jitpack.io/#readium/r2-navigator-kotlin)
- [Readium CSS](https://github.com/readium/readium-css) (Handles styles, layout and user settings)
- [Readium LCP](https://github.com/readium/r2-lcp-kotlin) 


## Install and run the testapp

git clone https://github.com/readium/r2-testapp-kotlin.git



### Add Readium LCP support

The support of Readium LCP in the R2 Reader Android test-app is still prototypal. Activation of Readium LCP support in the codebase is therefore still highly manual. LCP support will be the default situation when the test-app is Readium LCP Certified by EDRLab (which is still not the case, they are still some issues to solve).  

To activate Readium LCP support in the Android test-app you’ll have to:

Send a request to EDRLab (contact at edrlab.org) for a pre-compiled test library named “liblcp.aar”. As this lib will have to be replaced with a production grade pre-compiled library when implementers have signed the Readium LCP Terms of Use, we think it is easier to provide a precompiled lib rather than source code. It will also be a proper way to get in touch with the managers of the Readium LCP network.

1. Add Module “liblcp.aar”
....

2. Add Module "r2-lcp"
....




