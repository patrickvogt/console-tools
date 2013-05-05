#!/usr/bin/env python -c

# using PyUSB core module for USB init, transfer etc.
import usb.core
# using socket for htonl() (host to network byte order (long))
import socket

# using static variables inside functions
def static_var(varname, value):
    def decorate(func):
        setattr(func, varname, value)
        return func
    return decorate

# using pseudo-enums
def enum(**enums):
    return type('Enum', (), enums)

# enum for the gb rom header info    
GB_ROM = enum \
(
    ROMHEADER_SIZE   =  0x150,  # header size of GB ROM 
    ROMTITLE_OFFSET  =  0x134,  # address of the ROM title
    ROMTITLE_SIZE    =  0x010,  # size of the ROM title (could be shorter)
    GBC_FLAG_OFFSET  =  0x143,  # address of the GBC flag
    SGB_FLAG_OFFSET  =  0x146,  # address of the SGB flag
    ROMSIZE_OFFSET   =  0x148,  # address of the ROM size
    RAMSIZE_OFFSET   =  0x149,  # address of the RAM size
)

# enum for the flash card info
EMS = enum \
(
    BANK_SIZE     =  0x400000,             # bank size
    BANK0_OFFSET  =  0x000000,             # start address of first bank
    BANK1_OFFSET  =  0x000000 + 0x400000,  # start address of second bank
    SRAM_SIZE     =  0x020000,             # SRAM size
    EP_SEND       =  0x000002,             # ENDPOINT_OUT
    EP_RECV       =  0x000081,             # ENDPOINT_IN
#   EP_DUNNO      =  0x000083,             # Another ENDPOINT_IN?
    VID           =  0x004670,             # Vendor id of EMS Productions
    PID           =  0x009394,             # Product id of the GB flash card
)

# enum for the USB transfer
USB_TRANSFER = enum \
(
    BLOCKSIZE_DATAREAD   =  4096,  # blocksize for reading data from the flash card
    BLOCKSIZE_DATAWRITE  =    32,  # blocksize for writing data to the flash card
    # INFO: original software also uses 41 = 32 (data) + 9 (cmd) bytes
    TIMEOUT_DATAWRITE    =  2500,  # timeout for writing a blocksize to the flash card
    # INFO: if blocksize is dynamic this has to be changed accordingly 
)

# enum for commands which will be sent to the flash card in order to read/write the ROM/SRAM
EMS_COMMANDS = enum \
(
    ROM_READ   = 0xff,
    ROM_WRITE  = 0x57,
    SRAM_READ  = 0x6d,
    SRAM_WRITE = 0x4d
)

# some private helper functions

# convert and return a byte array into a printable string
def _bytearray2ascii(bytearr):
    return "".join(map(chr, bytearr))
    
# returns the start address of the given bank
def _get_bank_start_address(bank):
    if 0 == bank: #first bank
        return EMS.BANK0_OFFSET
    elif 1 == bank: #second bank
        return EMS.BANK1_OFFSET
    else:
        raise ValueError('Invalid bank number given.')
    
# prepare and return a command which should be send to the flash card
def _prepare_ems_cmd(ems_cmd, address, value):
    # convert the given arguments to network byte order (4 bytes)
    address = socket.htonl(address)
    value = socket.htonl(value)

    # return a byte array with the 9 bytes of the command
    return bytearray\
    ( \
        [   ems_cmd, \
            (address & 0x000000ff), \
            (address & 0x0000ff00) >> 8,  \
            (address & 0x00ff0000) >> 16, \
            (address & 0xff000000) >> 24, \
            (value   & 0x000000ff), \
            (value   & 0x0000ff00) >> 8, \
            (value   & 0x00ff0000) >> 16, \
            (value   & 0xff000000) >> 24, \
        ] \
    )

# prepare and return a read command of 'value' bytes starting at 'address'
def _prepare_ems_rom_read(address, value):
   return _prepare_ems_cmd(EMS_COMMANDS.ROM_READ, address, value)

# not implemented yet -- shouldn't be too hard   
def _prepare_ems_sram_read():
    print "SRAM_read -- not implemented yet"

# prepare and return a write command of 'value' bytes starting at 'address'
def _prepare_ems_rom_write(address, value):
    return _prepare_ems_cmd(EMS_COMMANDS.ROM_WRITE, address, value)

# not implemented yet -- shouldn't be too hard 
def _prepare_ems_sram_write():
    print "SRAM_write -- not implemented yet"
    
# print the ROM title (assuming it is 15 bytes long)
def _print_rom_title(header):
    print _bytearray2ascii(header[GB_ROM.ROMTITLE_OFFSET:GB_ROM.ROMTITLE_OFFSET+GB_ROM.ROMTITLE_SIZE])
    
# print a separator
def _print_separator():
    print "----------------"
    
# print ROM type
def _print_rom_type(header):
    print "Type: ",
    
    if header[GB_ROM.GBC_FLAG_OFFSET] == 0x80:
        print "GB/GBC"
    elif header[GB_ROM.GBC_FLAG_OFFSET] == 0xC0:
        print "GBC only"
    else:
        print "GB"
     
# print SGB enhancements     
def _print_sgb(header):
    print "SGB?: ",
    
    if header[GB_ROM.SGB_FLAG_OFFSET] == 0x03:
        print "Yes"
    else:
        print "No"
       
# print ROM size       
def _print_rom_size(header):
    print "ROM size: "+str(1 << (header[GB_ROM.ROMSIZE_OFFSET] + 5))+"kB"

# print RAM size    
def _print_ram_size(header):
    print "RAM size:",
    
    tmp = header[GB_ROM.RAMSIZE_OFFSET]
    
    if tmp >= 0x01 and tmp <= 0x04:
        print str(1 << (2*tmp-1))+"kB"
    else:
        print "None"
    
# prints some of the header info of the rom
def _print_header(header):
    # print ROM title
    _print_rom_title(header)
    
    # separator
    _print_separator()
    
    # print the type of the ROM
    _print_rom_type(header)
    
    # print SGB enhancements of the ROM
    _print_sgb(header)
    
    # print ROM size
    _print_rom_size(header)
    
    # print RAM size
    _print_ram_size(header)
    
    # print empty line
    print ""
    
# try to find and open the flash card
# psuedo-singleton
@static_var("instance", None)
def ems_open():
    # function already called and instance already found?
    if None == ems_open.instance:
        ems_open.instance = usb.core.find(idVendor=EMS.VID, idProduct=EMS.PID)
    return ems_open.instance
    
# read the header of the ROM in bank 'bank'
def ems_read_rom_header(ems_devh, bank): 
    # prepare args
    address = _get_bank_start_address(bank)
    value = GB_ROM.ROMHEADER_SIZE
    
    # prepare command
    cmd = _prepare_ems_rom_read(address, value)
    
    # send read command
    ems_devh.write(EMS.EP_SEND, cmd)
    
    #receive and return ROM header data
    return ems_devh.read(EMS.EP_RECV, GB_ROM.ROMHEADER_SIZE) 
    
# read the ROM in bank 'bank' and save to file 'filename'
def ems_read_rom(ems_devh, bank, filename, count):
    # prepare args
    address = _get_bank_start_address(bank)
    value = USB_TRANSFER.BLOCKSIZE_DATAREAD

    # save the initial start address where we started reading
    init_address = address
    
    # copy from USB to ROM
    with open(filename, 'wb') as f: # open writable file
        while address < (init_address + count): # is still something to read?
            # prepare command
            cmd = _prepare_ems_rom_read(address, value)
            # send read command
            ems_devh.write(EMS.EP_SEND, cmd)
            # receive the ROM data
            buf = ems_devh.read(EMS.EP_RECV, USB_TRANSFER.BLOCKSIZE_DATAREAD)
            # if data was read
            if buf:
                # write it to the file
                f.write(buf)
                # update the read address
                address = address + USB_TRANSFER.BLOCKSIZE_DATAREAD
            else:
                break 
        # close file
        f.close()
        
# dump both banks of the flash card
def ems_dump(ems_devh, filename):
    # dump bank 0
    ems_read_rom(ems_devh, 0, "bank_0"+filename, EMS.BANK_SIZE)
    # dump bank 1
    ems_read_rom(ems_devh, 1, "bank_1"+filename, EMS.BANK_SIZE)

# write the ROM file in 'filename' to bank 'bank' via USB
def ems_write_rom(ems_devh, bank, filename):
    # prepare args
    address = _get_bank_start_address(bank)
    value = USB_TRANSFER.BLOCKSIZE_DATAWRITE

    # copy from ROM to USB
    with open(filename, 'rb') as f: # open readable file
        while True:
            # read from ROM file
            buf = f.read(USB_TRANSFER.BLOCKSIZE_DATAWRITE)
            # if data was read
            if buf:
                # prepare write command
                cmd = _prepare_ems_rom_write(address, value)
                # add the actual data to the command
                cmd.extend(buf)
                # write the data to the flash card
                ems_devh.write(EMS.EP_SEND, cmd, None, USB_TRANSFER.TIMEOUT_DATAWRITE)
                # update the write address
                address = address + USB_TRANSFER.BLOCKSIZE_DATAWRITE
            else:
                # all data written -> finished
                break
        # close the file
        f.close()
            
# TESTS

# ################################
# # 01. TEST FOR ROM HEADER INFO #
# ################################

# # try to open flash card
# ems_devh = ems_open()  

# # is flash card connected and was it found?
# if ems_devh is None:
    # raise ValueError('EMS was not found.')
    
# # read the header of the ROMs in both banks
# # for both banks do
# for bank in range(0,2):
    # # read the header
    # header = ems_read_rom_header(ems_devh, bank)
    # # and just print it
    # print "Bank "+str(bank)+":"
    # _print_header(header)
    
# ############################
# # 02. TEST FOR ROM WRITING #
# ############################

# # try to open flash card
# ems_devh = ems_open()  

# # is flash card connected and was it found?
# if ems_devh is None:
    # raise ValueError('EMS was not found.')
    
# # write file 'sml2.gb' to second bank
# ems_write_rom(ems_devh, 1, "sml2.gb")

# ############################
# # 03. TEST FOR ROM READING #
# ############################

# # try to open flash card
# ems_devh = ems_open()

# # read the header of the ROM in the second bank
# header = ems_read_rom_header(ems_devh, 1)
# # read the actual ROM in the second bank into 'bla.gb'
# ems_read_rom(ems_devh, 1, "bla.gb", 1 << (header[GB_ROM.ROMSIZE_OFFSET] + 15))   

# ########################
# # 04. TEST FOR DUMPING #
# ########################

# # try to open flash card
# ems_devh = ems_open()

# # dump both banks
# ems_dump(ems_devh, "dump.gb") 
