#!/usr/bin/env python3
"""Compare every block of two saves of the same world, position by position, over the chunks present in both,
plus block entities, entities (falling blocks etc.), POI and heightmaps.
usage: worlddiff.py <worldA> <worldB> [--strip-legacy] [--fence-rename] [--diag-to-vanilla]
  --strip-legacy    drop 4.x diagonal fence properties on both sides before comparing
  --fence-rename    treat diagonalfences:<ns>/<path> as <ns>:<path> (B side)
"""
import sys, os, glob, struct, zlib, types, re, collections, json
src=open(os.path.join(os.path.dirname(os.path.abspath(__file__)),'palettes.py')).read().split('pat=re.compile')[0]
m=types.ModuleType('p'); exec(src, m.__dict__)
A,B=sys.argv[1],sys.argv[2]; opts=sys.argv[3:]
leg=re.compile(r'(north_east|south_east|south_west|north_west)=(true|false),?')
def canon(e, side):
    n=e['Name']; p=dict(e.get('Properties',{}))
    if '--fence-rename' in opts:
        mm=re.match(r'diagonalfences:([^/]+)/(.+)',n)
        if mm: n=f'{mm.group(1)}:{mm.group(2)}'
    if '--strip-legacy' in opts:
        for k in ('north_east','south_east','south_west','north_west'): p.pop(k,None)
    return n+('['+','.join(f'{k}={v}' for k,v in sorted(p.items()))+']' if p else '')
def load_region(path):
    out={}
    if not os.path.exists(path): return out
    data=open(path,'rb').read()
    if len(data)<8192: return out
    for idx in range(1024):
        off=struct.unpack_from('>I',data,idx*4)[0]
        if not off: continue
        s=(off>>8)*4096
        ln,comp=struct.unpack_from('>iB',data,s)
        raw=zlib.decompress(data[s+5:s+4+ln])
        out[idx]=m.rd(raw,3+struct.unpack_from('>H',raw,1)[0],10)[0]
    return out
def sec_states(sec, side):
    bs=sec.get('block_states')
    if not bs: return None
    pal=[canon(e,side) for e in bs['palette']]; data=bs.get('data'); n=len(pal)
    if n==1 or not data: return [pal[0]]*4096
    bits=max(4,(n-1).bit_length()); per=64//bits; mask=(1<<bits)-1
    return [pal[((data[i//per]&0xFFFFFFFFFFFFFFFF)>>((i%per)*bits))&mask] for i in range(4096)]
def norm_obj(o):
    return json.dumps(o, sort_keys=True, default=lambda x: list(x) if isinstance(x,(bytes,bytearray)) else str(x))
diff=collections.Counter(); ex=[]; positions=0; chunks=0; be_total=0; be_diff=[]; hm_diff=0; maxpal=0
falling=[]; poi_total=0; poi_diff=0; ent_total=0; ent_diff=0
for f in sorted(glob.glob(os.path.join(A,'region','*.mca'))):
    ra=load_region(f); rb=load_region(os.path.join(B,'region',os.path.basename(f)))
    for idx,ca in ra.items():
        cb=rb.get(idx)
        if cb is None: continue
        chunks+=1
        sb={s['Y']:s for s in cb.get('sections',[])}
        for s in ca.get('sections',[]):
            maxpal=max(maxpal,len(s.get('block_states',{}).get('palette',[])))
            xa=sec_states(s,'A')
            if xa is None: continue
            xb=sec_states(sb[s['Y']],'B') if s['Y'] in sb else None
            for i in range(4096):
                positions+=1
                va=xa[i]; vb=xb[i] if xb else '<missing>'
                if va!=vb:
                    diff[(va,vb)]+=1
                    if len(ex)<6: ex.append(((ca['xPos']*16+(i&15), s['Y']*16+(i>>8), ca['zPos']*16+((i>>4)&15)),va,vb))
        bea={(b['x'],b['y'],b['z']):b for b in ca.get('block_entities',[])}
        beb={(b['x'],b['y'],b['z']):b for b in cb.get('block_entities',[])}
        for k,v in bea.items():
            be_total+=1
            if norm_obj(v)!=norm_obj(beb.get(k)): be_diff.append((k,v.get('id')))
        ha,hb=ca.get('Heightmaps',{}),cb.get('Heightmaps',{})
        if any(norm_obj(ha[k])!=norm_obj(hb[k]) for k in set(ha)&set(hb)): hm_diff+=1
for sub in ('entities','poi'):
    for f in sorted(glob.glob(os.path.join(A,sub,'*.mca'))):
        ra=load_region(f); rb=load_region(os.path.join(B,sub,os.path.basename(f)))
        for idx,ca in ra.items():
            cb=rb.get(idx)
            if cb is None: continue
            if sub=='poi':
                poi_total+=1
                if norm_obj(ca.get('Sections'))!=norm_obj(cb.get('Sections')): poi_diff+=1
            else:
                eb={str(e.get('UUID')):e for e in cb.get('Entities',[])}
                for e in ca.get('Entities',[]):
                    ent_total+=1
                    o=eb.get(str(e.get('UUID')))
                    if 'BlockState' in e:
                        falling.append((e['id'], canon(e['BlockState'],'A'), canon(o['BlockState'],'B') if o and 'BlockState' in o else '<gone>'))
print(f"chunks compared: {chunks}, block positions: {positions}, largest section palette in A: {maxpal} entries")
print(f"block positions that differ: {sum(diff.values())}")
for (x,y),c in diff.most_common(20): print(f"  {c:7d}  {x}  ->  {y}")
for e in ex: print('   e.g.', e)
print(f"block entities: {be_total}, differing: {len(be_diff)} {be_diff[:6]}")
print(f"heightmaps differing (types present on both sides): {hm_diff} chunks; poi chunks compared: {poi_total}, differing: {poi_diff}; entities in A: {ent_total}")
for x in falling: print('  entity with BlockState:', x)
