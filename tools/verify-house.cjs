const assert=require('node:assert/strict');
const {readRegion,ids}=require('./world-nbt.cjs');
const before=readRegion('backups/2026-11-01-spawn-house/r.-1.0.mca');
const after=readRegion('world/region/r.-1.0.mca');
let changes=0,chests=0,spawners=0;
assert.equal(after.chunks.size,before.chunks.size);
for(const [i,{root:a}] of after.chunks){const b=before.chunks.get(i).root;
  if(after.chunks.get(i).raw.equals(before.chunks.get(i).raw))continue;
  for(const s of a.v.sections.v.items){if(!s.block_states)continue;const old=b.v.sections.v.items.find(v=>v.Y.v===s.Y.v);const ai=ids(s),bi=ids(old);
    for(let n=0;n<4096;n++){const av=s.block_states.v.palette.v.items[ai[n]],bv=old.block_states.v.palette.v.items[bi[n]];if(JSON.stringify(av)===JSON.stringify(bv))continue;
      const x=a.v.xPos.v*16+(n&15),y=s.Y.v*16+(n>>8),z=a.v.zPos.v*16+((n>>4)&15);
      assert(x>=-117&&x<=-88&&y>=20&&y<=37&&z>=71&&z<=101,'Change outside the box');changes++;
    }
  }
  for(const e of a.v.block_entities.v.items){if(e.x.v< -117||e.x.v> -88||e.y.v<20||e.y.v>37||e.z.v<71||e.z.v>101)continue;
    if(e.id.v==='minecraft:chest'){assert(e.LootTable.v.startsWith('minecraft:chests/'));chests++;}
    if(e.id.v==='minecraft:mob_spawner'){assert(['minecraft:zombie','minecraft:skeleton'].includes(e.SpawnData.v.entity.v.id.v));assert.equal(e.SpawnPotentials.v.items.length,1);spawners++;}
  }
}
assert.equal(chests,3);assert.equal(spawners,2);assert(changes>2000);
console.log(JSON.stringify({verified:true,changes,chests,spawners,exteriorAndIronShell:'unchanged'}));
