# FRPD-TV

An Android FRP (Fast Reverse Proxy) daemon manager optimized for Android TV and older Android devices (4.x and above).

## Features

- Runs on Android 4.x (API level 19) and higher, including Android TV
- Integrates FRP client version 0.65
- Simple UI to start/stop the FRP service
- Background service management
- Auto-start on boot functionality
- Import TOML configuration files
- Optimized UI for TV screens with remote control navigation
- Dedicated layouts for both phones/tablets and TVs

## Requirements

- Android 4.4 (API level 19) or higher
- Root access (optional, for some advanced features)
- For Android TV: Recommended minimum screen size of 600dp

## Setup

1. Download the ARM version of FRP client (frpc) version 0.65.0
2. Place it in `app/src/main/assets/frpc`
3. Optionally, download the FRP server (frps) and place it in `app/src/main/assets/frps`
4. Update the configuration file after first launch

## Building

To build the project, use Android Studio or run:

```bash
./gradlew assembleDebug
```

## Usage

1. Launch the app
2. Select FRP mode (Client or Server)
3. Import configuration file (TOML format) using the "Import Configuration File" button
4. Click "Start FRP Service" to start the FRP client/server
5. Click "Stop FRP Service" to stop the FRP client/server
6. Use "Auto-start" toggle to enable/disable auto-start on boot

## Configuration

After first launch, you can import TOML configuration files. Example configurations:

Client configuration (frpc.toml):
```toml
[common]
server_addr = "your-frp-server.com"
server_port = 7000
token = "your-token"

[ssh]
type = "tcp"
local_ip = "127.0.0.1"
local_port = 22
remote_port = 6000
```

Server configuration (frps.toml):
```toml
[common]
bind_port = 7000
token = "your-token"
```

## Android TV Support

This application is optimized for Android TV with:
- Leanback launcher support
- Dedicated TV layout with larger controls
- Remote control navigation support
- Proper banner for TV app drawer

## License

This project is licensed under the MIT License.