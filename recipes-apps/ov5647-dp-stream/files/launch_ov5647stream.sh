sudo xmutil unloadapp
sudo xmutil loadapp ov5647stream
# Stop before changing frame dimensions
sudo devmem 0x80010000 32 0x00000000

# Active height = 1080
sudo devmem 0x80010010 32 0x00000438

# Active width = 1920
sudo devmem 0x80010018 32 0x00000780

# Background pattern = color bars
sudo devmem 0x80010020 32 0x00000009

# No foreground overlay
sudo devmem 0x80010028 32 0x00000000

# No mask
sudo devmem 0x80010030 32 0x00000000

# Motion speed
sudo devmem 0x80010038 32 0x00000001

# RGB
sudo devmem 0x80010040 32 0x00000000

# Enable AXI4-Stream video input
sudo devmem 0x80010098 32 0x00000001

# Full-frame pass-through rectangle
# Left boundary: inclusive
#sudo devmem 0x800100A0 32 0x00000000

# Right boundary: exclusive = 1920
#sudo devmem 0x800100A8 32 0x00000780

# Upper boundary: inclusive
#sudo devmem 0x800100B0 32 0x00000000

# Lower boundary: exclusive = 1080
#sudo devmem 0x800100B8 32 0x00000438

# Start continuously: ap_start + auto_restart
#sudo devmem 0x80010000 32 0x00000081

# VTC base address: 0x80000000

# Reset/disable the VTC
sudo devmem 0x80000000 32 0x80000000
sudo devmem 0x80000000 32 0x00000000

# Generator active size:
# vertical active = 1080, horizontal active = 1920
sudo devmem 0x80000060 32 0x04380780

# Progressive video encoding
sudo devmem 0x80000068 32 0x00000000

# Positive active-video, blanking, HSYNC, and VSYNC polarities
sudo devmem 0x8000006C 32 0x0000007F

# Horizontal total = 2200
sudo devmem 0x80000070 32 0x00000898

# Vertical totals: field 1 = 1125, field 0 = 1125
sudo devmem 0x80000074 32 0x04650465

# Horizontal sync:
# start = 2008, end = 2052
sudo devmem 0x80000078 32 0x080407D8

# Vertical blank horizontal offsets:
# start = end = 1920
sudo devmem 0x8000007C 32 0x07800780

# Vertical sync:
# start line = 1083, end line = 1088
sudo devmem 0x80000080 32 0x0440043B

# Vertical sync horizontal offsets:
# start = end = 2008
sudo devmem 0x80000084 32 0x07D807D8

# Mirror field-0 settings into field-1 registers
sudo devmem 0x80000088 32 0x07800780
sudo devmem 0x8000008C 32 0x0440043B
sudo devmem 0x80000090 32 0x07D807D8

# Field-1 active vertical size = 1080
sudo devmem 0x80000094 32 0x04380000

# Enable:
# - internal generator register sources
# - register updates
# - generator
# - VTC core
sudo devmem 0x80000000 32 0x03FDEF07
sudo devmem 0xFD4AB224 32 0x00000001
sudo devmem 0xFD4AB20C 32 0x00010101
sudo devmem 0xFD4AB210 32 0x00010101
sudo devmem 0xFD4AB214 32 0x00010101
sudo devmem 0xFD4AA044 32 0x00001000
sudo devmem 0xFD4AA048 32 0x00000000
sudo devmem 0xFD4AA04C 32 0x00000000
sudo devmem 0xFD4AA050 32 0x00000000
sudo devmem 0xFD4AA054 32 0x00001000
sudo devmem 0xFD4AA058 32 0x00000000
sudo devmem 0xFD4AA05C 32 0x00000000
sudo devmem 0xFD4AA060 32 0x00000000
sudo devmem 0xFD4AA064 32 0x00001000
sudo devmem 0xFD4AA068 32 0x00000000
sudo devmem 0xFD4AA06C 32 0x00000000
sudo devmem 0xFD4AA070 32 0x00000000
sudo devmem 0xFD4AA014 32 0x00000000
sudo devmem 0xFD4AA018 32 0x00000002
sudo devmem 0xFD4AA01C 32 0x00000000
sudo devmem 0xFD4AA00C 32 0x00000000
sudo devmem 0xFD4AB120 32 0x00000002
sudo devmem 0xFD4AB070 32 0x00000030

