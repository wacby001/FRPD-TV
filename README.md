# DroidFRPD

An Android FRP (Fast Reverse Proxy) daemon manager that works on Android 4.x devices.

## Features

- Runs on Android 4.x (API level 19) and higher
- Integrates FRP client version 0.65
- Simple UI to start/stop the FRP service
- Background service management

## Requirements

- Android 4.4 (API level 19) or higher
- Root access (optional, for some advanced features)

## Setup

1. Download the ARM version of FRP client (frpc) version 0.65.0
2. Place it in `app/src/main/assets/frpc`
3. Update the `frpc.ini` configuration file in the app's data directory after first launch

## Building

To build the project, use Android Studio or run:

```bash
./gradlew assembleDebug
```

## Usage

1. Launch the app
2. Click "Start FRP Service" to start the FRP client
3. Click "Stop FRP Service" to stop the FRP client

## Configuration

After first launch, edit the `frpc.ini` file in the app's data directory to configure your FRP connection settings.

Example configuration:
```ini
[common]
server_addr = your-frp-server.com
server_port = 7000
token = your-token

[ssh]
type = tcp
local_ip = 127.0.0.1
local_port = 22
remote_port = 6000
```

## License

This project is licensed under the MIT License.