#!/usr/bin/env python3
"""
Configure the UIO-controlled blocks of the csi2gamma pipeline.

Demosaic and the Gamma LUT are not V4L2 subdevices in this design - they are
bound to uio_pdrv_genirq and programmed from here.  Nothing else starts them,
so this must run before VIDIOC_STREAMON or the pipeline stalls.

Register maps are taken from the in-tree drivers:
  drivers/media/platform/xilinx/xilinx-demosaic.c
  drivers/media/platform/xilinx/xilinx-gamma.c
"""

import argparse
import mmap
import os
import struct
import sys
import time

# --- demosaic ---------------------------------------------------------------
DM_AP_CTRL, DM_WIDTH, DM_HEIGHT, DM_BAYER = 0x00, 0x10, 0x18, 0x28
BAYER = {"rggb": 0, "grbg": 1, "gbrg": 2, "bggr": 3}

# --- gamma lut --------------------------------------------------------------
GM_AP_CTRL, GM_WIDTH, GM_HEIGHT, GM_VFMT = 0x0000, 0x0010, 0x0018, 0x0020
GM_LUT_BASE = (0x0800, 0x1000, 0x1800)   # red, green, blue
GM_RGB = 0

# AP_CTRL: bit0 START, bit1 DONE, bit2 IDLE, bit3 READY, bit7 AUTO_RESTART
STREAM_ON = (1 << 0) | (1 << 7)

# AXI GPIO holding the active-low core resets.  Not a UIO device - it belongs
# to gpio-xilinx - so it is reached through /dev/mem.  Only bits 0 and 3 are
# touched; bit 2 is the OV5647 power-down and is owned by the sensor driver.
GPIO_BASE = 0x80000000
GPIO_DATA = 0x00
RESET_BITS = (1 << 0) | (1 << 3)     # demosaic, gamma


def pulse_resets():
    """Reset the demosaic and gamma cores.

    Their V4L2 drivers used to do this in s_stream(0).  Under UIO nothing
    does, so after a stream stops both cores are left part-way through a
    frame (AP_CTRL reads 0x81 with ap_idle clear) and will never resync with
    the next frame.  They must be reset before being re-armed.
    """
    page = os.sysconf("SC_PAGE_SIZE")
    fd = os.open("/dev/mem", os.O_RDWR | os.O_SYNC)
    try:
        mem = mmap.mmap(fd, page, offset=GPIO_BASE)
        cur = struct.unpack("<I", mem[GPIO_DATA:GPIO_DATA + 4])[0]
        mem[GPIO_DATA:GPIO_DATA + 4] = struct.pack("<I", cur & ~RESET_BITS)
        time.sleep(0.02)
        mem[GPIO_DATA:GPIO_DATA + 4] = struct.pack("<I", cur | RESET_BITS)
        time.sleep(0.02)
        mem.close()
    finally:
        os.close(fd)


def find_uio(name):
    root = "/sys/class/uio"
    for entry in sorted(os.listdir(root)):
        try:
            with open(f"{root}/{entry}/name") as fh:
                if fh.read().strip() != name:
                    continue
            with open(f"{root}/{entry}/maps/map0/size") as fh:
                size = int(fh.read().strip(), 16)
            return f"/dev/{entry}", size
        except OSError:
            continue
    sys.exit(
        f'UIO device "{name}" not found.\n'
        "Load the driver with its match string first:\n"
        '    modprobe uio_pdrv_genirq of_id="generic-uio"'
    )


class Core:
    """A memory-mapped UIO device."""

    def __init__(self, name):
        path, size = find_uio(name)
        self.path = path
        self.fd = os.open(path, os.O_RDWR | os.O_SYNC)
        self.mem = mmap.mmap(self.fd, size, offset=0)

    def write(self, off, val):
        self.mem[off:off + 4] = struct.pack("<I", val & 0xFFFFFFFF)

    def read(self, off):
        return struct.unpack("<I", self.mem[off:off + 4])[0]

    def close(self):
        self.mem.close()
        os.close(self.fd)


def gamma_curve(gamma, depth):
    """LUT for the given gamma. 1.0 is linear (no visible change)."""
    top = (1 << depth) - 1
    return [min(top, round(((v / top) ** gamma) * top)) for v in range(top + 1)]


def program_gamma(core, width, height, depth, r, g, b, start=True):
    core.write(GM_WIDTH, width)
    core.write(GM_HEIGHT, height)
    core.write(GM_VFMT, GM_RGB)
    # The core packs two 16-bit entries per 32-bit word, 1 << (depth-1) words.
    for base, curve in zip(GM_LUT_BASE, (gamma_curve(r, depth),
                                         gamma_curve(g, depth),
                                         gamma_curve(b, depth))):
        off = base
        for i in range(1 << (depth - 1)):
            core.write(off, (curve[2 * i + 1] << 16) | curve[2 * i])
            off += 4
    if start:
        core.write(GM_AP_CTRL, STREAM_ON)


def program_demosaic(core, width, height, pattern, start=True):
    core.write(DM_WIDTH, width)
    core.write(DM_HEIGHT, height)
    core.write(DM_BAYER, BAYER[pattern])
    if start:
        core.write(DM_AP_CTRL, STREAM_ON)


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--width", type=int, default=1920)
    ap.add_argument("--height", type=int, default=1080)
    ap.add_argument("--bayer", choices=sorted(BAYER), default="bggr")
    ap.add_argument("--depth", type=int, default=10,
                    help="gamma LUT bit depth; must match xlnx,video-width")
    ap.add_argument("--gamma", type=float, default=1.0,
                    help="applied to all three channels unless overridden")
    ap.add_argument("--red", type=float)
    ap.add_argument("--green", type=float)
    ap.add_argument("--blue", type=float)
    ap.add_argument("--gamma-only", action="store_true",
                    help="reprogram the LUTs only; leave the demosaic alone")
    ap.add_argument("--no-reset", action="store_true",
                    help="skip the reset pulse (use when retuning a live stream)")
    args = ap.parse_args()

    # A core left mid-frame by a previous stream never resyncs, so reset
    # before arming.  Skipped for --gamma-only, which is meant to be safe to
    # run against a running stream.
    if not args.gamma_only and not args.no_reset:
        pulse_resets()

    r = args.red if args.red is not None else args.gamma
    g = args.green if args.green is not None else args.gamma
    b = args.blue if args.blue is not None else args.gamma

    gm = Core("csi2gamma-gamma-lut")
    program_gamma(gm, args.width, args.height, args.depth, r, g, b)
    print(f"gamma  : {args.width}x{args.height} R={r} G={g} B={b} "
          f"AP_CTRL=0x{gm.read(GM_AP_CTRL):02x}")
    gm.close()

    if not args.gamma_only:
        dm = Core("csi2gamma-demosaic")
        program_demosaic(dm, args.width, args.height, args.bayer)
        print(f"demosaic: {args.width}x{args.height} {args.bayer} "
              f"AP_CTRL=0x{dm.read(DM_AP_CTRL):02x}")
        dm.close()


if __name__ == "__main__":
    main()
