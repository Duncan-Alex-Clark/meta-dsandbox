# OV5647 out-of-tree driver with the KV260 I2C timing patch

This **is** wired into the build. Building the image pulls it in automatically.

## What it does

Upstream's `sensor_oe_enable_regs` enables every output pad:

| register | upstream | patched |
|----------|----------|---------|
| 0x3000   | 0x0f     | 0x0c    |
| 0x3001   | **0xff** | **0x1f**|
| 0x3002   | 0xe4     | 0xe4    |

On the KV260, writing 0xff to 0x3001 leaves the sensor unable to accept
another SCCB transfer for tens of milliseconds, so either the next write in
the array or the first read in `ov5647_detect()` returns -EIO and probe fails
with "camera not available, check power".

Measured on this board: back-to-back writes of `0x3001 = 0xff` succeeded
**4/30**, while `0x00`, `0x0f` and `0x55` succeeded **30/30**. `0x1f` is in
that working range. The failure is sensor-state dependent, which is why probe
failed intermittently rather than every time.

Values from https://github.com/juanma-rm/kv260_rpicamera_to_dp
(`plan_port_v4l2.md` §2.3).

## How it is wired in

Three pieces, all automatic:

1. **`recipes-kernel/ov5647/files/ov5647.cfg`** disables the in-tree driver:

       # CONFIG_VIDEO_OV5647 is not set

   It must stay disabled. Built in (`=y`) it claims the sensor at boot and the
   out-of-tree module can never bind. As a module (`=m`) there would be two
   `ov5647.ko` providing the same module name and depmod would choose between
   them arbitrarily.

2. **`ov5647-patched_1.0.bb`** builds the driver out of tree. `files/ov5647.c`
   is the pristine driver from this kernel; the fix is applied at build time by
   `files/0001-ov5647-kv260-i2c-timing.patch` during `do_patch`, so the delta
   stays visible and reviewable rather than being pre-baked into the source.

3. **`build/conf/local.conf`**:

       IMAGE_INSTALL:append = " ov5647-patched"

   `module.bbclass` sets `KERNEL_MODULES_META_PACKAGE = "${PN}"`, so this pulls
   in the split `kernel-module-ov5647-${KERNEL_VERSION}` package that actually
   contains the `.ko`.

The module auto-loads: the device tree instantiates the sensor as an I2C
client with `compatible = "ovti,ov5647"`, and `MODULE_DEVICE_TABLE(of, ...)`
gives depmod the matching alias.

## Source provenance

`files/ov5647.c` is `drivers/media/i2c/ov5647.c` from **your** kernel
(linux-xlnx 6.12.10), deliberately not a newer upstream copy. An out-of-tree
module must match the V4L2 subdev API of the kernel it is built against, and
that API has changed since the `xlnx_rebase_v5.15` tree the reference project
used, so a newer driver would not compile here.

If the kernel is ever uprevved, refresh `files/ov5647.c` from the new tree and
re-check that the patch still applies.

## Verifying after flashing

    # the module is present and is the only ov5647
    find /lib/modules/$(uname -r) -name 'ov5647.ko*'

    # it loaded and bound
    dmesg | grep -i ov5647
    ls -l /sys/bus/i2c/devices/6-0036/driver

    # confirm the patched values took effect
    modinfo ov5647 | head -3

Probe should now succeed consistently rather than intermittently, so
`/dev/media0` should appear on every `xmutil loadapp`.
