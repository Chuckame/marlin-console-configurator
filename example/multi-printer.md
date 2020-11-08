# How to use marlin-console-configurator with multiple printers ?

- 1. Locate your profiles folder, here we will use `/data/printing/profiles` containing profiles for each printers.

- 2. get the wanted Marlin code with `git clone https://github.com/MarlinFirmware/Marlin.git /data/printing/Marlin`

- 3. go to root folder: `cd /data/printing`

- 4. Apply the ender3 profile with ABL : `docker run --rm -it -v ${PWD}:/app/files chuckame/marlin-console-configurator apply Marlin/Marlin -p profiles/ender3/base.yml profiles/ender3/abl.yml --save`

- 5. You will be prompted for saving the displayed configuration, just type `y` if you're okay

- 6. Build Marlin and send the firmware to the current configured printer. 

- 7. Exec `cd /data/printing/Marlin && git reset --hard` to reset Marlin to default config

- 8. Go to `3.` and target another printer config
