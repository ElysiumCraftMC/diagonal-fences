#!/usr/bin/env python3
"""Count blocks per palette Name (matching regex) and report any vanilla fence entries still carrying legacy diagonal properties."""
import sys, importlib.util, re, os
spec=importlib.util.spec_from_file_location('p', os.path.join(os.path.dirname(os.path.abspath(__file__)),'palettes.py'))
import types
src=open(os.path.join(os.path.dirname(os.path.abspath(__file__)),'palettes.py')).read().split('pat=re.compile')[0]
m=types.ModuleType('p'); exec(src, m.__dict__)
pat=re.compile(sys.argv[2] if len(sys.argv)>2 else 'fence$')
counts={}; legacy=0; chunks=set()
for c in m.chunks(sys.argv[1]):
    for sec in c.get('sections',[]):
        bs=sec.get('block_states',{}); pal=bs.get('palette',[]); data=bs.get('data')
        n=len(pal)
        if n==1 or not data: idx=[0]*4096
        else:
            bits=max(4,(n-1).bit_length()); per=64//bits; mask=(1<<bits)-1; idx=[]
            for i in range(4096):
                v=data[i//per] & 0xFFFFFFFFFFFFFFFF
                idx.append((v>>((i%per)*bits))&mask)
        for i,e in enumerate(pal):
            if not pat.search(e['Name']): continue
            k=idx.count(i)
            counts[e['Name']]=counts.get(e['Name'],0)+k
            if e['Name'].startswith('minecraft:') and any(p in e.get('Properties',{}) for p in ('north_east','south_east','south_west','north_west')):
                legacy+=k; chunks.add((c['xPos'],c['zPos']))
for k,v in sorted(counts.items()): print(f"{v:6d} {k}")
print(f"legacy-format vanilla fence blocks: {legacy} in {len(chunks)} chunks")
