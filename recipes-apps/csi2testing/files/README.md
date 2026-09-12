# Summary
This application is designed to stream a csi2 camera feed directly to a display via the display port of the kv260. 

For this application to work, there are some commands that need to be run.

## Steps to Launch Application
1. After booting the system, unload the default application with 'xmutil unloadapp'
2. Load the csi2testing application with 'xmutil loadapp csi2testing'
3. Run the image processing pipeline configuration script

media-ctl -d /dev/media0 \
    --set-v4l2 \
    '"ov5647 6-0036":0[fmt:SBGGR10_1X10/1920x1080 field:none]'

media-ctl -d /dev/media0 \
    --set-v4l2 \
    '"80020000.mipi-csi2-rx-subsystem":0[fmt:SBGGR10_1X10/1920x1080 field:none]'

media-ctl -d /dev/media0 \
    --set-v4l2 \
    '"80020000.mipi-csi2-rx-subsystem":1[fmt:SBGGR10_1X10/1920x1080 field:none]'

media-ctl -d /dev/media0 \
    --set-v4l2 \
    '"80030000.v-demosaic":0[fmt:SBGGR10_1X10/1920x1080 field:none]'

media-ctl -d /dev/media0 \
    --set-v4l2 \
    '"80030000.v-demosaic":1[fmt:RBG888_1X24/1920x1080 field:none]'

v4l2-ctl -d /dev/video0 \
    --set-fmt-video=width=1920,height=1080,pixelformat=BGR3

v4l2-ctl -d /dev/v4l-subdev2 \
	    --set-ctrl=auto_exposure=1,gain_automatic=1,white_balance_automatic=1

echo 0 > /sys/class/vtconsole/vtcon1/bind

modetest -M zynqmp-dpsub \
	    -w 36:alpha:0

4. Set the display to 1920x1080p

modetest -M zynqmp-dpsub \
     -s 42@40:1920x1080-60@BG24 \
     -d

5. Open a new terminal tab and start an ssh session as root user into the KV260
6. Start the video streaming session

gst-launch-1.0 -v \
     v4l2src \
         device=/dev/video0 \
         io-mode=mmap \
     ! video/x-raw,width=1920,height=1080,format=BGR,framerate=30/1 \
     ! kmssink \
         driver-name=zynqmp-dpsub \
         plane-id=33 \
         can-scale=false \
         render-rectangle='<0,0,1920,1080>' \
         hold-extra-sample=true \
         show-preroll-frame=false \
         sync=true

## Conclusion
After performing the above steps, you should see a video feed visible on your connected display






















