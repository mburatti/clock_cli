#!/usr/bin/env python3
"""
Smart Weather Clock CLI Tool
Acts as a browser automation to control settings for the clock device.
Works using standard library Python to avoid external dependencies.
"""

import argparse
import json
import os
import sys
import time
import urllib.request
import urllib.parse
from datetime import datetime

# Default clock IP
DEFAULT_IP = "172.30.153.123"

def hex_to_rgb565(hex_str):
    """Converts hex color string (#RRGGBB or RRGGBB) to RGB565 decimal value."""
    hex_str = hex_str.lstrip('#')
    if len(hex_str) != 6:
        raise ValueError("Hex color must be exactly 6 characters long (e.g. FF0000).")
    r = int(hex_str[0:2], 16)
    g = int(hex_str[2:4], 16)
    b = int(hex_str[4:6], 16)
    return ((r >> 3) << 11) | ((g >> 2) << 5) | (b >> 3)

def rgb565_to_hex(val):
    """Converts RGB565 decimal value to #RRGGBB hex color."""
    if not val:
        return "#000000"
    val = int(val)
    r = (val >> 11) & 0x1F
    g = (val >> 5) & 0x3F
    b = val & 0x1F
    
    r_dec = int(r * 255 / 31)
    g_dec = int(g * 255 / 63)
    b_dec = int(b * 255 / 31)
    
    return f"#{r_dec:02x}{g_dec:02x}{b_dec:02x}"

def encode_multipart_formdata(fields, files):
    """Encodes fields and files into multipart/form-data format for urllib."""
    boundary = b'----WebKitFormBoundary' + str(int(time.time() * 1000)).encode('ascii')
    body = bytearray()
    
    for key, value in fields.items():
        body.extend(b'--' + boundary + b'\r\n')
        body.extend(f'Content-Disposition: form-data; name="{key}"\r\n\r\n'.encode('utf-8'))
        body.extend(str(value).encode('utf-8'))
        body.extend(b'\r\n')
        
    for key, (filename, file_content, content_type) in files.items():
        body.extend(b'--' + boundary + b'\r\n')
        body.extend(f'Content-Disposition: form-data; name="{key}"; filename="{filename}"\r\n'.encode('utf-8'))
        body.extend(f'Content-Type: {content_type}\r\n\r\n'.encode('utf-8'))
        body.extend(file_content)
        body.extend(b'\r\n')
        
    body.extend(b'--' + boundary + b'--\r\n')
    content_type = f'multipart/form-data; boundary={boundary.decode("ascii")}'
    return body, content_type

class ClockDevice:
    def __init__(self, ip):
        self.ip = ip
        self.base_url = f"http://{ip}"
        self.headers = {
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36'
        }

    def simulate_browse(self):
        """Simulates opening the main webpage in browser."""
        url = f"{self.base_url}/"
        print(f"[*] Simulating page view: {url}")
        req = urllib.request.Request(url, headers=self.headers)
        try:
            with urllib.request.urlopen(req, timeout=5) as response:
                response.read()
                print("[+] Page loaded successfully (HTTP 200).")
                return True
        except Exception as e:
            print(f"[-] Failed to load main webpage: {e}")
            return False

    def send_config(self, key, value):
        """Sends a config key-value update to the device API."""
        encoded_val = urllib.parse.quote(str(value))
        url = f"{self.base_url}/api/set?key={key}&value={encoded_val}"
        print(f"[*] Sending parameter update: {key} = {value}...")
        req = urllib.request.Request(url, headers=self.headers)
        try:
            with urllib.request.urlopen(req, timeout=5) as response:
                content = response.read().decode('utf-8', errors='ignore')
                print(f"[+] Success. Response: {content.strip()}")
                return True
        except Exception as e:
            print(f"[-] Error updating {key}: {e}")
            return False

    def get_config(self):
        """Fetches the current configuration JSON from the device."""
        url = f"{self.base_url}/config"
        print(f"[*] Retrieving configuration from: {url}")
        req = urllib.request.Request(url, headers=self.headers)
        try:
            with urllib.request.urlopen(req, timeout=5) as response:
                data = json.loads(response.read().decode('utf-8'))
                return data
        except Exception as e:
            print(f"[-] Error reading configuration: {e}")
            return None

    def get_themes(self):
        """Fetches the theme list configurations from the device."""
        url = f"{self.base_url}/theme/list"
        print(f"[*] Retrieving theme list from: {url}")
        req = urllib.request.Request(url, headers=self.headers)
        try:
            with urllib.request.urlopen(req, timeout=5) as response:
                data = json.loads(response.read().decode('utf-8'))
                return data
        except Exception as e:
            print(f"[-] Error reading theme list: {e}")
            return None

    def toggle_theme(self, theme_id, state):
        """Toggles enabling of specific theme in rotation list."""
        url = f"{self.base_url}/theme/toggle?id={theme_id}&state={state}"
        print(f"[*] Toggling theme id {theme_id} to state {state}...")
        req = urllib.request.Request(url, headers=self.headers)
        try:
            with urllib.request.urlopen(req, timeout=5) as response:
                print(f"[+] Theme toggled successfully.")
                return True
        except Exception as e:
            print(f"[-] Error toggling theme: {e}")
            return False

    def set_theme_interval(self, seconds):
        """Sets the theme interval in seconds."""
        url = f"{self.base_url}/theme/interval?val={seconds}"
        print(f"[*] Updating theme interval to {seconds} seconds...")
        req = urllib.request.Request(url, headers=self.headers)
        try:
            with urllib.request.urlopen(req, timeout=5) as response:
                print(f"[+] Interval updated successfully.")
                return True
        except Exception as e:
            print(f"[-] Error updating interval: {e}")
            return False

    def restart(self):
        """Sends a restart command to the device."""
        url = f"{self.base_url}/restart"
        print("[*] Sending system restart signal...")
        req = urllib.request.Request(url, headers=self.headers)
        try:
            with urllib.request.urlopen(req, timeout=5) as response:
                print("[+] Restart command sent.")
                return True
        except Exception as e:
            # Reconnection or socket closure is expected during reboot
            print(f"[+] Restart sent (Connection closed or returned: {e})")
            return True

    def upload_generic_file(self, file_path):
        """Uploads a generic file to the root directory on the device."""
        if not os.path.exists(file_path):
            print(f"[-] File not found: {file_path}")
            return False
            
        filename = os.path.basename(file_path)
        with open(file_path, "rb") as f:
            file_content = f.read()

        url = f"{self.base_url}/upload"
        print(f"[*] Uploading file '{filename}' ({len(file_content)} bytes)...")
        
        fields = {}
        files = {
            "file": (filename, file_content, "application/octet-stream")
        }
        
        body, content_type = encode_multipart_formdata(fields, files)
        
        headers = self.headers.copy()
        headers['Content-Type'] = content_type
        
        req = urllib.request.Request(url, data=body, headers=headers, method="POST")
        try:
            with urllib.request.urlopen(req, timeout=30) as response:
                print(f"[+] Upload successful (HTTP {response.status}).")
                return True
        except Exception as e:
            print(f"[-] Upload failed: {e}")
            return False

    def upload_gif(self, file_path):
        """Uploads an 80x80 GIF image to the device."""
        if not os.path.exists(file_path):
            print(f"[-] File not found: {file_path}")
            return False

        # Verify GIF signature
        with open(file_path, "rb") as f:
            header = f.read(6)
            if header not in (b"GIF87a", b"GIF89a"):
                print("[-] Validation Error: Only GIF files are allowed.")
                return False
            
            # Simple dimension parsing for GIF
            # Logical Screen Width is at offset 6, height at offset 8 (uint16 little endian)
            f.seek(6)
            width = int.from_bytes(f.read(2), "little")
            height = int.from_bytes(f.read(2), "little")
            if width != 80 or height != 80:
                print(f"[-] Size Error: GIF size is {width}x{height}. SD PRO requires 80x80 pixels.")
                return False
                
            f.seek(0)
            file_content = f.read()

        url = f"{self.base_url}/upload"
        print(f"[*] Uploading GIF slideshow file '4.gif' ({len(file_content)} bytes)...")
        
        fields = {}
        files = {
            "imageFile": ("4.gif", file_content, "image/gif")
        }
        
        body, content_type = encode_multipart_formdata(fields, files)
        
        headers = self.headers.copy()
        headers['Content-Type'] = content_type
        
        req = urllib.request.Request(url, data=body, headers=headers, method="POST")
        try:
            with urllib.request.urlopen(req, timeout=30) as response:
                print(f"[+] GIF uploaded successfully.")
                return True
        except Exception as e:
            print(f"[-] GIF upload failed: {e}")
            return False

def print_config(config):
    """Pretty prints the configuration dictionary."""
    if not config:
        return
    print("\n================ Smart Weather Clock Configuration ================")
    print(f"City:             {config.get('city', 'Unknown')}")
    print(f"Global Brightness:{config.get('brightness', 'Unknown')}")
    print(f"Temperature Unit: {'Celsius' if config.get('celsius') == 1 else 'Fahrenheit'}")
    print(f"Clock Mode:       {'12-Hour' if config.get('hour12') == 1 else '24-Hour'}")
    print(f"Mile Display:     {'Enabled' if config.get('mile') == 1 else 'Disabled'}")
    print(f"Time Format:      {config.get('timeformat', 'Unknown')} (0: MM/DD/YYYY, 1: DD/MM/YYYY, 2: YYYY/MM/DD)")
    print(f"Sync Time:        {'Enabled' if config.get('sync') == 1 else 'Disabled'}")
    print(f"GIF ID Selection: {config.get('gifnum', 'Unknown')}")
    print(f"Theme Index:      {config.get('theme', 'Unknown')}")
    print(f"NTP Server:       {config.get('ntp', 'Unknown')}")
    print(f"OpenWeather Key:  {config.get('weatherkey', 'Unknown')}")
    
    print("\n--- Night Mode Settings ---")
    print(f"Night Mode:       {'Enabled' if config.get('nightmode') == 1 else 'Disabled'}")
    print(f"Start Time:       {config.get('starttime', 'Unknown')}:00")
    print(f"Stop Time:        {config.get('stoptime', 'Unknown')}:00")
    print(f"Night Brightness: {config.get('nightbrightness', 'Unknown')}")
    
    print("\n--- Network ---")
    print(f"WiFi SSID:        {config.get('ssid', 'Unknown')}")
    print(f"WiFi Password:    {config.get('password', 'Unknown')}")
    
    print("\n--- Colors ---")
    print(f"Clock Color 1:    {rgb565_to_hex(config.get('color1'))} (Raw: {config.get('color1')})")
    print(f"Clock Color 2:    {rgb565_to_hex(config.get('color2'))} (Raw: {config.get('color2')})")
    print(f"Clock Color 3:    {rgb565_to_hex(config.get('color3'))} (Raw: {config.get('color3')})")
    
    if 'freespace' in config:
        free_mb = config['freespace'] / (1024 * 1024)
        print(f"\nFree Storage:     {free_mb:.2f} MB")
    print("===================================================================\n")

def print_themes(theme_data):
    """Pretty prints the themes rotation configuration."""
    if not theme_data:
        return
    print("\n================== Theme Rotation Configurations ==================")
    interval = theme_data.get('interval', 0)
    print(f"Theme Interval:   {f'{interval} Seconds' if interval > 0 else 'Disabled (Static)'}")
    print("\nRotation List:")
    for theme in theme_data.get('themes', []):
        status = "[x] Enabled " if theme.get('enabled') else "[ ] Disabled"
        print(f"  {status} | ID: {theme.get('id'):<2} | Theme: {theme.get('name')}")
    print("===================================================================\n")

def main():
    parser = argparse.ArgumentParser(
        description="CLI tool to control and configure parameters of the Smart Weather Clock device.",
        formatter_class=argparse.RawTextHelpFormatter,
        epilog="""Examples:
  # Query current device configuration:
  python3 clock_cli.py --get-config

  # Set display brightness to 75 and city to "Paris":
  python3 clock_cli.py --brightness 75 --city "Paris"

  # Update color 1 (and trigger ws_r/g/b main color update):
  python3 clock_cli.py --color1 "#FF5733"

  # Configure and enable night mode:
  python3 clock_cli.py --nightmode 1 --starttime 22 --stoptime 7 --nightbrightness 5

  # Upload a custom 80x80 GIF to slideshow:
  python3 clock_cli.py --upload-gif path/to/my_clock.gif
"""
    )
    
    # Global Device Settings
    parser.add_argument('--ip', type=str, default=DEFAULT_IP,
                        help=f"IP address of the clock device (default: {DEFAULT_IP})")
    parser.add_argument('--get-config', action='store_true',
                        help="Retrieve and display current configuration settings from the device.")
    parser.add_argument('--get-themes', action='store_true',
                        help="Retrieve and display theme rotation configuration.")

    # Parameters from Display Settings
    parser.add_argument('--celsius', type=int, choices=[0, 1],
                        help="Celsius display: 1 = Show Temp in Celsius, 0 = Fahrenheit")
    parser.add_argument('--hour12', type=int, choices=[0, 1],
                        help="12-Hour Clock format: 1 = 12-Hour, 0 = 24-Hour")
    parser.add_argument('--mile', type=int, choices=[0, 1],
                        help="Mile Display: 1 = Miles, 0 = Metric")
    parser.add_argument('--sync-time', action='store_true',
                        help="Sync local computer/browser timezone offset and status with the clock.")
    parser.add_argument('--time-format', type=int, choices=[0, 1, 2],
                        help="Time/Date display format:\n0 = MM/DD/YYYY\n1 = DD/MM/YYYY\n2 = YYYY/MM/DD")
    parser.add_argument('--city', type=str,
                        help="Update weather city name.")
    parser.add_argument('--gif-list', type=int, choices=[1, 2, 3, 4, 5],
                        help="Select preloaded slideshow app: 1=spaceman, 2=divergent, 3=bird, 4=wave, 5=Customization")

    # Brightness settings
    parser.add_argument('--brightness', type=int,
                        help="Set global screen brightness level (integer 2 to 99).")

    # Colors
    parser.add_argument('--color1', type=str,
                        help="Set Primary Clock Color (hex format, e.g. '#FF0000' or 'FF0000').")
    parser.add_argument('--color2', type=str,
                        help="Set Secondary Clock Color (hex format).")
    parser.add_argument('--color3', type=str,
                        help="Set Clock Seconds Color (hex format).")

    # Night Mode settings
    parser.add_argument('--nightmode', type=int, choices=[0, 1],
                        help="Enable night mode: 1 = Enabled, 0 = Disabled")
    parser.add_argument('--starttime', type=int, choices=range(18, 24),
                        help="Set Night Mode start hour (18-23)")
    parser.add_argument('--stoptime', type=int, choices=range(5, 13),
                        help="Set Night Mode stop hour (5-12)")
    parser.add_argument('--nightbrightness', type=int,
                        help="Set screen brightness during night mode (integer 2 to 99).")

    # WiFi settings
    parser.add_argument('--wifi-ssid', type=str,
                        help="WiFi Network SSID (requires --wifi-pass as well).")
    parser.add_argument('--wifi-pass', type=str,
                        help="WiFi Network Password.")

    # System Keys settings
    parser.add_argument('--ntp', type=str,
                        help="Update NTP Server address.")
    parser.add_argument('--weather-key', type=str,
                        help="Update OpenWeatherMap API Key.")

    # Theme loops settings
    parser.add_argument('--theme-interval', type=int,
                        help="Update theme rotation interval in seconds (0 to disable).")
    parser.add_argument('--toggle-theme', nargs=2, metavar=('THEME_ID', 'STATE'),
                        help="Enable/disable a theme index in rotation. State: 1 = Enabled, 0 = Disabled.")

    # System Actions
    parser.add_argument('--restart', action='store_true',
                        help="Request system restart (reboot ESP).")
    parser.add_argument('--upload-gif', type=str, metavar='FILE_PATH',
                        help="Upload an 80x80 GIF image to the slideshow customization album.")
    parser.add_argument('--upload-file', type=str, metavar='FILE_PATH',
                        help="Upload a generic configuration or system file to device root.")

    args = parser.parse_args()

    # Determine if any settings commands were passed
    has_action = any([
        args.get_config, args.get_themes, args.celsius is not None, args.hour12 is not None,
        args.mile is not None, args.sync_time, args.time_format is not None, args.city is not None,
        args.gif_list is not None, args.brightness is not None, args.color1 is not None,
        args.color2 is not None, args.color3 is not None, args.nightmode is not None,
        args.starttime is not None, args.stoptime is not None, args.nightbrightness is not None,
        args.wifi_ssid is not None, args.wifi_pass is not None, args.ntp is not None,
        args.weather_key is not None, args.theme_interval is not None, args.toggle_theme is not None,
        args.restart, args.upload_gif is not None, args.upload_file is not None
    ])

    if not has_action:
        parser.print_help()
        sys.exit(0)

    # Instantiate clock controller
    clock = ClockDevice(args.ip)

    # 1. Simulate opening browser homepage
    if not clock.simulate_browse():
        sys.exit(1)

    # 2. Query configurations first if requested
    if args.get_config:
        config = clock.get_config()
        print_config(config)
        
    if args.get_themes:
        themes = clock.get_themes()
        print_themes(themes)

    # 3. Handle specific config parameters updates
    if args.celsius is not None:
        clock.send_config('celsius', '1' if args.celsius else '0')
        
    if args.hour12 is not None:
        clock.send_config('time12_24', '1' if args.hour12 else '0')
        
    if args.mile is not None:
        clock.send_config('mile', '1' if args.mile else '0')
        
    if args.sync_time:
        # Get local timezone offset in hours
        tz_offset = -time.timezone / 3600.0
        if time.daylight:
            tz_offset = -time.altzone / 3600.0
        clock.send_config('timesync', '1')
        time.sleep(0.2)
        clock.send_config('zone', int(tz_offset))

    if args.time_format is not None:
        clock.send_config('timeformat', args.time_format)

    if args.city is not None:
        clock.send_config('city', args.city)

    if args.gif_list is not None:
        clock.send_config('gifnum', args.gif_list)

    if args.brightness is not None:
        val = max(2, min(99, args.brightness))
        clock.send_config('lcd_brightness', val)

    # Color Pickers
    if args.color1 is not None:
        try:
            rgb_val = hex_to_rgb565(args.color1)
            clock.send_config('color1', rgb_val)
            # Send main color components (ws_r, ws_g, ws_b)
            hex_clean = args.color1.lstrip('#')
            r = int(hex_clean[0:2], 16)
            g = int(hex_clean[2:4], 16)
            b = int(hex_clean[4:6], 16)
            time.sleep(0.2)
            clock.send_config('ws_r', r)
            time.sleep(0.05)
            clock.send_config('ws_g', g)
            time.sleep(0.05)
            clock.send_config('ws_b', b)
        except ValueError as e:
            print(f"[-] Color 1 error: {e}")

    if args.color2 is not None:
        try:
            rgb_val = hex_to_rgb565(args.color2)
            clock.send_config('color2', rgb_val)
        except ValueError as e:
            print(f"[-] Color 2 error: {e}")

    if args.color3 is not None:
        try:
            rgb_val = hex_to_rgb565(args.color3)
            clock.send_config('color3', rgb_val)
        except ValueError as e:
            print(f"[-] Color 3 error: {e}")

    # Night Mode
    if args.nightmode is not None:
        clock.send_config('nightmode', '1' if args.nightmode else '0')

    if args.starttime is not None:
        clock.send_config('starttime', args.starttime)

    if args.stoptime is not None:
        clock.send_config('stoptime', args.stoptime)

    if args.nightbrightness is not None:
        val = max(2, min(99, args.nightbrightness))
        clock.send_config('nightbrightness', val)

    # Keys (NTP/Weather)
    if args.ntp is not None:
        clock.send_config('ntp', args.ntp)

    if args.weather_key is not None:
        clock.send_config('weatherkey', args.weather_key)

    # WiFi Connection Setup
    if args.wifi_ssid is not None:
        if args.wifi_pass is None:
            print("[-] Error: Connecting to WiFi requires both --wifi-ssid and --wifi-pass.")
        else:
            clock.send_config('ssid', args.wifi_ssid)
            time.sleep(0.5)
            clock.send_config('password', args.wifi_pass)

    # Theme Interval & Toggle
    if args.theme_interval is not None:
        clock.set_theme_interval(args.theme_interval)

    if args.toggle_theme is not None:
        try:
            theme_id = int(args.toggle_theme[0])
            state = int(args.toggle_theme[1])
            if state not in (0, 1):
                raise ValueError("State must be 0 or 1")
            clock.toggle_theme(theme_id, state)
        except ValueError as e:
            print(f"[-] Toggle theme error: {e}")

    # Binary and generic uploads
    if args.upload_gif is not None:
        clock.upload_gif(args.upload_gif)

    if args.upload_file is not None:
        clock.upload_generic_file(args.upload_file)

    # Restart Command
    if args.restart:
        clock.restart()

if __name__ == '__main__':
    main()
