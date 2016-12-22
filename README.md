<img align="right" src="https://raw.githubusercontent.com/sanidgmbh/debugghost/master/debugghostlib/src/main/res/mipmap-xxxhdpi/ic_ghost.png" />
# DebugGhost
Android Developer Tool

## What's this for?
The main goal is to loosely couple a development tool to your project (only in debug build-type) without the need of a lot of if-else conditions, adding "debug-menu-items" to your action bar to run some test code and so on. In my case, the biggest need was getting my current sqlite database and having a look into it. That's where DebugGhost startet. DebugGhost runs a lightweight server in the context of your app. You can connect to it via WebBrowser from your development maschine and browse the sqlite database and also download it.
DebugGhost does **not** use the adb. It uses WiFi, so your development maschine has to be in the same local network and devices have to be able to find each other (that's not a givin in guest Wifis).

Current function:
* browse and download through your projects SQLite database
* view device infos
* send commands to your device

*more features are planned, but one step after another ;)*

## Getting started
Follow these steps to get DebugGhost up and running in your project:
* [include the lib in your project](../../wiki/#include-the-lib-in-your-project) (only for debug build-type)
    * [as lib with jitpack.io](../../wiki/#add-lib-with-jitpack.io) (no maven repo yet, sry)
    * [as module in your project](../../wiki/#add-lib-as-module-in-your-project)
* [setup Application class](../../wiki/#setup-application-class) 
    * [include DebugGhostBridge](../../wiki/#include-Debugghostbridge)


## Not for production
DebugGhost is for developers and not for production purpose. Be aware of, that this project has **no focus on security or performance**. It's meant to be used while developing your app.

## Versions

### v1.0b (7d0ac8d - for jitpack.io)
* fixed HTTP protocol, now working in diverse browsers
* showing app icon
* added bootstrap UI for HTML pages
* added device info
* added commands

### v0.1.1b
* You can now download your sqlite database through the webbrowser
* You can start/stop the server via notification

### v0.1b
Currently, DebugGhost only supports browsing through your projects sqlite database through a webbrowser on a machine in the same network.
*At this state, this was only tested using a Mac (Sierra) with Chrome.*

## License

This project is licensed under the Apache 2 License - see the [LICENSE](LICENSE) file for details
