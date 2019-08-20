# Android-LcdParamConfig
Configure lcd parameters with u-disk or sdcard card (based on rk3288 android 8.1 sdk). 

Run the lcdparamservice program in the background, it will listen to the u-disk and sdcard insertion, parse the screen file (lcd_parameters) in the u-disk or sdcard, and then write the parameters to the lcdparam partition. The next time uboot is started, these parameters will be taken out and updated to the dtb partition.

- Support lvds, edp, mipi screen.
- Support panel timing parameter configuration.
- Support mipi initialization sequence configuration.
- Support screen rotation configuration.
- Support screen density configuration.

## Integration
1. Copy lcdparamservice/ to system/core/ directory.
2. Refer to u-boot/drivers/video/rockchip_display.c to modify the related file.
3. Increase lcdparam partition.
```
CMDLINE: console=ttyFIQ0 androidboot.baseband=N/A androidboot.selinux=permissive androidboot.hardware=rk30board androidboot.console=ttyFIQ0 init=/init initrd=0x62000000,0x00800000 mtdparts=rk29xxnand:0x00002000@0x0000200 (uboot),0x00002000@0x00004000(trust),0x00002000@0x00006000(misc),0x00008000@0x0000800 (resource),0x00010000@0x00010000(kernel),0x00010000@0x00020000(boot),0x00020000@0x0003000 (recovery),0x00038000@0x00050000(backup),0x00002000@0x00088000(security),0x00100000@0x0008a00 (cache),0x00400000@0x0018a000(system),0x00008000@0x0058a000(metadata),0x00080000@0x0059200 (vendor),0x00080000@0x00612000(oem),0x00000400@0x00692000(frp),0x000004000@0x00692400(lcdparam),-@0x0069640 (userdata)
```
4. Modify the following file:  
 
```diff
diff --git a/device/rockchip/common/ueventd.rockchip.rc b/device/rockchip/common/ueventd.rockchip.rc
index 26c2395..0780221 100755
--- a/device/rockchip/common/ueventd.rockchip.rc
+++ b/device/rockchip/common/ueventd.rockchip.rc
@@ -366,3 +366,6 @@
 /dev/i2c-1 0660 system system
 #for ovr
 /dev/ovr0                0664   system          system
+
+# for lcdparam read/write
+/dev/block/mmcblk1p16 0666 system system
diff --git a/device/rockchip/common/device.mk b/device/rockchip/common/device.mk
index fe49f56..55fb8f0 100755
--- a/device/rockchip/common/device.mk
+++ b/device/rockchip/common/device.mk
@@ -150,6 +150,10 @@ ifeq ($(strip $(TARGET_BOARD_PLATFORM_PRODUCT)), box)
       pppoe-service
 endif
 
+# Update lcd parameters from sdcard
+PRODUCT_PACKAGES += \
+       lcdparamservice
+
 ifneq ($(filter atv box, $(strip $(TARGET_BOARD_PLATFORM_PRODUCT))), )
     PRODUCT_COPY_FILES += \
       $(LOCAL_PATH)/resolution_white.xml:/system/usr/share/resolution_white.xml
diff --git a/device/rockchip/common/init.rk30board.rc b/device/rockchip/common/init.rk30board.rc
index b7ae3e1..58eea6d 100755
--- a/device/rockchip/common/init.rk30board.rc
+++ b/device/rockchip/common/init.rk30board.rc
@@ -313,6 +313,10 @@ service shutdownanim /system/bin/bootanimation shutdown
     disabled
     oneshot
 
+#for update lcd parameters
+service lcdparamservice /system/bin/lcdparamservice
+    class main
+
 #for bd        
 service iso_operate /vendor/bin/iso
     class main
diff --git a/device/rockchip/common/sepolicy/file_contexts b/device/rockchip/common/sepolicy/file_contexts
index 3f5c043..7133a28 100755
--- a/device/rockchip/common/sepolicy/file_contexts
+++ b/device/rockchip/common/sepolicy/file_contexts
@@ -49,6 +49,9 @@
 /system/bin/mkntfs   u:object_r:vold_exec:s0
 /system/bin/ntfsfix  u:object_r:vold_exec:s0
 
+# for update lcd parameters
+/system/bin/lcdparamservice            u:object_r:lcdparamservice_exec:s0
+
 #hdmi
 /sys/devices/virtual/display/HDMI(/.*)? -- u:object_r:sysfs_hdmi:s0
 
diff --git a/device/rockchip/common/sepolicy/init.te b/device/rockchip/common/sepolicy/init.te
index fc28bb1..16d79c4 100755
--- a/device/rockchip/common/sepolicy/init.te
+++ b/device/rockchip/common/sepolicy/init.te
@@ -5,6 +5,7 @@
 #')
 
 domain_trans(init, rk_store_keybox_exec, rk_store_keybox)
+domain_trans(init, lcdparamservice_exec, lcdparamservice)
 
 allow init serial_device:chr_file { write ioctl };
 allow init kernel:system { module_request };
diff --git a/device/rockchip/common/sepolicy/lcdparamservice.te b/device/rockchip/common/sepolicy/lcdparamservice.
new file mode 100755
index 0000000..6e72fd1
--- /dev/null
+++ b/device/rockchip/common/sepolicy/lcdparamservice.te
@@ -0,0 +1,4 @@
+type lcdparamservice, domain, coredomain, mlstrustedsubject;
+type lcdparamservice_exec, exec_type, vendor_file_type, file_type;
+
+init_daemon_domain(lcdparamservice)
```

## lcd_parameters
```
# ---------------------------
# Description:
# The first line of the "#" is a comment, the comment is just to explain how to use it.
# Does not have any other features, can be ignored.
#
# Configuration format is as follows:
#
# Name = Value;
#
# Note: The end of the line must end with ";"
# ---------------------------


# ---------------------------
# General parameters
# ---------------------------
# Screen rotation angle, 0 | 90 | 180 | 270
orientation = 0;

# Screen density, 120 | 160 | 240 | 320
density = 120;


# ---------------------------
# Lcd interface type
# 0: mipi | 1: eDP | 2: lvds
# ---------------------------
panel-type = 2;


# ---------------------------
# Whether to initialize the screen in uboot
# 0: Initialize in uboot | 1: Initialize only in kernel
# ---------------------------
uboot-init = 1;


# ---------------------------
# No need to modify
# ---------------------------
unprepare-delay-ms = 100;
enable-delay-ms = 100;
disable-delay-ms = 100;
prepare-delay-ms = 100;
reset-delay-ms = 100;
init-delay-ms = 100;
width-mm = 100;
height-mm = 100;


# ---------------------------
# panel timing
# ---------------------------
clock-frequency = 152000000;    # clock
hactive = 1920;                 # width
vactive = 1080;                 # height
hback-porch = 192;              # hbp
hfront-porch = 48;              # hfp
vback-porch = 71;               # vbp
vfront-porch = 3;               # vfp
hsync-len = 32;                 # hs
vsync-len = 6;                  # vs
hsync-active = 0;
vsync-active = 0;
de-active = 0;
pixelclk-active = 0;


# ---------------------------
# for lvds panel
# ---------------------------
# 0：MEDIA_BUS_FMT_RGB565_1X16 | 1：MEDIA_BUS_FMT_RGB666_1X18 | 2：MEDIA_BUS_FMT_RGB888_1X24 | 3：MEDIA_BUS_FMT_ARGB8888_1X32
lvds,format = 2;

# Lvds data format, 0：vesa 1: jeida
lvds,mode = 0;

# Lvds data width, 18 | 24
lvds,width = 24;

# Single or dual lvds, 0：Single | 1：Dual
lvds,channel = 1;


# ---------------------------
# for mipi panel
# ---------------------------
#dsi,lane-rate = 500
#dsi,flags = 0;
#dsi,format = 0;
#dsi,lanes = 4;
#panel-init-sequence = 29 00 06 14 01 08 00 00 00 ff aa 01 02 03 04 05 06 07 ff aa AA bb ff;
```

## Usage
```
ls328-default:/ $ lcdparamservice -h
USAGE: [-srw] [-k key] [-v value]
WHERE: -s = scan sdcard and udisk
       -r = read parameter
       -w = write parameter
       -k = key
       -v = value

```

### Update screen parameters with u-disk or sdcard
1. Refer to the lcd_parameters file to modify the parameters inside to the actual lcd parameters.
2. Copy the lcd_parameters file to the u-disk or sdcard.
3. Insert the u-disk or sdcard into the Android board.
4. The lcdparamservice will detect lcd_parameters and parse it, then restart.

### Manually modify specific parameters
For example, change the screen density to 240：
```
$ lcdparamservice -w -k density -v 240
```

### Read specific parameters
For example, read the screen density：
```
$ lcdparamservice -r -k density
```

## Developed By
* ayst.shen@foxmail.com

## License
```
Copyright 2019 Bob Shen

Licensed under the Apache License, Version 2.0 (the "License"); you may 
not use this file except in compliance with the License. You may obtain 
a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
License for the specific language governing permissions and limitations 
under the License.
```
