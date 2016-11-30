<img align="right" src="https://raw.githubusercontent.com/sanidgmbh/debugghost/master/debugghostlib/src/main/res/mipmap-xxxhdpi/ic_ghost.png" />
# DebugGhost
Android Debugging Tool for Developers

## What's this for?
The main goal is to loosely couple a development tool to your project without the need of a lot of if-else conditions, adding "debug-menu-items" to your action bar to run some test code and so on. In my case, the biggest need was getting my current sqlite database and having a look into it. That's where DebugGhost startet. DebugGhost runs a lightweight server in the context of your app. You can connect to it via WebBrowser from your development maschine and browse the sqlite database and also download it.

DebugGhost does **not** use the adb. It uses WiFi, so your development maschine has to be in the same local network and devices have to be able to find each other (that's not a givin in guest Wifis).

*more features are planned, but one step after another ;)*

## Getting started
The following lines will start the service:
```java
Intent serviceIntent = new Intent(this, DebugGhostService.class);
serviceIntent.putExtra(DebugGhostService.INTENT_EXTRA_DB_NAME, MyDB.DATABASE_NAME);
serviceIntent.putExtra(DebugGhostService.INTENT_EXTRA_DB_VERSION, MyDB.DATABASE_VERSION);
startService(serviceIntent);
```
A notification will be shown, if the service is running. You can stop it any time by clicking on the notification.

## Not for production
DebugGhost is for developers and not for production purpose. Be aware of, that this project has **no focus on security or performance**. It's meant to be used while developing your app.

## How to set up your app project to compile DebugGhostLib only in a development-flavor
It is possible to compile DebugGhostLib only in a special flavor, which (as mentioned above) should not be the production flavor. Unfortunately, you have to setup product flavors. Android build process currently doesn't support this feature using buildTypes (like release or debug).

### Add module to your project
* Checkout/download DebugGhost.
* in AndroidStudio: File > New > Import Module...
* select the location of your DebugGhost download
* name the module :debugghostlib (or whatever you like, just make sure the name matches in the dependencies section later)
* done

### Setup product flavors in build.gradle of your app
```gradle
productFlavors {
        production {
            ...
        }
        dev {
        }
}
```

### Configure build.gradle of your app to only compile DebugGhostLib for dev-flavor

```gradle
dependencies {
  ...
  // only compile for dev-flavor
  devCompile project(path: ':debugghostlib')
}
```

### Add a java class acting as 'bridge'
Add a java class (I call mine DebugGhostBrdige.java and usually put it in the package .debugghost).
You need to have this class in *every flavor* in the same package except for the **main** folder.

```java
public class DebugGhostBridge {

    private static final String LOG_TAG = DebugGhostBridge.class.getSimpleName();

    public static void runDebugGhost(Activity activity) {

      Log.d(LOG_TAG, "Starting DebugGhostService");

      Intent serviceIntent = new Intent(activity, DebugGhostService.class);
      serviceIntent.putExtra(DebugGhostService.INTENT_EXTRA_DB_NAME, MyDB.DATABASE_NAME);
      serviceIntent.putExtra(DebugGhostService.INTENT_EXTRA_DB_VERSION, MyDB.DATABASE_VERSION);
      activity.startService(serviceIntent);
    }

    public static void stopDebugGhost(Activity activity) {

      Log.d(LOG_TAG, "Stopping DebugGhostService");

      Intent serviceIntent = new Intent(activity, DebugGhostService.class);
      serviceIntent.putExtra(DebugGhostService.INTENT_EXTRA_DB_NAME, MyDB.DATABASE_NAME);
      serviceIntent.putExtra(DebugGhostService.INTENT_EXTRA_DB_VERSION, MyDB.DATABASE_VERSION);
      activity.stopService(serviceIntent);
    }
}
```

The bridge class will look like this in all flavors where you don't want DebugGhostLib to run:
```java
public class DebugGhostBridge {

    private static final String LOG_TAG = DebugGhostBridge.class.getSimpleName();

    public static void runDebugGhost(Activity activity) {
    }

    public static void stopDebugGhost(Activity activity) {
    }
}
```
Now you just add two lines to your MainActivity (or whatever activity where you want to start the service).
```java
public class MainActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_home);

      DebugGhostBridge.runDebugGhost(this);
  }
  
  @Override
  protected void onDestroy() {
      DebugGhostBridge.stopDebugGhost(this);
      super.onDestroy();
  }
```

## Versions
### v0.1.1b
* You can now download your sqlite database through the webbrowser
* You can start/stop the server via notification

### v0.1b
Currently, DebugGhost only supports browsing through your projects sqlite database through a webbrowser on a machine in the same network.
*At this state, this was only tested using a Mac (Sierra) with Chrome.*

## SQLite-Browser
Starting the service will open a ServerSocket on your android device and lets you connect to the device in **the same local network**.
The IP address will be shown in the log.
Connect with your browser on port 8080.
```bash
http://<my-ip>:8080
```
## License

This project is licensed under the Apache 2 License - see the [LICENSE](LICENSE) file for details
