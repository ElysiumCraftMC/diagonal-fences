#!/usr/bin/env python3
"""Compare what a vanilla client decoded (probe.js output) with the block states the server saved at the same positions.
usage: probe-compare.py <world> <probe.json>"""
import sys, json, os, types, collections
src=open(os.path.join(os.path.dirname(os.path.abspath(__file__)),'worlddiff.py')).read().split('diff=collections.Counter()')[0]
sys_argv=sys.argv; sys.argv=['x','a','b']
m=types.ModuleType('w'); m.__dict__['__file__']=os.path.join(os.path.dirname(os.path.abspath(__file__)),'worlddiff.py'); exec(src,m.__dict__)
world, probe = sys_argv[1], json.load(open(sys_argv[2]))
reg=m.load_region(os.path.join(world,'region','r.0.0.mca'))
truth={}
for c in reg.values():
    for s in c.get('sections',[]):
        if s['Y']!=9: continue
        xs=m.sec_states(s,'A')
        for i in range(4096):
            x=c['xPos']*16+(i&15); z=c['zPos']*16+((i>>4)&15); y=144+(i>>8)
            truth[f'{x},{y},{z}']=xs[i]
same=0; wrong=[]; missing=0
for k,v in probe.items():
    t=truth.get(k)
    if v is None: missing+=1; continue
    if v['state']==t: same+=1
    else: wrong.append((k,t,v['state'],v['stateId']))
print(f"positions: {len(probe)}, client decoded correctly: {same}, wrong: {len(wrong)}, not received: {missing}")
for w in wrong[:25]: print(f"  {w[0]:>14}  server has {w[1]}\n{'':18}vanilla client sees {w[2]} (id {w[3]})")
