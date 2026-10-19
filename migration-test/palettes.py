#!/usr/bin/env python3
"""Dump fence-related block palette entries (and chest items) from a world's overworld region files."""
import struct, zlib, sys, os, glob, re
def rd(b, i, t):
    if t==1: return struct.unpack_from('>b',b,i)[0], i+1
    if t==2: return struct.unpack_from('>h',b,i)[0], i+2
    if t==3: return struct.unpack_from('>i',b,i)[0], i+4
    if t==4: return struct.unpack_from('>q',b,i)[0], i+8
    if t==5: return struct.unpack_from('>f',b,i)[0], i+4
    if t==6: return struct.unpack_from('>d',b,i)[0], i+8
    if t==7:
        n=struct.unpack_from('>i',b,i)[0]; return b[i+4:i+4+n], i+4+n
    if t==8:
        n=struct.unpack_from('>H',b,i)[0]; return b[i+2:i+2+n].decode('utf8','replace'), i+2+n
    if t==9:
        et=b[i]; n=struct.unpack_from('>i',b,i+1)[0]; i+=5; out=[]
        for _ in range(n):
            v,i=rd(b,i,et); out.append(v)
        return out,i
    if t==10:
        d={}
        while True:
            et=b[i]; i+=1
            if et==0: return d,i
            n=struct.unpack_from('>H',b,i)[0]; k=b[i+2:i+2+n].decode(); i+=2+n
            d[k],i=rd(b,i,et)
    if t==11:
        n=struct.unpack_from('>i',b,i)[0]; return list(struct.unpack_from('>%di'%n,b,i+4)), i+4+4*n
    if t==12:
        n=struct.unpack_from('>i',b,i)[0]; return list(struct.unpack_from('>%dq'%n,b,i+4)), i+4+8*n
    raise ValueError(t)
def chunks(world):
    for f in sorted(glob.glob(os.path.join(world,'region','*.mca'))):
        data=open(f,'rb').read()
        if len(data)<8192: continue
        for idx in range(1024):
            off=struct.unpack_from('>I',data,idx*4)[0]
            if not off: continue
            s=(off>>8)*4096
            ln,comp=struct.unpack_from('>iB',data,s)
            raw=zlib.decompress(data[s+5:s+4+ln])
            root,_=rd(raw,3+struct.unpack_from('>H',raw,1)[0],10)
            yield root
pat=re.compile(sys.argv[2] if len(sys.argv)>2 else 'fence')
for c in chunks(sys.argv[1]):
    cx,cz=c.get('xPos'),c.get('zPos')
    for sec in c.get('sections',[]):
        for e in sec.get('block_states',{}).get('palette',[]):
            if pat.search(e['Name']):
                print(f"chunk {cx},{cz} sec y={sec['Y']}: {e['Name']} {dict(sorted(e.get('Properties',{}).items()))}")
    for be in c.get('block_entities',[]):
        if 'Items' in be:
            print(f"chunk {cx},{cz} block_entity {be['id']} @{be['x']},{be['y']},{be['z']}: {[(it['id'],it['Count']) for it in be['Items']]}")
