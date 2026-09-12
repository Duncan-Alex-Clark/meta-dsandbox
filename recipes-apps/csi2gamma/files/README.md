# Summary
This application is designed to stream a csi2 camera feed directly to a display via the display port of the kv260, with a Video Gamma LUT in the processing chain so the effect of gamma correction can be seen live.

    OV5647 -> MIPI CSI-2 RX -> Demosaic -> Gamma LUT -> Frame Buffer Write -> DDR

For this application to work, there are some commands that need to be run.

Note: the commands below have no leading '>' characters, so they can be pasted directly into a shell.

## Steps to Launch Application
1. After booting the system, unload the default application with 'xmutil unloadapp'
2. Load the csi2gamma application with 'xmutil loadapp csi2gamma'
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
    '"fpga-region:isp_subset_converto":0[fmt:SBGGR10_1X10/1920x1080 field:none]'

media-ctl -d /dev/media0 \
    --set-v4l2 \
    '"fpga-region:isp_subset_converto":1[fmt:SBGGR8_1X8/1920x1080 field:none]'

media-ctl -d /dev/media0 \
    --set-v4l2 \
    '"80030000.v-demosaic":0[fmt:SBGGR8_1X8/1920x1080 field:none]'

media-ctl -d /dev/media0 \
    --set-v4l2 \
    '"80030000.v-demosaic":1[fmt:RBG888_1X24/1920x1080 field:none]'

media-ctl -d /dev/media0 \
    --set-v4l2 \
    '"80050000.v_gamma_lut":0[fmt:RBG888_1X24/1920x1080 field:none]'

media-ctl -d /dev/media0 \
    --set-v4l2 \
    '"80050000.v_gamma_lut":1[fmt:RBG888_1X24/1920x1080 field:none]'

v4l2-ctl -d /dev/video0 \
    --set-fmt-video=width=1920,height=1080,pixelformat=BGR3

Confirm the whole chain before streaming. Every adjacent pad pair must
agree on code, width and height:

media-ctl -d /dev/media0 -p | grep -E '^- entity|fmt:'

Note on the subset converter entity name. It is spelled
'fpga-region:isp_subset_converto', with the trailing 'r' missing. That is
not a typo. The media controller entity name field holds 31 characters plus
a terminator, and the node name in pl.dtsi produces a 32 character device
name, so the kernel truncates the last character. /sys/class/video4linux/
shows the full name; media-ctl does not. Using the untruncated name makes
media-ctl fail to find the entity, and because that failure is quiet the
converter silently keeps its power-on default of RGB888_1X24 on both pads.

Note on the converter pad order. The sink must be set before the source.
Setting the sink copies width, height and field to the source pad while
deliberately preserving the source pad's own code; setting the source then
applies the requested code. Reversed, the sink write flattens the source
back to 10-bit and the conversion disappears.

Note on the demosaic sink. It is SBGGR8_1X8, not SBGGR10_1X10. The subset
converter has already narrowed the stream to 8 bits by that point. This
differs from a pipeline with no converter in it.

Note on the two gamma pads: the entity is spelled with underscores, unlike
the others, because the node in pl.dtsi is named v_gamma_lut@80050000
rather than v-gamma-lut@80050000. If the node is ever renamed to match the
rest, these two commands become "80050000.v-gamma-lut".

The gamma ports are xlnx,video-width = <8>, so the RGB half of the chain is
RBG888_1X24 and capture is BGR3, exactly as in csi2testing.

4. Set the sensor controls

This pipeline has four subdevices rather than csi2testing's three, so the
sensor is no longer /dev/v4l-subdev2. Look it up by name:

SENSOR=$(media-ctl -d /dev/media0 -e "ov5647 6-0036")

v4l2-ctl -d $SENSOR \
        --set-ctrl=auto_exposure=1,gain_automatic=1,white_balance_automatic=1

echo 0 > /sys/class/vtconsole/vtcon1/bind

modetest -M zynqmp-dpsub \
        -w 36:alpha:0

5. Confirm frames are being captured

This exercises the whole capture path with no gstreamer and no display, so a
failure here is upstream of both:

timeout 10 v4l2-ctl -d /dev/video0 --stream-mmap --stream-count=10000 2>&1 | tr -cd '<' | wc -c

Expect roughly 300 frames in 10 seconds.

6. Set the display to 1920x1080p

This holds the mode, so leave it running in the foreground.

modetest -M zynqmp-dpsub \
    -s 42@40:1920x1080-60@BG24 \
    -d

7. Open a new terminal tab and start an ssh session as root user into the KV260
8. Start the video streaming session

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

## Configuring the Gamma block
9. Open a third terminal tab and start another ssh session as root user

The Gamma LUT is a V4L2 subdevice, so its correction curves are ordinary
controls. No register writes are needed.

GAMMA=$(media-ctl -d /dev/media0 -e "80050000.v_gamma_lut")

v4l2-ctl -d $GAMMA --list-ctrls

There are three controls, one per channel. Each takes a value from 1 to 40
with a default of 10, the value being gamma multiplied by 10:

    value 10  ->  gamma 1.0  (linear, no visible change)
    value  4  ->  gamma 0.4  (brighter midtones)
    value 22  ->  gamma 2.2  (darker midtones)

    red_gamma_correction_1_0_1_10    0x0098c9c1
    blue_gamma_correction_1_0_1_10   0x0098c9c2
    green_gamma_correction_1_0_1_1   0x0098c9c3

The generated control names are awkward to type, so set them by numeric ID:

v4l2-ctl -d $GAMMA --set-ctrl=0x0098c9c1=4
v4l2-ctl -d $GAMMA --set-ctrl=0x0098c9c2=4
v4l2-ctl -d $GAMMA --set-ctrl=0x0098c9c3=4

Read the current values back at any time:

v4l2-ctl -d $GAMMA --get-ctrl=0x0098c9c1,0x0098c9c2,0x0098c9c3

10. Sweep the gamma to make the effect obvious

for g in 4 7 10 15 20 25 30; do
    v4l2-ctl -d $GAMMA --set-ctrl=0x0098c9c1=$g
    v4l2-ctl -d $GAMMA --set-ctrl=0x0098c9c2=$g
    v4l2-ctl -d $GAMMA --set-ctrl=0x0098c9c3=$g
    echo "gamma = 0.$g"
    sleep 2
done

Return to linear when finished:

v4l2-ctl -d $GAMMA --set-ctrl=0x0098c9c1=10
v4l2-ctl -d $GAMMA --set-ctrl=0x0098c9c2=10
v4l2-ctl -d $GAMMA --set-ctrl=0x0098c9c3=10

Driving a single channel tints the image, which makes it obvious the LUT is
live rather than the sensor's own auto white balance reacting:

v4l2-ctl -d $GAMMA --set-ctrl=0x0098c9c1=5

The controls take effect on the running stream, so gst-launch does not need to
be restarted between values.

## Troubleshooting

gst-launch fails immediately with "Failed to allocate required memory" and
"Buffer pool activation failed". This is almost never a memory problem.
GStreamer's v4l2 buffer pool issues VIDIOC_STREAMON as part of activating
the pool, so a pipeline that fails link validation reports it here rather
than as a negotiation error. Check CmaFree in /proc/meminfo to rule memory
out, then dump the graph and look for two adjacent pads whose codes differ:

grep -i cma /proc/meminfo
media-ctl -d /dev/media0 -p | grep -E '^- entity|fmt:'

The usual cause is that the subset converter never got configured, because
media-ctl was given its untruncated entity name and quietly did nothing.
Both converter pads reading RGB888_1X24 is the signature of this.

No /dev/media0. The composite driver only registers the media device once
every subdevice in the graph has bound. Check 'dmesg | grep ov5647'.

media-ctl reports it cannot find the gamma entity. Confirm the exact spelling,
which follows the node name in pl.dtsi:

media-ctl -d /dev/media0 -p | grep entity

VIDIOC_STREAMON fails with EINVAL. Two adjacent pads disagree. The DMA's media
bus code, width and height must match the Gamma source pad exactly. Compare
'media-ctl -d /dev/media0 -p' against 'v4l2-ctl -d /dev/video0 --get-fmt-video'.

Step 5 reports 0 frames. The pipeline is configured but no pixel data is
reaching the frame buffer. Confirm all four entities appear in 'media-ctl -p'
with their links ENABLED, and check dmesg for sensor errors:

dmesg | grep -iE "ov5647|csi2rx"

Gamma controls have no visible effect. Confirm the stream is actually running
and that the values are being applied, with '--get-ctrl' above. A value of 10
is linear and looks identical to no gamma at all.

## Conclusion
After performing the above steps, you should see a video feed visible on your connected display, and its gamma response should change as the controls are adjusted from the third session.
