SUMMARY = "KV260 CSI2Stream FPGA application"
DESCRIPTION = "Builds and installs the CSI2Stream device-tree overlay, FPGA bitstream, metadata, and launch scripts."
LICENSE = "CLOSED"

DEPENDS = "dtc-native"

SRC_URI = " \
    file://csi2stream.bin \
    file://device-tree \
    file://dp-res.sh \
    file://launch.sh \
    file://shell.json \
"

S = "${WORKDIR}"
B = "${WORKDIR}/build"

# The bitstream and overlay are specific to the target hardware.
PACKAGE_ARCH = "${MACHINE_ARCH}"

FPGA_APP_DIR = "${nonarch_base_libdir}/firmware/xilinx/csi2stream"

DTS_DIR = "${WORKDIR}/device-tree"
DTS_SOURCE = "${DTS_DIR}/pl.dtsi"
DTS_PREPROCESSED = "${B}/pl.dts.pp"
DTBO_OUTPUT = "${B}/csi2stream.dtbo"

do_compile[cleandirs] = "${B}"

do_compile() {
    # Run the C preprocessor so that #include directives and
    # dt-bindings constants in pl.dtsi can be resolved.
    ${BUILD_CPP} \
        -nostdinc \
        -undef \
        -D__DTS__ \
        -x assembler-with-cpp \
        -I${DTS_DIR} \
        -I${DTS_DIR}/include \
        -o ${DTS_PREPROCESSED} \
        ${DTS_SOURCE}

    # Compile the preprocessed overlay.
    #
    # -@ generates the symbol and fixup sections needed when an
    # overlay references labels from the base device tree.
    dtc \
        -@ \
        -I dts \
        -O dtb \
        -i ${DTS_DIR} \
        -i ${DTS_DIR}/include \
        -o ${DTBO_OUTPUT} \
        ${DTS_PREPROCESSED}
}

do_install() {
    # Install scripts in the root user's home directory.
    install -d ${D}${ROOT_HOME}

    install -m 0755 \
        ${WORKDIR}/dp-res.sh \
        ${D}${ROOT_HOME}/dp-res.sh

    install -m 0755 \
        ${WORKDIR}/launch.sh \
        ${D}${ROOT_HOME}/launch.sh

    # Install the FPGA application bundle.
    install -d ${D}${FPGA_APP_DIR}

    install -m 0644 \
        ${WORKDIR}/csi2stream.bin \
        ${D}${FPGA_APP_DIR}/csi2stream.bin

    install -m 0644 \
        ${DTBO_OUTPUT} \
        ${D}${FPGA_APP_DIR}/csi2stream.dtbo

    install -m 0644 \
        ${WORKDIR}/shell.json \
        ${D}${FPGA_APP_DIR}/shell.json
}

FILES:${PN} += " \
    ${ROOT_HOME}/dp-res.sh \
    ${ROOT_HOME}/launch.sh \
    ${FPGA_APP_DIR} \
"
