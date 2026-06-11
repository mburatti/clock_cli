# Recreation Plan & Maintenance Guide

This guide outlines the process to recreate, update, or adapt the `clock_cli.py` tool if the Smart Weather Clock device's firmware or web interface changes in the future.

---

## Step-by-Step Update Process

If the device webpage changes (e.g., after a firmware update), follow these steps to align the CLI tool with the new interface:

### 1. Retrieve the Updated Assets
1. Open the clock device page in your web browser.
2. Save the page source to `webpage.html`.
3. Check the `<script>` tags at the bottom of the HTML file (e.g., `<script src="javascript.js"></script>`).
4. Fetch the script file using curl:
   ```bash
   curl -s http://172.30.153.123/javascript.js -o new_javascript.js
   ```

### 2. Map DOM Inputs to API Requests
Analyze the JavaScript file (typically in the `attachEventListeners` function) to determine how user interactions are sent to the device. Look for:
*   **API Set Endpoint**: Usually formatted as `/api/set?key={key}&value={value}`.
*   **Key Names**: Note what keys are sent for specific settings (e.g. `celsius`, `time12_24`, `lcd_brightness`).
*   **Value Ranges & Types**:
    *   *Booleans*: Toggled as `'1'` (enabled) or `'0'` (disabled).
    *   *Sliders/Brightness*: Integers mapping from `2` to `99`.
    *   *Date/Time format*: Selection indices (`0`, `1`, `2`).
*   **Special Actions**:
    *   *Time Sync*: Check if timezone offset computation has changed.
    *   *Theme Rotation*: Check `/theme/toggle` and `/theme/interval` query parameters.
    *   *System Reboot*: Check if `/restart` is still the restart path.

### 3. Map Color Transformations
If the color selection mechanisms change:
*   Note how standard Hex colors (`#RRGGBB`) are packed.
*   The current device uses standard **RGB565** format for colors 1, 2, and 3:
    ```python
    ((r >> 3) << 11) | ((g >> 2) << 5) | (b >> 3)
    ```
*   Ensure the debounced components (`ws_r`, `ws_g`, `ws_b`) are still sent for primary color updates.

### 4. Inspect File Upload Form Formats
If GIF slideshow upload or generic file upload fails:
*   Check the HTML `<input type="file" ...>` parameters.
*   Check JavaScript's `FormData` keys:
    *   GIF uploads currently require a form field key of `"imageFile"` with a hardcoded filename `"4.gif"`.
    *   Generic uploads require form field key `"file"` with the original filename.
*   Modify `encode_multipart_formdata` or the arguments inside `upload_gif`/`upload_generic_file` functions in `clock_cli.py` to match the updated field names.

---

## Device API Endpoint Reference (Current Version)

The current firmware version maps parameters as follows:

| Feature / UI Element | HTTP Method | Endpoint Path & Parameters | Description |
| :--- | :--- | :--- | :--- |
| **Get Full Config** | `GET` | `/config` | Returns JSON of all device settings |
| **Get Themes List** | `GET` | `/theme/list` | Returns JSON containing theme rotation list |
| **Global Brightness** | `GET` | `/api/set?key=lcd_brightness&value={2-99}` | Adjust global LED display brightness |
| **Toggle Temp Unit** | `GET` | `/api/set?key=celsius&value={0\|1}` | Switch Celsius vs Fahrenheit |
| **Clock Format** | `GET` | `/api/set?key=time12_24&value={0\|1}` | Switch 12-Hour vs 24-Hour mode |
| **Date Format** | `GET` | `/api/set?key=timeformat&value={0\|1\|2}` | Toggle MM/DD/YYYY, DD/MM/YYYY, YYYY/MM/DD |
| **Update Weather City**| `GET` | `/api/set?key=city&value={name}` | Update weather city name string |
| **Clock Primary Color**| `GET` | `/api/set?key=color1&value={RGB565}` | Sets primary color. Also triggers `ws_r`, `ws_g`, `ws_b` parameters |
| **Night Mode Switch** | `GET` | `/api/set?key=nightmode&value={0\|1}` | Enable or disable night mode brightness limits |
| **NTP Server Update** | `GET` | `/api/set?key=ntp&value={server}` | Update time server host address |
| **Weather API Key** | `GET` | `/api/set?key=weatherkey&value={key}` | Update OpenWeatherMap API Key |
| **WiFi Credentials** | `GET` | `/api/set?key=ssid...` then `password...` | Configure new access point settings |
| **Theme Rotation Loop**| `GET` | `/theme/toggle?id={id}&state={0\|1}` | Enable/disable specific slide themes in the cycle |
| **Theme Loop Speed** | `GET` | `/theme/interval?val={seconds}` | Interval time for theme change loop |
| **GIF upload** | `POST` | `/upload` (Multipart form-data) | Form field `"imageFile"`, filename `"4.gif"` |
| **Generic upload** | `POST` | `/upload` (Multipart form-data) | Form field `"file"`, filename `{any}` |
| **System Restart** | `GET` | `/restart` | Restart the clock's ESP module |
