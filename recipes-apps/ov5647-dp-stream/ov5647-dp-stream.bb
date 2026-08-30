SUMMARY = "KV260 OV5647 streaming application"
DESCRIPTION = "Streams OV5647 sensor data out through display port"
LICENSE = "CLOSED"

DEPENDS = "dtc-native"

SRC_URI = " \
    file://ov5647stream.bin \
    file://device-tree \
    file://shell.json \
    file://launch_ov5647stream.sh \
"

S = "${WORKDIR}"
B = "${WORKDIR}/build"

# The bitstream and overlay are specific to the target hardware.
PACKAGE_ARCH = "${MACHINE_ARCH}"

FPGA_APP_DIR = "${nonarch_base_libdir}/firmware/xilinx/ov5647stream"

DTS_DIR = "${WORKDIR}/device-tree"
DTS_SOURCE = "${DTS_DIR}/pl.dtsi"
DTS_PREPROCESSED = "${B}/pl.dts.pp"
DTBO_OUTPUT = "${B}/ov5647stream.dtbo"

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
    install -d ${D}${ROOT_HOME}

    # Install the FPGA application bundle.
    install -d ${D}${FPGA_APP_DIR}

    install -m 0644 \
        ${WORKDIR}/ov5647stream.bin \
        ${D}${FPGA_APP_DIR}/ov5647stream.bin

    install -m 0644 \
        ${DTBO_OUTPUT} \
        ${D}${FPGA_APP_DIR}/ov5647stream.dtbo

    install -m 0644 \
        ${WORKDIR}/shell.json \
        ${D}${FPGA_APP_DIR}/shell.json
    
    install -m 0644 \
        ${WORKDIR}/launch_ov5647stream.sh \
        ${D}${ROOT_HOME}/launch_ov5647stream.sh
}

FILES:${PN} += " \
    ${FPGA_APP_DIR} \
    ${ROOT_HOME} \
"
