SUMMARY = "KV260 CSI2 Gamma FPGA application"
DESCRIPTION = "Builds and installs the CSI2Gamma device-tree overlay, FPGA bitstream, and metadata. Streams an OV5647 feed through a Video Gamma LUT so its correction controls can be exercised live."
LICENSE = "CLOSED"

DEPENDS = "dtc-native"

SRC_URI = " \
    file://csi2gamma_wrapper.bin \
    file://device-tree \
    file://shell.json \
    file://README.md \
"

S = "${WORKDIR}"
B = "${WORKDIR}/build"

# The bitstream and overlay are specific to the target hardware.
PACKAGE_ARCH = "${MACHINE_ARCH}"

FPGA_APP_DIR = "${nonarch_base_libdir}/firmware/xilinx/csi2gamma"

DTS_DIR = "${WORKDIR}/device-tree"
DTS_SOURCE = "${DTS_DIR}/pl.dtsi"
DTBO_OUTPUT = "${B}/csi2gamma.dtbo"

do_compile[cleandirs] = "${B}"

do_compile() {
    # -@ generates the symbol and fixup sections needed when an
    # overlay references labels from the base device tree.
    dtc \
        -@ \
        -I dts \
        -O dtb \
        -i ${DTS_DIR} \
        -i ${DTS_DIR}/include \
        -o ${DTBO_OUTPUT} \
        ${DTS_SOURCE}
}

do_install() {
    # Install the FPGA application bundle.
    #
    # dfx-mgr locates the bitstream and overlay by the accelerator
    # directory name, so both are installed as csi2gamma.*  The
    # firmware-name property inside the overlay is not used for this.
    install -d ${D}${FPGA_APP_DIR}

    install -m 0644 \
        ${WORKDIR}/csi2gamma_wrapper.bin \
        ${D}${FPGA_APP_DIR}/csi2gamma.bin

    install -m 0644 \
        ${DTBO_OUTPUT} \
        ${D}${FPGA_APP_DIR}/csi2gamma.dtbo

    install -m 0644 \
        ${WORKDIR}/shell.json \
        ${D}${FPGA_APP_DIR}/shell.json

    install -m 0644 \
        ${WORKDIR}/README.md \
        ${D}${FPGA_APP_DIR}/README.md

}

FILES:${PN} += " \
    ${FPGA_APP_DIR} \
"

# files/csi2gamma-uio.py is retained for reference but deliberately NOT
# installed.  It programs the Demosaic and Gamma LUT registers directly, which
# was needed when those blocks were UIO devices.  They are V4L2 subdevices
# again, so their drivers own those registers and the gamma correction is set
# with v4l2-ctl instead - see README.md.
