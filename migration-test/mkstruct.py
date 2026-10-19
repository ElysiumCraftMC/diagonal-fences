#!/usr/bin/env python3
"""Write a structure template exactly as Diagonal Fences 4.2.6 would have stored it (legacy palette props)."""
import struct, gzip, sys
def s(x): b=x.encode(); return struct.pack('>H',len(b))+b
def tag(t,name,payload): return bytes([t])+s(name)+payload
def comp(items): return b''.join(items)+b'\x00'
def string(v): return s(v)
def lst(t,elems): return bytes([t])+struct.pack('>i',len(elems))+b''.join(elems)
def ints(v): return struct.pack('>i',v)
def fence(name, **p):
    base=dict(east='false',north='false',south='false',west='false',north_east='false',south_east='false',south_west='false',north_west='false',waterlogged='false')
    base.update(p)
    return comp([tag(8,'Name',string(name)), tag(10,'Properties',comp([tag(8,k,string(v)) for k,v in sorted(base.items())]))])
palette=[fence('minecraft:acacia_fence',south_east='true'), fence('minecraft:acacia_fence',north_west='true',waterlogged='true')]
def block(x,y,z,st): return comp([tag(9,'pos',lst(3,[ints(x),ints(y),ints(z)])), tag(3,'state',ints(st))])
blocks=[block(0,0,0,0), block(1,0,1,1)]
root=tag(10,'',comp([tag(3,'DataVersion',ints(3120)), tag(9,'size',lst(3,[ints(2),ints(1),ints(2)])), tag(9,'palette',lst(10,palette)), tag(9,'blocks',lst(10,blocks)), tag(9,'entities',lst(10,[]))]))
open(sys.argv[1],'wb').write(gzip.compress(root))
