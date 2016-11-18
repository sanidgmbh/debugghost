# DebugGhost
Android Debugging Tool for Developers

## Getting started
The following lines will start the service:
```bash
Intent serviceIntent = new Intent(this, DebugGhostService.class);
serviceIntent.putExtra(DebugGhostService.INTENT_EXTRA_DB_NAME, MySalusDatabaseHelper.DATABASE_NAME);
serviceIntent.putExtra(DebugGhostService.INTENT_EXTRA_DB_VERSION, MySalusDatabaseHelper.DATABASE_VERSION);
startService(serviceIntent);
```
A notification will be shown, if the service is running. You can stop it every time by clicking on the notification.

## Versions
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
