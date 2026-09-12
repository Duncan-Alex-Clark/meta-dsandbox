SUMMARY = "Xilinx Video Gamma LUT driver, out of tree, with 10-bit media bus support"
DESCRIPTION = "Builds the Xilinx Video Gamma LUT driver as an out-of-tree \
kernel module.  0001-xilinx-gamma-support-10-bit-media-bus-code.patch lets it \
report and accept MEDIA_BUS_FMT_RBG101010_1X30 when the IP is built 10 bits \
wide.  Upstream hard-codes MEDIA_BUS_FMT_RBG888_1X24 on the sink pad and as \
the default format, even though it already programs a full 1024-entry 10-bit \
LUT selected from xlnx,video-width.  That mismatch makes the frame buffer's \
xbgr2101010 format unreachable, because xvip_dma_verify_format() requires the \
DMA's media bus code to equal the Gamma source pad's exactly.  The in-tree \
driver is disabled by recipes-kernel/xilinx-gamma so this module is the only \
provider of xilinx-gamma.ko."
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://xilinx-gamma.c;beginline=1;endline=10;md5=58e336f5f9daa78f320603b33aa5e999"

inherit module

# The three files are the pristine sources from this kernel
# (drivers/media/platform/xilinx).  xilinx-gamma.c is kept unmodified so the
# delta stays visible in the patch, which bitbake applies during do_patch.
#
# xilinx-vip.h and xilinx-gamma-coeff.h are private headers of that directory,
# so they have to travel with the driver.  The xvip_* functions it calls
# (xvip_init_resources, xvip_cleanup_resources, xvip_enum_mbus_code,
# xvip_enum_frame_size) are EXPORT_SYMBOL_GPL from the built-in xilinx-video
# core, so they resolve at load time.
SRC_URI = " \
    file://Makefile \
    file://xilinx-gamma.c \
    file://xilinx-gamma-coeff.h \
    file://xilinx-vip.h \
    file://0001-xilinx-gamma-support-10-bit-media-bus-code.patch \
"

S = "${WORKDIR}"

# module.bbclass sets KERNEL_MODULES_META_PACKAGE = "${PN}", so the split
# kernel-module-xilinx-gamma-${KERNEL_VERSION} package is pulled in by
# RDEPENDS when gamma-patched is added to IMAGE_INSTALL.
RPROVIDES:${PN} += "kernel-module-xilinx-gamma"
