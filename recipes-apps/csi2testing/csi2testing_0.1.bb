SUMMARY = "KV260 CSI2Stream FPGA application"
DESCRIPTION = "Builds and installs the CSI2Stream device-tree overlay, FPGA bitstream, metadata, and launch scripts."
LICENSE = "CLOSED"

DEPENDS = "dtc-native"

SRC_URI = " \
    file://csi2testing.bin \
    file://device-tree \
    file://shell.json \
    file://README.md \
"

S = "${WORKDIR}"
B = "${WORKDIR}/build"

# The bitstream and overlay are specific to the target hardware.
PACKAGE_ARCH = "${MACHINE_ARCH}"

FPGA_APP_DIR = "${nonarch_base_libdir}/firmware/xilinx/csi2testing"

DTS_DIR = "${WORKDIR}/device-tree"
DTS_SOURCE = "${DTS_DIR}/pl.dtsi"
DTS_PREPROCESSED = "${B}/pl.dts.pp"
DTBO_OUTPUT = "${B}/csi2testing.dtbo"

do_compile[cleandirs] = "${B}"

do_compile() {
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
    install -d ${D}${FPGA_APP_DIR}

    install -m 0644 \
        ${WORKDIR}/csi2testing.bin \
        ${D}${FPGA_APP_DIR}/csi2testing.bin

    install -m 0644 \
        ${DTBO_OUTPUT} \
        ${D}${FPGA_APP_DIR}/csi2testing.dtbo

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
