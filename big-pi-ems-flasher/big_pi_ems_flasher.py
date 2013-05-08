#!/usr/bin/env python -c

from Tkinter import *
from ttk import *
from tkFileDialog import *
from pi_ems_flasher import *
import os.path
import thread

#todo merging analysieren (höchste priorität)
#todo nicht lesen schreiben wenn cancel gedrückt wurde
#todo fortschrittsbalken in alle anseren dinger einfügen
#rom header mit dateigröße überprüfen
#prüfen, ob datei <= 4096kb ist
#prüfen, ob SRAM < 128kb ist

DIALOG_CLOSE = False

def nothing():
    print ""
    
def read(file, pbar, dlg):
    global DIALOG_CLOSE

    ems_devh = ems_open()
    ems_read_rom(ems_devh, 1, os.path.normpath(file), pbar)
    ems_close()
    
    DIALOG_CLOSE = True

def read_rom(root):
    file = asksaveasfilename(filetypes=[("GameBoy ROM-Files", "*.gb")])
    
    dlg = Toplevel()
    dlg.protocol('WM_DELETE_WINDOW', nothing)
    frame = Frame(dlg)
    frame.pack()
    pbar = Progressbar(frame, mode="determinate", orient="horizontal")
    pbar.pack()

    dlg.focus_set()
    ## Make sure events only go to our dialog
    dlg.grab_set()
    ## Make sure dialog stays on top of its parent window (if needed)
    dlg.transient(root)
    
    thread.start_new_thread(read, (file,pbar,dlg))
    
    dlg.update()
    dlg.deiconify()
    
    # warte auf fortschrittsbalken
    while False == DIALOG_CLOSE:
        dlg.update()
        
    dlg.destroy()
    
def write_rom():
    ems_devh = ems_open()
    
    file = askopenfilename(filetypes=[("GameBoy ROM-Files", "*.gb")])
    
    ems_write_rom(ems_devh, 1, os.path.normpath(file))
    ems_close()
    
def read_sram():
    ems_devh = ems_open()
    
    file = asksaveasfilename(filetypes=[("GameBoy SRAM-Files", "*.sav")])
    
    ems_read_sram(ems_devh, os.path.normpath(file))
    ems_close()
    
def write_sram():
    ems_devh = ems_open()
    
    file = askopenfilename(filetypes=[("GameBoy SRAM-Files", "*.sav")])
    
    ems_write_sram(ems_devh, os.path.normpath(file))
    ems_close()
    
def dummy():
    print "dummy"
    
app_title = u"\u03a0 EMS Flasher"
mpadx = 10
mpady = mpadx
    
root = Tk()
root.title(app_title+" (v0.0)")

frame = Frame(root)
frame.pack()

title = Label(frame, text=app_title, font=("Helvetica", 20, "bold"), width=20, anchor=CENTER)
title.pack(padx=mpadx, pady=(13,5))

group = LabelFrame(frame, text="File selection (not yet implemented in the backend)")
group.pack(padx=mpadx, pady=mpady, fill=BOTH)

f2 = Frame(group)
f2.pack(pady=(10,0))

# create the tree and scrollbars
      
dataCols = ("first","second","third")       
tree = Treeview(f2,columns=dataCols,
                         displaycolumns=("second","third"), height=5)
 
ysb = Scrollbar(orient=VERTICAL, command= tree.yview)
tree['yscroll'] = ysb.set
 
# setup column headings
tree.column("second", stretch=1, width=125)
tree.column("third", width=125)
tree.heading("#0", text="File Name", anchor=W)
tree.heading("second", text="Game Name", anchor=W)
tree.heading("third", text="Size", anchor=W)
#tree.insert('', 'end', 'first', text='Widget Tour')

#tree.set('first', 'second', '12KB')
 
# add tree and scrollbars to frame
tree.grid(in_=f2, row=0, column=0, sticky=NSEW)
ysb.grid(in_=f2, row=0, column=1, sticky=NS)

f3 = Frame(group)
f3.pack(pady=mpady)

button = Button(f3, text="Add", command=dummy)
button.pack(side=LEFT, padx=(5,5))
button.configure(state=DISABLED)

hi_there = Button(f3, text="Remove", command=dummy)
hi_there.pack(side=LEFT,padx=(0,5))
hi_there.configure(state=DISABLED)

hi_there1 = Button(f3, text="Remove All", command=dummy)
hi_there1.pack(side=LEFT,padx=(0,25))
hi_there1.configure(state=DISABLED)

group = LabelFrame(frame, text="Card Information")
group.pack(padx=mpadx, pady=mpady, fill=BOTH)

f1 = Frame(group)
f1.pack(side=TOP)

w = Label(f1, text="ROM bank:")
w.pack(padx=mpadx, side=LEFT)

value = StringVar()
box = Combobox(f1, textvariable=value, state='readonly', width=25)
box['values'] = ("Bank #0 (32M) [0x000000]", "Bank #1 (32M) [0x400000]")
box.current(0)
box.pack(fill=X, pady=mpady, side=LEFT)

f2 = Frame(group)
f2.pack(padx=mpadx, pady=mpady)

# create the tree and scrollbars
      
dataCols = ("first","second")       
tree = Treeview(f2,columns=dataCols,
                         displaycolumns=("second"), height=5)
 
ysb = Scrollbar(orient=VERTICAL, command= tree.yview)
tree['yscroll'] = ysb.set
 
# setup column headings
tree.column("second", stretch=1, width=250)
tree.heading("#0", text="Game Name", anchor=W)
tree.heading("second", text="Size", anchor=W)
tree.insert('', 'end', 'first', text='Widget Tour')

tree.set('first', 'second', '12KB')
 
# add tree and scrollbars to frame
tree.grid(in_=f2, row=0, column=0, sticky=NSEW)
ysb.grid(in_=f2, row=0, column=1, sticky=NS)

f3 = Frame(group)
f3.pack(pady=(0,10))

hi_there2 = Button(f3, text="Read ROM", command= lambda: read_rom(root))
hi_there2.pack(side=LEFT, padx=(0,5))

hi_there3 = Button(f3, text="Write ROM(s)", command=write_rom)
hi_there3.pack(side=LEFT, padx=(0,15))

hi_there2 = Button(f3, text="Read SRAM", command=read_sram)
hi_there2.pack(side=LEFT,padx=(0,5))

hi_there3 = Button(f3, text="Write SRAM", command=write_sram)
hi_there3.pack(side=LEFT)

status = Label(root, text="Status bar", relief=SUNKEN, anchor=W)
status.pack(side=BOTTOM, fill=X)

root.mainloop()