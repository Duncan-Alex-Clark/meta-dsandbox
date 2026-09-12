SUMMARY = "OV5647 sensor driver, out of tree, with the KV260 I2C timing fix"
DESCRIPTION = "Builds the OV5647 sensor driver as an out-of-tree kernel \
module.  The KV260 I2C timing fix is applied at build time by \
0001-ov5647-kv260-i2c-timing.patch: upstream's sensor_oe_enable_regs writes \
0xff to register 0x3001, which leaves the sensor unable to accept another \
SCCB transfer for tens of milliseconds, so probe intermittently fails with \
-EIO (\"camera not available, check power\").  See \
https://github.com/juanma-rm/kv260_rpicamera_to_dp plan_port_v4l2.md \
section 2.3.  The in-tree driver is disabled by recipes-kernel/ov5647 so \
this module is the only provider of ov5647.ko."
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://ov5647.c;beginline=1;endline=12;md5=4535583e8e356c2b9e63b2574a1135e8"

inherit module

# ov5647.c is the pristine driver from this kernel (linux-xlnx 6.12.10,
# drivers/media/i2c/ov5647.c).  It is kept unmodified so the delta stays
# visible in the patch, which bitbake applies during do_patch.
#
# It is deliberately not a newer upstream copy: an out-of-tree module has to
# match the V4L2 subdev API of the kernel it is built against, and that API
# has moved since the xlnx_rebase_v5.15 tree the reference project used.
SRC_URI = " \
    file://Makefile \
    file://ov5647.c \
    file://0001-ov5647-kv260-i2c-timing.patch \
"

S = "${WORKDIR}"

# module.bbclass sets KERNEL_MODULES_META_PACKAGE = "${PN}", so the split
# kernel-module-ov5647-${KERNEL_VERSION} package is pulled in by RDEPENDS when
# ov5647-patched is added to IMAGE_INSTALL.  This makes the unversioned name
# resolvable too, matching poky's hello-mod example.
RPROVIDES:${PN} += "kernel-module-ov5647"
