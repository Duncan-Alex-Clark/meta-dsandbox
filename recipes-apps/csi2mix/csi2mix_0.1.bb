SUMMARY = "KV260 CSI2 Mixing Test Application"
DESCRIPTION = "To mix two video stream creating a picture-in-picture effect"
LICENSE = "CLOSED"

DEPENDS = "dtc-native"

SRC_URI = "\
    file://csi2mix.bin \
    file://device-tree \
    file://shell.json \
    file://README.md \
"

FPGA_APP_DIR = "/lib/firmware/xilinx/csi2mix"
DTS_DIR = "${WORKDIR}/device-tree"

do_compile() {
    dtc \
        -@ \
        -I dts \
        -O dtb \
        -o csi2mix.dtbo \
        ${DTS_DIR}/pl.dtsi
}

do_install() {
    install -d ${D}${FPGA_APP_DIR}

    install -m 0644 \
        ${WORKDIR}/csi2mix.bin \
        ${D}${FPGA_AP_DIR}/csi2mix.bin

    install -m 0644 \
        ${WORKDIR}/csi2mix.dtbo \
        ${D}${FPGA_APP_DIR}/csi2mix.dtbo

    install -m 0644 \
        ${WORKDIR}/shell.json \
        ${D}${FPGA_APP_DIR}/shell.json

    install -m 0644 \
        ${WORKDIR}/README.md \
        ${D}${FPGA_APP_DIR}/README.md
}

FILES:${PN}:append = "\
    ${FPGA_APP_DIR} \
"















